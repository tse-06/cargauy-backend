package uy.carga.tse.logica.autenticacion.gubuy;

import static org.junit.jupiter.api.Assertions.*;
import static uy.carga.tse.logica.autenticacion.gubuy.TestSupport.config;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.http.HttpClient;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Base64;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;

/** Prueba el cliente contra un servidor HTTP real local que hace de gub.uy. */
class HttpGubUyClientTest {

    private HttpServer server;
    private String base;
    private final AtomicInteger discoveryHits = new AtomicInteger();
    private final AtomicInteger tokenHits = new AtomicInteger();
    private final AtomicReference<String> tokenAuthHeader =
        new AtomicReference<>();
    private final AtomicReference<String> tokenBody = new AtomicReference<>();
    private volatile int tokenStatus = 200;
    private volatile String tokenResponse = "{\"id_token\":\"el.id.token\"}";
    private volatile long tokenDelayMs = 0;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        base = "http://127.0.0.1:" + server.getAddress().getPort();
        server.createContext("/.well-known/openid-configuration", ex -> {
            discoveryHits.incrementAndGet();
            reply(
                ex,
                200,
                "{\"issuer\":\"" +
                    base +
                    "\",\"authorization_endpoint\":\"" +
                    base +
                    "/authorize\"," +
                    "\"token_endpoint\":\"" +
                    base +
                    "/token\",\"jwks_uri\":\"" +
                    base +
                    "/jwks\"}"
            );
        });
        server.createContext("/token", ex -> {
            tokenHits.incrementAndGet();
            tokenAuthHeader.set(
                ex.getRequestHeaders().getFirst("Authorization")
            );
            tokenBody.set(
                new String(
                    ex.getRequestBody().readAllBytes(),
                    StandardCharsets.UTF_8
                )
            );
            if (tokenDelayMs > 0) {
                try {
                    Thread.sleep(tokenDelayMs);
                } catch (InterruptedException ignored) {}
            }
            reply(ex, tokenStatus, tokenResponse);
        });
        server.createContext("/jwks", ex -> reply(ex, 200, "{\"keys\":[]}"));
        server.start();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static void reply(
        com.sun.net.httpserver.HttpExchange ex,
        int status,
        String body
    ) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        try {
            ex.sendResponseHeaders(status, bytes.length);
            ex.getResponseBody().write(bytes);
        } catch (IOException ignored) {
            // el cliente ya corto por timeout
        } finally {
            ex.close();
        }
    }

    private HttpGubUyClient client() {
        var cfg = config(false, base);
        return new HttpGubUyClient(
            cfg,
            HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(1))
                .build(),
            Clock.systemUTC()
        );
    }

    @Test
    void descubreLosEndpointsYCacheaElResultado() {
        var c = client();
        var e = c.discover();
        assertEquals(base + "/token", e.tokenEndpoint());
        assertEquals(base + "/authorize", e.authorizationEndpoint());
        c.discover();
        c.fetchJwks();
        assertEquals(1, discoveryHits.get(), "el discovery se cachea");
    }

    @Test
    void canjeaElCodigoConClientSecretBasicYFormUrlencoded() {
        var response = client().exchangeCode("co/de+1", "ver1");
        assertEquals("el.id.token", response.idToken());

        // secreto con caracteres especiales: se codifica antes de base64 (RFC 6749 2.3.1)
        String esperado =
            "Basic " +
            Base64.getEncoder().encodeToString(
                "cargauy-test:s3cret%3A%2F%2B".getBytes(StandardCharsets.UTF_8)
            );
        assertEquals(esperado, tokenAuthHeader.get());
        assertTrue(tokenBody.get().contains("grant_type=authorization_code"));
        assertTrue(tokenBody.get().contains("code=co%2Fde%2B1"));
        assertTrue(tokenBody.get().contains("code_verifier=ver1"));
        assertTrue(
            tokenBody
                .get()
                .contains(
                    "redirect_uri=https%3A%2F%2Fapi.carga.uy%2Fauth%2Fgubuy%2Fcallback"
                )
        );
    }

    @Test
    void unRechazoDeGubUyEsAuthExceptionYNoSeReintenta() {
        tokenStatus = 400;
        tokenResponse = "{\"error\":\"invalid_grant\"}";
        var e = assertThrows(GubUyAuthException.class, () ->
            client().exchangeCode("x", null)
        );
        assertEquals(CodigoError.GUBUY_CODIGO_RECHAZADO, e.codigo());
        assertEquals(
            1,
            tokenHits.get(),
            "el code es de un solo uso: no se reintenta"
        );
    }

    @Test
    void un5xxEsUnavailable() {
        tokenStatus = 503;
        tokenResponse = "{}";
        assertThrows(ServicioNoDisponibleException.class, () ->
            client().exchangeCode("x", null)
        );
    }

    @Test
    void respondeLentoSeCortaPorTimeout() {
        tokenDelayMs = 3000;
        long t0 = System.nanoTime();
        assertThrows(ServicioNoDisponibleException.class, () ->
            client().exchangeCode("x", null)
        );
        assertTrue(
            Duration.ofNanos(System.nanoTime() - t0).toMillis() < 2500,
            "debe respetar el timeout de 1s"
        );
    }

    @Test
    void trasFallasSostenidasElBreakerCortaSinLlamarAGubUy() {
        var c = client(); // umbral = 3 fallas
        tokenStatus = 503;
        tokenResponse = "{}";
        for (int i = 0; i < 3; i++) {
            assertThrows(ServicioNoDisponibleException.class, () ->
                c.exchangeCode("x", null)
            );
        }
        int antes = tokenHits.get();
        assertThrows(ServicioNoDisponibleException.class, () ->
            c.exchangeCode("x", null)
        );
        assertEquals(
            antes,
            tokenHits.get(),
            "con el breaker abierto no se hace el pedido"
        );
    }

    @Test
    void gubUyCaidoDuranteElDiscoveryEsUnavailable() throws IOException {
        server.stop(0); // puerto cerrado: connection refused
        assertThrows(ServicioNoDisponibleException.class, () ->
            client().discover()
        );
    }
}
