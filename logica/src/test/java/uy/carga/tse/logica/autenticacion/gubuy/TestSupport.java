package uy.carga.tse.logica.autenticacion.gubuy;

import io.jsonwebtoken.Jwts;
import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Date;
import java.util.List;
import java.util.Map;

final class TestSupport {

    static final String ISSUER = "https://auth-testing.example.uy/oidc/v1";
    static final String CLIENT_ID = "cargauy-test";
    static final Instant NOW = Instant.parse("2026-10-08T12:00:00Z");
    static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private TestSupport() {}

    static GubUyConfig config(boolean pkce) {
        return config(pkce, "http://unused.invalid");
    }

    static GubUyConfig config(boolean pkce, String issuer) {
        byte[] key = new byte[32];
        java.util.Arrays.fill(key, (byte) 7);
        return new GubUyConfig(
            issuer,
            CLIENT_ID,
            "s3cret:/+",
            "https://api.carga.uy/auth/gubuy/callback",
            List.of("openid", "personal_info", "document", "email"),
            List.of("https://app.carga.uy/", "cargauy://auth"),
            key,
            pkce,
            "numero_documento",
            "nid",
            Duration.ofSeconds(1),
            Duration.ofSeconds(1),
            3,
            Duration.ofSeconds(30),
            Duration.ofMinutes(5),
            true
        );
    }

    static KeyPair rsaKeys() {
        return Jwts.SIG.RS256.keyPair().build();
    }

    /** JWKS con una unica clave publica RSA. */
    static String jwks(KeyPair keys, String kid) {
        RSAPublicKey pub = (RSAPublicKey) keys.getPublic();
        JsonObjectBuilder jwk = Json.createObjectBuilder()
            .add("kty", "RSA")
            .add("kid", kid)
            .add("use", "sig")
            .add("alg", "RS256")
            .add("n", b64(pub.getModulus()))
            .add("e", b64(pub.getPublicExponent()));
        return Json.createObjectBuilder()
            .add("keys", Json.createArrayBuilder().add(jwk))
            .build()
            .toString();
    }

    static String idToken(
        KeyPair keys,
        String kid,
        Map<String, Object> overrides
    ) {
        // la audiencia se resuelve antes de armar el builder: jjwt no permite reemplazarla despues
        String audiencia = overrides.containsKey("aud")
            ? (String) overrides.get("aud")
            : CLIENT_ID;
        var builder = Jwts.builder()
            .header()
            .keyId(kid)
            .and()
            .issuer(ISSUER)
            .subject("uid-123")
            .audience()
            .add(audiencia)
            .and()
            .issuedAt(Date.from(NOW.minusSeconds(10)))
            .expiration(Date.from(NOW.plusSeconds(300)))
            .claim("nonce", "nonce-1")
            .claim("numero_documento", "12345678")
            .claim("email", "chofer@example.uy")
            .claim("nid", 2);
        overrides.forEach((k, v) -> {
            if (v == null) {
                builder.claims().delete(k);
            } else {
                switch (k) {
                    case "iss" -> builder.issuer((String) v);
                    case "aud" -> {
                    } // ya aplicada arriba
                    case "exp" -> builder.expiration((Date) v);
                    default -> builder.claim(k, v);
                }
            }
        });
        return builder.signWith(keys.getPrivate(), Jwts.SIG.RS256).compact();
    }

    private static String b64(BigInteger n) {
        byte[] bytes = n.toByteArray();
        if (bytes.length > 1 && bytes[0] == 0) {
            bytes = java.util.Arrays.copyOfRange(bytes, 1, bytes.length);
        }
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
