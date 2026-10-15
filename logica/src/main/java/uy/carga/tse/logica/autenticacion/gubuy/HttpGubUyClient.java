package uy.carga.tse.logica.autenticacion.gubuy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.IOException;
import java.io.StringReader;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;

/**
 * Cliente HTTP hacia ID Uruguay con timeouts, reintentos acotados (solo en pedidos idempotentes)
 * y circuit breaker. Autenticacion del cliente: client_secret_basic.
 */
@ApplicationScoped
public class HttpGubUyClient implements GubUyClient {

    private static final Logger log = LoggerFactory.getLogger(
        HttpGubUyClient.class
    );
    private static final Duration DISCOVERY_TTL = Duration.ofHours(1);
    private static final int IDEMPOTENT_ATTEMPTS = 2;

    private final GubUyConfig config;
    private final HttpClient http;
    private final CircuitBreaker breaker;
    private final Clock clock;

    private volatile OidcEndpoints cachedEndpoints;
    private volatile Instant cachedAt = Instant.MIN;

    /** Requerido por CDI para crear el proxy del bean. */
    protected HttpGubUyClient() {
        this.config = null;
        this.http = null;
        this.breaker = null;
        this.clock = null;
    }

    @Inject
    public HttpGubUyClient(GubUyConfig config) {
        this(
            config,
            HttpClient.newBuilder()
                .connectTimeout(config.connectTimeout())
                .followRedirects(HttpClient.Redirect.NEVER)
                .build(),
            Clock.systemUTC()
        );
    }

    HttpGubUyClient(GubUyConfig config, HttpClient http, Clock clock) {
        this.config = config;
        this.http = http;
        this.clock = clock;
        this.breaker = new CircuitBreaker(
            config.breakerFailureThreshold(),
            config.breakerOpenDuration(),
            clock
        );
    }

    @Override
    public OidcEndpoints discover() {
        OidcEndpoints cached = cachedEndpoints;
        if (
            cached != null &&
            clock.instant().isBefore(cachedAt.plus(DISCOVERY_TTL))
        ) {
            return cached;
        }
        String url =
            stripTrailingSlash(config.issuer()) +
            "/.well-known/openid-configuration";
        JsonObject json = parse(getWithRetry(url));
        OidcEndpoints endpoints = new OidcEndpoints(
            string(json, "issuer"),
            string(json, "authorization_endpoint"),
            string(json, "token_endpoint"),
            string(json, "jwks_uri")
        );
        cachedEndpoints = endpoints;
        cachedAt = clock.instant();
        return endpoints;
    }

    @Override
    public TokenResponse exchangeCode(String code, String codeVerifier) {
        String tokenEndpoint = discover().tokenEndpoint();
        StringBuilder form = new StringBuilder()
            .append("grant_type=authorization_code")
            .append("&code=")
            .append(enc(code))
            .append("&redirect_uri=")
            .append(enc(config.redirectUri()));
        if (codeVerifier != null) {
            form.append("&code_verifier=").append(enc(codeVerifier));
        }
        // RFC 6749 2.3.1: client_id y client_secret se codifican como x-www-form-urlencoded antes de base64
        String basic = Base64.getEncoder().encodeToString(
            (
                enc(config.clientId()) +
                ":" +
                enc(config.clientSecret())
            ).getBytes(StandardCharsets.UTF_8)
        );
        HttpRequest request = HttpRequest.newBuilder(URI.create(tokenEndpoint))
            .timeout(config.requestTimeout())
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Authorization", "Basic " + basic)
            .POST(HttpRequest.BodyPublishers.ofString(form.toString()))
            .build();

        // Sin reintentos: el authorization code solo se puede canjear una vez.
        HttpResponse<String> response = breaker.execute(() -> enviar(request));
        int status = response.statusCode();
        if (status == 200) {
            JsonObject json = parse(response.body());
            return new TokenResponse(string(json, "id_token"));
        }
        String error = "invalid_grant";
        try {
            JsonObject json = parse(response.body());
            if (json.get("error") instanceof JsonString s) {
                error = s.getString();
            }
        } catch (RuntimeException ignored) {
            // cuerpo no JSON: se deja invalid_grant
        }
        log.warn(
            "Token endpoint de gub.uy rechazo el intercambio: status={} error={}",
            status,
            error
        );
        throw new GubUyAuthException(
            CodigoError.GUBUY_CODIGO_RECHAZADO,
            "gub.uy rechazo el codigo de autorizacion (" + error + ")"
        );
    }

    @Override
    public String fetchJwks() {
        String jwksUri = discover().jwksUri();
        return getWithRetry(jwksUri);
    }

    // ---- internos ----

    private String getWithRetry(String url) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
            .timeout(config.requestTimeout())
            .header("Accept", "application/json")
            .GET()
            .build();
        ServicioNoDisponibleException last = null;
        for (int attempt = 1; attempt <= IDEMPOTENT_ATTEMPTS; attempt++) {
            try {
                HttpResponse<String> response = breaker.execute(() ->
                    enviar(request)
                );
                if (response.statusCode() == 200) {
                    return response.body();
                }
                throw new ServicioNoDisponibleException(
                    CodigoError.GUBUY_NO_DISPONIBLE,
                    "gub.uy respondio " + response.statusCode() + " en " + url
                );
            } catch (ServicioNoDisponibleException e) {
                last = e;
                if (attempt < IDEMPOTENT_ATTEMPTS) {
                    sleep(Duration.ofMillis(200L * attempt));
                }
            }
        }
        throw last;
    }

    /**
     * Envia el pedido y trata un 5xx como falla de disponibilidad. Tiene que lanzar aca, dentro del
     * circuit breaker: si el status se revisara despues, el breaker veria un "exito" y nunca se abriria.
     */
    private HttpResponse<String> enviar(HttpRequest request) {
        HttpResponse<String> response = send(request);
        if (response.statusCode() >= 500) {
            throw new ServicioNoDisponibleException(
                CodigoError.GUBUY_NO_DISPONIBLE,
                "gub.uy respondio " + response.statusCode()
            );
        }
        return response;
    }

    private HttpResponse<String> send(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (IOException e) {
            throw new ServicioNoDisponibleException(
                CodigoError.GUBUY_NO_DISPONIBLE,
                "Error de red hacia gub.uy: " + e.getClass().getSimpleName(),
                e
            );
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ServicioNoDisponibleException(
                CodigoError.GUBUY_NO_DISPONIBLE,
                "Interrumpido esperando a gub.uy",
                e
            );
        }
    }

    private static void sleep(Duration d) {
        try {
            Thread.sleep(d.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static JsonObject parse(String body) {
        try (var reader = Json.createReader(new StringReader(body))) {
            return reader.readObject();
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                CodigoError.GUBUY_NO_DISPONIBLE,
                "Respuesta invalida de gub.uy",
                e
            );
        }
    }

    private static String string(JsonObject json, String field) {
        JsonValue v = json.get(field);
        if (v instanceof JsonString s && !s.getString().isBlank()) {
            return s.getString();
        }
        throw new ServicioNoDisponibleException(
            CodigoError.GUBUY_NO_DISPONIBLE,
            "Respuesta de gub.uy sin el campo '" + field + "'"
        );
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }

    private static String stripTrailingSlash(String s) {
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
