package uy.carga.tse.logica.autenticacion.gubuy;

import io.jsonwebtoken.JwsHeader;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.LocatorAdapter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonNumber;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import java.io.StringReader;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.PublicKey;
import java.security.Signature;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.Cedula;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;

/**
 * Valida el id_token de ID Uruguay: firma (JWKS del proveedor), iss, aud, exp y nonce,
 * y extrae la identidad. Solo acepta firmas asimetricas publicadas en el JWKS.
 * <p>
 * Excepcion para testing: el ambiente de pruebas de ID Uruguay firma con una RSA de 1024 bits,
 * que la libreria JWT se niega a usar (RFC 7518 exige 2048). Con la variable de entorno
 * {@code GUBUY_ALLOW_WEAK_RSA=true} la firma se verifica con java.security.Signature, sin exigir
 * ese tamano. Esta apagada por defecto y no debe activarse en produccion.
 */
@ApplicationScoped
public class IdTokenValidator {

    private static final Logger log = LoggerFactory.getLogger(
        IdTokenValidator.class
    );

    private static final Duration CLOCK_SKEW = Duration.ofSeconds(60);
    private static final Duration MIN_JWKS_REFRESH = Duration.ofSeconds(30);

    private final GubUyConfig config;
    private final GubUyClient client;
    private final Clock clock;
    private final boolean permitirRsaDebil;

    private volatile Map<String, Key> keysByKid = Map.of();
    private volatile Instant lastJwksFetch = Instant.MIN;

    protected IdTokenValidator() {
        this.config = null;
        this.client = null;
        this.clock = null;
        this.permitirRsaDebil = false;
    }

    @Inject
    public IdTokenValidator(GubUyConfig config, GubUyClient client) {
        this(
            config,
            client,
            Clock.systemUTC(),
            "true".equalsIgnoreCase(System.getenv("GUBUY_ALLOW_WEAK_RSA"))
        );
    }

    IdTokenValidator(GubUyConfig config, GubUyClient client, Clock clock) {
        this(config, client, clock, false);
    }

    IdTokenValidator(
        GubUyConfig config,
        GubUyClient client,
        Clock clock,
        boolean permitirRsaDebil
    ) {
        this.config = config;
        this.client = client;
        this.clock = clock;
        this.permitirRsaDebil = permitirRsaDebil;
    }

    public GubUyIdentity validate(String idToken, String expectedNonce) {
        if (idToken == null || idToken.isBlank()) {
            throw new GubUyAuthException(
                CodigoError.GUBUY_TOKEN_INVALIDO,
                "gub.uy no entrego un id_token"
            );
        }
        String expectedIssuer = client.discover().issuer();
        Map<String, Object> claims = permitirRsaDebil
            ? verificarConClaveDebil(idToken, expectedIssuer)
            : verificarConLibreria(idToken, expectedIssuer);

        Object valorNonce = claims.get("nonce");
        String nonce = valorNonce instanceof String s ? s : null;
        if (nonce == null || !constantTimeEquals(nonce, expectedNonce)) {
            log.warn("id_token rechazado: el nonce no coincide");
            throw new GubUyAuthException(
                CodigoError.GUBUY_TOKEN_INVALIDO,
                "El nonce del id_token no coincide"
            );
        }

        // se lee como Object: si el proveedor manda la cedula como numero, no hay que fallar
        Object valorDocumento = claims.get(config.documentClaim());
        String documento =
            valorDocumento == null
                ? null
                : Cedula.normalizar(String.valueOf(valorDocumento));
        if (documento == null || documento.isBlank()) {
            // solo los nombres de los claims, nunca sus valores: sirve para ajustar GUBUY_DOCUMENT_CLAIM
            log.warn(
                "El id_token no trae el claim '{}'. Claims recibidos: {}",
                config.documentClaim(),
                claims.keySet()
            );
            throw new GubUyAuthException(
                CodigoError.GUBUY_TOKEN_INVALIDO,
                "El id_token no incluye el claim '" +
                    config.documentClaim() +
                    "'"
            );
        }
        String nivel =
            config.levelClaim() == null
                ? null
                : asString(claims.get(config.levelClaim()));
        return new GubUyIdentity(
            asString(claims.get("sub")),
            documento,
            asString(claims.get("email")),
            nivel
        );
    }

    // ---- verificacion con la libreria (camino normal) ----

