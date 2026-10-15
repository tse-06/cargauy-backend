package uy.carga.tse.logica.autenticacion.gubuy;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GubUyConfigProducerTest {

    private static final String SECRETO = "valor-que-no-debe-filtrarse";

    private static Map<String, String> base() {
        Map<String, String> env = new HashMap<>();
        env.put("GUBUY_ISSUER", "https://auth-testing.example.uy/oidc/v1");
        env.put("GUBUY_CLIENT_ID", "id");
        env.put("GUBUY_CLIENT_SECRET", SECRETO);
        env.put(
            "GUBUY_REDIRECT_URI",
            "https://api.carga.uy/auth/gubuy/callback"
        );
        env.put(
            "GUBUY_ALLOWED_RETURN",
            "https://app.carga.uy/, cargauy://auth"
        );
        env.put(
            "AUTH_FLOW_KEY",
            Base64.getEncoder().encodeToString(new byte[32])
        );
        return env;
    }

    @Test
    void cargaValoresPorDefecto() {
        var cfg = GubUyConfigProducer.fromLookup(base()::get);
        assertTrue(cfg.scopes().contains("openid"));
        assertEquals("numero_documento", cfg.documentClaim());
        assertEquals("nid", cfg.levelClaim());
        assertFalse(cfg.usePkce());
        assertTrue(cfg.cookieSecure());
        assertEquals(2, cfg.allowedReturnPrefixes().size());
    }

    @Test
    void faltaUnaVariableObligatoriaFallaConMensajeClaro() {
        var env = base();
        env.remove("GUBUY_CLIENT_SECRET");
        var e = assertThrows(IllegalArgumentException.class, () ->
            GubUyConfigProducer.fromLookup(env::get)
        );
        assertTrue(e.getMessage().contains("clientSecret"));
    }

    @Test
    void claveDeFlujoCortaSeRechaza() {
        var env = base();
        env.put(
            "AUTH_FLOW_KEY",
            Base64.getEncoder().encodeToString(new byte[8])
        );
        assertThrows(IllegalArgumentException.class, () ->
            GubUyConfigProducer.fromLookup(env::get)
        );
    }

    @Test
    void toStringNoFiltraSecretos() {
        var cfg = GubUyConfigProducer.fromLookup(base()::get);
        assertFalse(cfg.toString().contains(SECRETO));
        assertFalse(cfg.toString().contains(base().get("AUTH_FLOW_KEY")));
    }
}
