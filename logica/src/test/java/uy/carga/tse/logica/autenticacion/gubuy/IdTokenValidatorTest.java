package uy.carga.tse.logica.autenticacion.gubuy;

import static org.junit.jupiter.api.Assertions.*;
import static uy.carga.tse.logica.autenticacion.gubuy.TestSupport.*;

import java.security.KeyPair;
import java.time.Clock;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;

class IdTokenValidatorTest {

    /** gub.uy simulado: devuelve el JWKS configurado y cuenta las descargas. */
    static class FakeClient implements GubUyClient {

        String jwks;
        int jwksFetches;
        boolean jwksDown;

        @Override
        public OidcEndpoints discover() {
            return new OidcEndpoints(
                ISSUER,
                ISSUER + "/authorize",
                ISSUER + "/token",
                ISSUER + "/jwks"
            );
        }

        @Override
        public TokenResponse exchangeCode(String code, String codeVerifier) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String fetchJwks() {
            jwksFetches++;
            if (jwksDown) throw new ServicioNoDisponibleException(
                CodigoError.GUBUY_NO_DISPONIBLE,
                "jwks caido"
            );
            return jwks;
        }
    }

    private KeyPair keys;
    private FakeClient client;
    private IdTokenValidator validator;

    @BeforeEach
    void setUp() {
        keys = rsaKeys();
        client = new FakeClient();
        client.jwks = jwks(keys, "k1");
        validator = new IdTokenValidator(config(false), client, CLOCK);
    }

    @Test
    void aceptaUnTokenValidoYExtraeLaIdentidad() {
        var id = validator.validate(idToken(keys, "k1", Map.of()), "nonce-1");
        assertEquals("uid-123", id.subject());
        assertEquals("12345678", id.documento());
        assertEquals("chofer@example.uy", id.email());
        assertEquals("2", id.nivel());
    }

    @Test
    void laCedulaQuedaSoloConDigitos() {
        var conFormato = idToken(
            keys,
            "k1",
            Map.of("numero_documento", "1.234.567-8")
        );
        assertEquals(
            "12345678",
            validator.validate(conFormato, "nonce-1").documento()
        );
        var sinDigitos = idToken(keys, "k1", Map.of("numero_documento", "-"));
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(sinDigitos, "nonce-1")
        );
    }

    @Test
    void rechazaNonceDistinto() {
        var e = assertThrows(GubUyAuthException.class, () ->
            validator.validate(idToken(keys, "k1", Map.of()), "otro-nonce")
        );
        assertEquals(CodigoError.GUBUY_TOKEN_INVALIDO, e.codigo());
    }

    @Test
    void rechazaAudienceDeOtroCliente() {
        String token = idToken(keys, "k1", Map.of("aud", "otro-cliente"));
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(token, "nonce-1")
        );
    }

    @Test
    void rechazaIssuerDistinto() {
        String token = idToken(
            keys,
            "k1",
            Map.of("iss", "https://evil.example")
        );
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(token, "nonce-1")
        );
    }

    @Test
    void rechazaTokenExpirado() {
        var overrides = new HashMap<String, Object>();
        overrides.put("exp", Date.from(NOW.minusSeconds(3600)));
        String token = idToken(keys, "k1", overrides);
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(token, "nonce-1")
        );
    }

    @Test
    void rechazaFirmaDeOtraClave() {
        String falso = idToken(rsaKeys(), "k1", Map.of()); // mismo kid, otra clave privada
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(falso, "nonce-1")
        );
    }

    @Test
    void rechazaTokenSinFirma() {
        String sinFirma = io.jsonwebtoken.Jwts.builder()
            .issuer(ISSUER)
            .subject("x")
            .compact();
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(sinFirma, "nonce-1")
        );
    }

    @Test
    void rechazaKidDesconocidoYSoloRefrescaElJwksUnaVezPorVentana() {
        String token = idToken(keys, "k-desconocido", Map.of());
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(token, "nonce-1")
        );
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(token, "nonce-1")
        );
        assertEquals(
            1,
            client.jwksFetches,
            "un token falso no debe forzar descargas repetidas del JWKS"
        );
    }

    @Test
    void siRotanLasClavesRefrescaElJwks() {
        validator.validate(idToken(keys, "k1", Map.of()), "nonce-1");
        assertEquals(1, client.jwksFetches);

        KeyPair nuevas = rsaKeys();
        client.jwks = jwks(nuevas, "k2");
        // pasada la ventana minima de refresco
        var despues = new IdTokenValidator(
            config(false),
            client,
            Clock.fixed(NOW.plusSeconds(120), ZoneOffset.UTC)
        );
        despues.validate(
            idToken(
                nuevas,
                "k2",
                Map.of("exp", Date.from(NOW.plusSeconds(600)))
            ),
            "nonce-1"
        );
        assertEquals(2, client.jwksFetches);
    }

    @Test
    void rechazaTokenSinDocumento() {
        var overrides = new HashMap<String, Object>();
        overrides.put("numero_documento", null);
        String token = idToken(keys, "k1", overrides);
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(token, "nonce-1")
        );
    }

    @Test
    void siNoSePuedeDescargarElJwksEsUnProblemaDeDisponibilidadNoDeAutenticacion() {
        client.jwksDown = true;
        String token = idToken(keys, "k1", Map.of());
        assertThrows(ServicioNoDisponibleException.class, () ->
            validator.validate(token, "nonce-1")
        );
    }

    @Test
    void rechazaTokenVacio() {
        assertThrows(GubUyAuthException.class, () ->
            validator.validate("", "nonce-1")
        );
        assertThrows(GubUyAuthException.class, () ->
            validator.validate(null, "nonce-1")
        );
    }
}
