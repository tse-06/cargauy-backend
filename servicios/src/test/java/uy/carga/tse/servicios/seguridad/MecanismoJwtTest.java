package uy.carga.tse.servicios.seguridad;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class MecanismoJwtTest {

    @Test
    void tomaElTokenDelHeaderBearer() {
        assertEquals("abc", MecanismoJwt.extraerToken("Bearer abc", null));
        assertEquals("abc", MecanismoJwt.extraerToken("bearer  abc ", null));
    }

    @Test
    void sinHeaderTomaLaCookie() {
        assertEquals("abc", MecanismoJwt.extraerToken(null, "abc"));
    }

    @Test
    void conHeaderLaCookieSeIgnora() {
        assertEquals(
            "del-header",
            MecanismoJwt.extraerToken("Bearer del-header", "de-la-cookie")
        );
        // un Authorization que no es Bearer deja la request anonima
        assertNull(MecanismoJwt.extraerToken("Basic dXNlcjpwYXNz", "abc"));
    }

    @Test
    void sinHeaderNiCookieNoHayToken() {
        assertNull(MecanismoJwt.extraerToken(null, null));
        assertNull(MecanismoJwt.extraerToken(null, " "));
    }
}