    private Map<String, Object> verificarConLibreria(
        String idToken,
        String expectedIssuer
    ) {
        try {
            return Jwts.parser()
                .keyLocator(
                    new LocatorAdapter<Key>() {
                        @Override
                        protected Key locate(JwsHeader header) {
                            return keyFor(
                                header.getKeyId(),
                                header.getAlgorithm()
                            );
                        }
                    }
                )
                .requireIssuer(expectedIssuer)
                .requireAudience(config.clientId())
                .clock(() -> Date.from(clock.instant()))
                .clockSkewSeconds(CLOCK_SKEW.toSeconds())
                .build()
                .parseSignedClaims(idToken)
                .getPayload();
        } catch (ServicioNoDisponibleException e) {
            throw e; // no se pudo obtener el JWKS: es un problema de disponibilidad, no del token
        } catch (JwtException | IllegalArgumentException e) {
            log.warn("id_token rechazado: {}", e.getMessage());
            throw new GubUyAuthException(
                CodigoError.GUBUY_TOKEN_INVALIDO,
                "El id_token de gub.uy es invalido",
                e
            );
        }
    }

    // ---- verificacion propia (solo con GUBUY_ALLOW_WEAK_RSA=true, para el ambiente de testing) ----

    private Map<String, Object> verificarConClaveDebil(
        String idToken,
        String expectedIssuer
    ) {
        log.warn(
            "GUBUY_ALLOW_WEAK_RSA=true: la firma RS256 se verifica sin exigir 2048 bits (solo para testing)"
        );
        String[] partes = idToken.split("\\.", -1);
        if (partes.length != 3) {
            throw rechazo("no tiene tres partes");
        }
        JsonObject header = leerObjetoDelToken(partes[0]);
        if (!"RS256".equals(texto(header, "alg"))) {
            throw rechazo("algoritmo no admitido: " + texto(header, "alg"));
        }
        Key clave = keyFor(texto(header, "kid"), texto(header, "alg"));
        try {
            Signature firma = Signature.getInstance("SHA256withRSA");
            firma.initVerify((PublicKey) clave);
            firma.update(
                (partes[0] + "." + partes[1]).getBytes(
                    StandardCharsets.US_ASCII
                )
            );
            if (!firma.verify(Base64.getUrlDecoder().decode(partes[2]))) {
                throw rechazo("la firma no es valida");
            }
        } catch (GeneralSecurityException | IllegalArgumentException e) {
            throw rechazo("no se pudo verificar la firma: " + e.getMessage());
        }

        Map<String, Object> claims = new LinkedHashMap<>();
        leerObjetoDelToken(partes[1]).forEach((nombre, valor) ->
            claims.put(nombre, aJava(valor))
        );

        if (!expectedIssuer.equals(claims.get("iss"))) {
            throw rechazo("el emisor (iss) no coincide");
        }
        Object aud = claims.get("aud");
        boolean audienciaOk =
            aud instanceof String s
                ? s.equals(config.clientId())
                : aud instanceof List<?> lista &&
                  lista.contains(config.clientId());
        if (!audienciaOk) {
            throw rechazo("la audiencia (aud) no coincide");
        }
        long ahora = clock.instant().getEpochSecond();
        if (
            !(claims.get("exp") instanceof Number exp) ||
            ahora - CLOCK_SKEW.toSeconds() >= exp.longValue()
        ) {
            throw rechazo("el token vencio o no trae exp");
        }
        if (
            claims.get("nbf") instanceof Number nbf &&
            ahora + CLOCK_SKEW.toSeconds() < nbf.longValue()
        ) {
            throw rechazo("el token todavia no es valido (nbf)");
        }
        return claims;
    }

    private static GubUyAuthException rechazo(String detalle) {
        log.warn("id_token rechazado: {}", detalle);
        return new GubUyAuthException(
            CodigoError.GUBUY_TOKEN_INVALIDO,
            "El id_token de gub.uy es invalido"
        );
    }

    private static JsonObject leerObjetoDelToken(String base64url) {
        String cuerpo = new String(
            Base64.getUrlDecoder().decode(base64url),
            StandardCharsets.UTF_8
        );
        try (var lector = Json.createReader(new StringReader(cuerpo))) {
            return lector.readObject();
        } catch (RuntimeException e) {
            throw rechazo("una parte del token no se puede leer");
        }
    }

