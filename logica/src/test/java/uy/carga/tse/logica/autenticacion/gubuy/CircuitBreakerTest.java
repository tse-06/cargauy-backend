package uy.carga.tse.logica.autenticacion.gubuy;

import static org.junit.jupiter.api.Assertions.*;
import static uy.carga.tse.logica.autenticacion.gubuy.TestSupport.NOW;

import java.time.Duration;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;
import uy.carga.tse.logica.testsupport.MutableClock;

class CircuitBreakerTest {

    private static String fail() {
        throw new ServicioNoDisponibleException(
            CodigoError.GUBUY_NO_DISPONIBLE,
            "caido"
        );
    }

    @Test
    void seAbreTrasLasFallasConsecutivasYFallaRapidoSinLlamar() {
        var clock = new MutableClock(NOW);
        var cb = new CircuitBreaker(3, Duration.ofSeconds(30), clock);
        for (int i = 0; i < 3; i++) {
            assertThrows(ServicioNoDisponibleException.class, () ->
                cb.execute(CircuitBreakerTest::fail)
            );
        }
        assertEquals(CircuitBreaker.State.OPEN, cb.state());

        int[] llamadas = { 0 };
        assertThrows(ServicioNoDisponibleException.class, () ->
            cb.execute(() -> {
                llamadas[0]++;
                return "x";
            })
        );
        assertEquals(0, llamadas[0], "abierto: no debe ejecutar el pedido");
    }

    @Test
    void pasaAHalfOpenYSeCierraSiLaPruebaTieneExito() {
        var clock = new MutableClock(NOW);
        var cb = new CircuitBreaker(1, Duration.ofSeconds(30), clock);
        assertThrows(ServicioNoDisponibleException.class, () ->
            cb.execute(CircuitBreakerTest::fail)
        );
        clock.advance(Duration.ofSeconds(31));

        assertEquals("ok", cb.execute(() -> "ok"));
        assertEquals(CircuitBreaker.State.CLOSED, cb.state());
    }

    @Test
    void siLaPruebaFallaVuelveAAbrirse() {
        var clock = new MutableClock(NOW);
        var cb = new CircuitBreaker(1, Duration.ofSeconds(30), clock);
        assertThrows(ServicioNoDisponibleException.class, () ->
            cb.execute(CircuitBreakerTest::fail)
        );
        clock.advance(Duration.ofSeconds(31));

        assertThrows(ServicioNoDisponibleException.class, () ->
            cb.execute(CircuitBreakerTest::fail)
        );
        assertEquals(CircuitBreaker.State.OPEN, cb.state());
    }

    @Test
    void unRechazoDeAutenticacionNoCuentaComoFalla() {
        var cb = new CircuitBreaker(
            2,
            Duration.ofSeconds(30),
            new MutableClock(NOW)
        );
        for (int i = 0; i < 5; i++) {
            assertThrows(GubUyAuthException.class, () ->
                cb.execute(() -> {
                    throw new GubUyAuthException(
                        CodigoError.GUBUY_CODIGO_RECHAZADO,
                        "x"
                    );
                })
            );
        }
        assertEquals(CircuitBreaker.State.CLOSED, cb.state());
    }

    @Test
    void unExitoReiniciaElContadorDeFallas() {
        var cb = new CircuitBreaker(
            3,
            Duration.ofSeconds(30),
            new MutableClock(NOW)
        );
        assertThrows(ServicioNoDisponibleException.class, () ->
            cb.execute(CircuitBreakerTest::fail)
        );
        assertThrows(ServicioNoDisponibleException.class, () ->
            cb.execute(CircuitBreakerTest::fail)
        );
        cb.execute(() -> "ok");
        assertThrows(ServicioNoDisponibleException.class, () ->
            cb.execute(CircuitBreakerTest::fail)
        );
        assertThrows(ServicioNoDisponibleException.class, () ->
            cb.execute(CircuitBreakerTest::fail)
        );
        assertEquals(CircuitBreaker.State.CLOSED, cb.state());
    }
}
