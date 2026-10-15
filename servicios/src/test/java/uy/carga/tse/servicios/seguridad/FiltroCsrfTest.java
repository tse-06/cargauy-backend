package uy.carga.tse.servicios.seguridad;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class FiltroCsrfTest {

    @Test
    void rechazaUnPostConCookieYSinElEncabezado() {
        assertTrue(FiltroCsrf.debeRechazar("POST", true, false, null));
        assertTrue(FiltroCsrf.debeRechazar("DELETE", true, false, "otro"));
    }

    @Test
    void aceptaUnPostConCookieYElEncabezado() {
        assertFalse(FiltroCsrf.debeRechazar("POST", true, false, "cargauy"));
        assertFalse(FiltroCsrf.debeRechazar("PUT", true, false, "cargauy"));
    }

    @Test
    void noExigeElEncabezadoEnMetodosDeLectura() {
        assertFalse(FiltroCsrf.debeRechazar("GET", true, false, null));
        assertFalse(FiltroCsrf.debeRechazar("HEAD", true, false, null));
        assertFalse(FiltroCsrf.debeRechazar("OPTIONS", true, false, null));
    }

    @Test
    void sinCookieDeSesionNoHayNadaQueFalsificar() {
        // el login y el login de gub.uy llegan sin cookie, tambien desde el movil
        assertFalse(FiltroCsrf.debeRechazar("POST", false, false, null));
    }

    @Test
    void conAuthorizationNoExigeElEncabezado() {
        // el navegador no manda Authorization solo: la request no es falsificable
        assertFalse(FiltroCsrf.debeRechazar("POST", true, true, null));
    }
}