    private static Object aJava(JsonValue valor) {
        switch (valor.getValueType()) {
            case STRING:
                return ((JsonString) valor).getString();
            case NUMBER:
                JsonNumber numero = (JsonNumber) valor;
                if (numero.isIntegral()) {
                    return numero.longValue();
                }
                return numero.doubleValue();
            case TRUE:
                return Boolean.TRUE;
            case FALSE:
                return Boolean.FALSE;
            case ARRAY:
                List<Object> lista = new ArrayList<>();
                for (JsonValue item : (JsonArray) valor) {
                    lista.add(aJava(item));
                }
                return lista;
            case OBJECT:
                Map<String, Object> mapa = new LinkedHashMap<>();
                ((JsonObject) valor).forEach((k, v) -> mapa.put(k, aJava(v)));
                return mapa;
            default:
                return null;
        }
    }

    // ---- JWKS ----

    private Key keyFor(String kid, String alg) {
        Key key = kid == null ? null : keysByKid.get(kid);
        if (key == null && kid != null) {
            refreshJwks(); // puede haber rotacion de claves
            key = keysByKid.get(kid);
        }
        if (key == null) {
            // solo metadatos del encabezado, nunca claves ni valores del token
            log.warn(
                "id_token con alg={} kid={}; kids del JWKS: {}",
                alg,
                kid,
                keysByKid.keySet()
            );
            throw new GubUyAuthException(
                CodigoError.GUBUY_TOKEN_INVALIDO,
                "El id_token esta firmado con una clave desconocida"
            );
        }
        return key;
    }

    private synchronized void refreshJwks() {
        Instant now = clock.instant();
        if (now.isBefore(lastJwksFetch.plus(MIN_JWKS_REFRESH))) {
            return; // evita que un token falso fuerce descargas repetidas
        }
        JsonObject jwks = leerJson(client.fetchJwks());
        Map<String, Key> keys = new HashMap<>();
        if (jwks.get("keys") instanceof JsonArray array) {
            for (JsonValue valor : array) {
                if (!(valor instanceof JsonObject jwk)) {
                    continue;
                }
                String kid = texto(jwk, "kid");
                String use = texto(jwk, "use");
                if (
                    kid == null ||
                    !"RSA".equals(texto(jwk, "kty")) ||
                    (use != null && !"sig".equals(use))
                ) {
                    continue; // solo claves RSA de firma identificadas por kid
                }
                try {
                    // se arma con n y e: el x5c de ID Uruguay no es un certificado X.509 valido
                    keys.put(
                        kid,
                        clavePublicaRsa(texto(jwk, "n"), texto(jwk, "e"))
                    );
                } catch (
                    GeneralSecurityException
                    | IllegalArgumentException e
                ) {
                    log.warn(
                        "JWKS: se ignora la clave kid={}: {}",
                        kid,
                        e.getMessage()
                    );
                }
            }
        }
        log.info("JWKS cargado: {} clave(s) utilizable(s)", keys.size());
        keysByKid = Map.copyOf(keys);
        lastJwksFetch = now;
    }

    private static JsonObject leerJson(String cuerpo) {
        try (var lector = Json.createReader(new StringReader(cuerpo))) {
            return lector.readObject();
        } catch (RuntimeException e) {
            throw new ServicioNoDisponibleException(
                CodigoError.GUBUY_NO_DISPONIBLE,
                "JWKS invalido en la respuesta de gub.uy",
                e
            );
        }
    }

    private static String texto(JsonObject json, String campo) {
        return json.get(campo) instanceof JsonString s &&
            !s.getString().isBlank()
            ? s.getString()
            : null;
    }

    private static RSAPublicKey clavePublicaRsa(String n, String e)
        throws GeneralSecurityException {
        if (n == null || e == null) {
            throw new IllegalArgumentException("la clave no trae n y e");
        }
        BigInteger modulo = new BigInteger(1, Base64.getUrlDecoder().decode(n));
        BigInteger exponente = new BigInteger(
            1,
            Base64.getUrlDecoder().decode(e)
        );
        return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(
            new RSAPublicKeySpec(modulo, exponente)
        );
    }

    private static String asString(Object v) {
        return v == null ? null : String.valueOf(v);
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        return MessageDigest.isEqual(
            a.getBytes(StandardCharsets.UTF_8),
            b.getBytes(StandardCharsets.UTF_8)
        );
    }
}
