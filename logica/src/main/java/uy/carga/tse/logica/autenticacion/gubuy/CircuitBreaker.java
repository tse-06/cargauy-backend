package uy.carga.tse.logica.autenticacion.gubuy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Supplier;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;

/**
 * Circuit breaker minimo (CLOSED -> OPEN -> HALF_OPEN -> CLOSED).
 * <p>
 * Solo cuentan como falla las {@link ServicioNoDisponibleException}: un rechazo de autenticacion
 * ({@link GubUyAuthException}) significa que gub.uy respondio, por lo tanto no abre el breaker.
 * Mientras esta abierto falla rapido sin hacer el pedido, de modo que un gub.uy caido no
 * consume hilos del servidor de aplicaciones.
 */
public class CircuitBreaker {

    public enum State {
        CLOSED,
        OPEN,
        HALF_OPEN,
    }

    private final int failureThreshold;
    private final Duration openDuration;
    private final Clock clock;

    private State state = State.CLOSED;
    private int consecutiveFailures;
    private Instant openedAt;
    private boolean probeInFlight;

    public CircuitBreaker(
        int failureThreshold,
        Duration openDuration,
        Clock clock
    ) {
        if (failureThreshold < 1) {
            throw new IllegalArgumentException(
                "failureThreshold debe ser >= 1"
            );
        }
        this.failureThreshold = failureThreshold;
        this.openDuration = openDuration;
        this.clock = clock;
    }

    public <T> T execute(Supplier<T> call) {
        acquire();
        try {
            T result = call.get();
            onSuccess();
            return result;
        } catch (ServicioNoDisponibleException e) {
            onFailure();
            throw e;
        } catch (RuntimeException e) {
            // el servicio respondio (p. ej. invalid_grant): no es una falla de disponibilidad
            onSuccess();
            throw e;
        }
    }

    public synchronized State state() {
        return state;
    }

    private synchronized void acquire() {
        if (state == State.OPEN) {
            if (clock.instant().isBefore(openedAt.plus(openDuration))) {
                throw new ServicioNoDisponibleException(
                    CodigoError.GUBUY_NO_DISPONIBLE,
                    "gub.uy no disponible (circuit breaker abierto)"
                );
            }
            state = State.HALF_OPEN;
            probeInFlight = false;
        }
        if (state == State.HALF_OPEN) {
            if (probeInFlight) {
                throw new ServicioNoDisponibleException(
                    CodigoError.GUBUY_NO_DISPONIBLE,
                    "gub.uy no disponible (probando recuperacion)"
                );
            }
            probeInFlight = true; // un unico pedido de prueba
        }
    }

    private synchronized void onSuccess() {
        consecutiveFailures = 0;
        probeInFlight = false;
        state = State.CLOSED;
    }

    private synchronized void onFailure() {
        probeInFlight = false;
        consecutiveFailures++;
        if (
            state == State.HALF_OPEN || consecutiveFailures >= failureThreshold
        ) {
            state = State.OPEN;
            openedAt = clock.instant();
        }
    }
}
