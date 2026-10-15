package uy.carga.tse.logica.autenticacion.gubuy;

import static org.junit.jupiter.api.Assertions.*;
import static uy.carga.tse.logica.autenticacion.gubuy.TestSupport.*;

import java.net.URI;
import java.security.KeyPair;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

class GubUyLoginServiceTest {

    /** gub.uy simulado: el id_token que devuelve se arma con el nonce que realmente se envio en /authorize. */
    static class FakeGubUy implements GubUyClient {

        final KeyPair keys = rsaKeys();
        final List<String> codes = new ArrayList<>();
        final List<String> verifiers = new ArrayList<>();
        String nonceToEcho = "?";
        boolean down;

        @Override
        public OidcEndpoints discover() {
            if (down) throw new ServicioNoDisponibleException(
                CodigoError.GUBUY_NO_DISPONIBLE,
                "gub.uy caido"
            );
            return new OidcEndpoints(
                ISSUER,
                ISSUER + "/authorize",
                ISSUER + "/token",
                ISSUER + "/jwks"
            );
        }

        @Override
        public TokenResponse exchangeCode(String code, String codeVerifier) {
            codes.add(code);
            verifiers.add(codeVerifier);
            return new TokenResponse(
                idToken(keys, "k1", Map.of("nonce", nonceToEcho))
            );
        }

        @Override
        public String fetchJwks() {
            return jwks(keys, "k1");
        }
    }

    private FakeGubUy gubuy;

    private GubUyLoginService service(boolean pkce) {
        var cfg = config(pkce);
        var flows = new FlowStateService(cfg, CLOCK);
        var validator = new IdTokenValidator(cfg, gubuy, CLOCK);
        return new GubUyLoginService(cfg, gubuy, flows, validator);
    }

    @BeforeEach
    void setUp() {
        gubuy = new FakeGubUy();
    }

    private static Map<String, String> query(URI uri) {
        Map<String, String> out = new HashMap<>();
        for (String pair : uri.getRawQuery().split("&")) {
            String[] kv = pair.split("=", 2);
            out.put(
                kv[0],
                java.net.URLDecoder.decode(
                    kv[1],
                    java.nio.charset.StandardCharsets.UTF_8
                )
            );
        }
        return out;
    }

    @Test
    void startArmaElPedidoDeAutorizacion() {
        var start = service(false).start("https://app.carga.uy/inicio");
        var q = query(start.authorizationUri());

        assertTrue(
            start
                .authorizationUri()
                .toString()
                .startsWith(ISSUER + "/authorize?")
        );
        assertEquals("code", q.get("response_type"));
        assertEquals(CLIENT_ID, q.get("client_id"));
        assertEquals(
            "https://api.carga.uy/auth/gubuy/callback",
            q.get("redirect_uri")
        );
        assertEquals("openid personal_info document email", q.get("scope"));
        assertNotNull(q.get("state"));
        assertNotNull(q.get("nonce"));
        assertNull(q.get("code_challenge"));
        assertNotEquals(q.get("state"), q.get("nonce"));
    }

    @Test
    void conPkceEnviaElChallengeYElVerifierViajaEnLaCookieNoEnLaUrl() {
        var start = service(true).start(null);
        var q = query(start.authorizationUri());
        assertEquals("S256", q.get("code_challenge_method"));
        assertNotNull(q.get("code_challenge"));
        assertFalse(
            start.authorizationUri().toString().contains("code_verifier")
        );
    }

    @Test
    void flujoCompletoOk() {
        var svc = service(false);
        var start = svc.start("cargauy://auth");
        var q = query(start.authorizationUri());
        gubuy.nonceToEcho = q.get("nonce");
        // el nonce del token coincide con el del flujo; el resto del token lo arma TestSupport
        var result = svc.complete("abc123", q.get("state"), start.flowToken());

        assertEquals("12345678", result.identity().documento());
        assertEquals("cargauy://auth", result.returnTo());
        assertEquals(List.of("abc123"), gubuy.codes);
    }

