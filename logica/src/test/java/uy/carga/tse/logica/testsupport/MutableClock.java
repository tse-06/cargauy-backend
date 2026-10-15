package uy.carga.tse.logica.testsupport;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Reloj para pruebas que se adelanta a mano: permite probar vencimientos,
 * caches y ventanas de tiempo sin esperar tiempo real.
 * <p>
 * Las copias de {@link #withZone} comparten el instante con el original, asi
 * que adelantar uno adelanta todos.
 */
public final class MutableClock extends Clock {

    private final AtomicReference<Instant> ahora;
    private final ZoneId zona;

    public MutableClock(Instant inicio) {
        this(
            new AtomicReference<>(Objects.requireNonNull(inicio, "inicio")),
            ZoneOffset.UTC
        );
    }

    private MutableClock(AtomicReference<Instant> ahora, ZoneId zona) {
        this.ahora = ahora;
        this.zona = zona;
    }

    /** Adelanta el reloj (o lo atrasa, con una duracion negativa). */
    public void advance(Duration duracion) {
        ahora.updateAndGet(instante -> instante.plus(duracion));
    }

    public void set(Instant instante) {
        ahora.set(Objects.requireNonNull(instante, "instante"));
    }

    @Override
    public Instant instant() {
        return ahora.get();
    }

    @Override
    public ZoneId getZone() {
        return zona;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return zone.equals(zona) ? this : new MutableClock(ahora, zone);
    }
}
