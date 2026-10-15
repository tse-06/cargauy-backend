package uy.carga.tse.servicios.recursos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class AutenticacionGubUyResourceTest {

    @Test
    void lasPaginasWebRecibenLaSesionEnCookie() {
        assertTrue(
            AutenticacionGubUyResource.esDestinoWeb(
                "http://localhost:3000/auth/callback"
            )
        );
        assertTrue(
            AutenticacionGubUyResource.esDestinoWeb(
                "https://carga.example.uy/auth/callback"
            )
        );
    }

    @Test
    void elDeepLinkMovilRecibeElTokenEnElFragmento() {
        assertFalse(AutenticacionGubUyResource.esDestinoWeb("cargauy://auth"));
    }
}