    @Test
    void conPkceElVerifierDelFlujoSeEnviaAlCanje() {
        var svc = service(true);
        var start = svc.start(null);
        var q = query(start.authorizationUri());
        gubuy.nonceToEcho = q.get("nonce");
        svc.complete("c", q.get("state"), start.flowToken());

        String verifier = gubuy.verifiers.get(0);
        assertNotNull(verifier);
        assertEquals(GubUyLoginService.s256(verifier), q.get("code_challenge"));
    }

    @Test
    void rechazaStateQueNoCoincideYNoCanjeaElCodigo() {
        var svc = service(false);
        var start = svc.start(null);
        var e = assertThrows(GubUyAuthException.class, () ->
            svc.complete("c", "state-ajeno", start.flowToken())
        );
        assertEquals(CodigoError.GUBUY_ESTADO_INVALIDO, e.codigo());
        assertTrue(
            gubuy.codes.isEmpty(),
            "con state invalido no se debe canjear el code"
        );
    }

    @Test
    void rechazaCallbackSinCookieDeFlujo() {
        var svc = service(false);
        assertThrows(GubUyAuthException.class, () ->
            svc.complete("c", "s", null)
        );
    }

    @Test
    void rechazaTokenConNonceDeOtroFlujo() {
        var svc = service(false);
        var start = svc.start(null);
        var q = query(start.authorizationUri());
        gubuy.nonceToEcho = "nonce-de-otro-login";
        assertThrows(GubUyAuthException.class, () ->
            svc.complete("c", q.get("state"), start.flowToken())
        );
    }

    @Test
    void returnToPorDefectoEsElPrimeroPermitido() {
        var start = service(false).start(null);
        assertEquals(
            "https://app.carga.uy/",
            new FlowStateService(config(false), CLOCK)
                .verify(start.flowToken())
                .returnTo()
        );
    }

    @Test
    void returnToFueraDeLaListaSeRechaza() {
        var svc = service(false);
        for (String malo : List.of(
            "https://evil.example/",
            "https://app.carga.uy.evil.example/",
            "https://app.carga.uy@evil.example/",
            "cargauy://auth.evil",
            "javascript:alert(1)",
            "https://app.carga.uy/ok#token=x",
            "https://app.carga.uy/ok\r\nSet-Cookie: a=b"
        )) {
            var e = assertThrows(
                ValidacionNegocioException.class,
                () -> svc.start(malo),
                malo
            );
            assertEquals(CodigoError.RETURN_TO_INVALIDO, e.codigo());
        }
    }

    @Test
    void returnToPermitidos() {
        var svc = service(false);
        assertDoesNotThrow(() ->
            svc.start("https://app.carga.uy/empresas?x=1")
        );
        assertDoesNotThrow(() -> svc.start("cargauy://auth"));
        assertDoesNotThrow(() -> svc.start("cargauy://auth?x=1"));
    }

    @Test
    void returnToFromDevuelveVacioSiElFlujoEsInvalido() {
        var svc = service(false);
        assertTrue(svc.returnToFrom("basura").isEmpty());
        assertEquals(
            "https://app.carga.uy/",
            svc.returnToFrom(svc.start(null).flowToken()).orElseThrow()
        );
    }

    @Test
    void siGubUyNoEstaDisponibleStartFallaConUnavailable() {
        gubuy.down = true;
        assertThrows(ServicioNoDisponibleException.class, () ->
            service(false).start(null)
        );
    }

    @Test
    void elRolSolicitadoViajaEnElFlujoYLlegaAlResultado() {
        var svc = service(false);
        var start = svc.start("cargauy://auth", "CHOFER");
        var q = query(start.authorizationUri());
        assertNull(q.get("rol"), "el rol no se envia a gub.uy");
        gubuy.nonceToEcho = q.get("nonce");

        var result = svc.complete("c", q.get("state"), start.flowToken());
        assertEquals("CHOFER", result.rol());
    }

    @Test
    void sinRolSolicitadoElResultadoNoTieneRol() {
        var svc = service(false);
        var start = svc.start(null);
        gubuy.nonceToEcho = query(start.authorizationUri()).get("nonce");
        assertNull(
            svc
                .complete(
                    "c",
                    query(start.authorizationUri()).get("state"),
                    start.flowToken()
                )
                .rol()
        );
    }
}
