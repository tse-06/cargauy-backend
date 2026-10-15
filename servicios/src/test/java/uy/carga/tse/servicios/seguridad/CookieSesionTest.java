package uy.carga.tse.servicios.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Solo la vigencia y el flag Secure: armar la cookie necesita la implementacion de JAX-RS, que
 * la da el servidor.
 */
class CookieSesionTest {

    private static final Instant AHORA = Instant.parse("2026-10-13T12:00:00Z");

    @Test
    void secureEstaActivadoSalvoQueSeDesactive() {
        assertTrue(CookieSesion.secure(nombre -> null));
        assertTrue(CookieSesion.secure(nombre -> " "));
        assertTrue(CookieSesion.secure(nombre -> "true"));
        assertFalse(CookieSesion.secure(nombre -> "false"));
    }

    @Test
    void laVigenciaDeLaCookieEsLaDelToken() {
        assertEquals(
            1800,
            CookieSesion.segundosHasta(
                AHORA.plus(Duration.ofMinutes(30)),
                AHORA
            )
        );
    }

    @Test
    void unTokenVencidoDaCero() {
        assertEquals(
            0,
            CookieSesion.segundosHasta(AHORA.minusSeconds(5), AHORA)
        );
        assertEquals(0, CookieSesion.segundosHasta(AHORA, AHORA));
    }

    @Test
    void unaVigenciaMuyLargaNoDesbordaElEntero() {
        assertEquals(
            Integer.MAX_VALUE,
            CookieSesion.segundosHasta(
                AHORA.plus(Duration.ofDays(365L * 100)),
                AHORA
            )
        );
    }
}
