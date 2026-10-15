package uy.carga.tse.logica.autenticacion.gubuy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import java.time.Duration;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.function.Function;

/**
 * Lee la configuracion desde variables de entorno (o propiedades de sistema con el mismo nombre).
 * Variables:
 * <pre>
 * GUBUY_ISSUER, GUBUY_CLIENT_ID, GUBUY_CLIENT_SECRET, GUBUY_REDIRECT_URI   (obligatorias)
 * GUBUY_ALLOWED_RETURN   prefijos separados por coma (obligatoria)
 * AUTH_FLOW_KEY          clave HMAC en base64, >= 32 bytes (obligatoria)
 * GUBUY_SCOPES           por defecto "openid personal_info document email auth_info"
 * GUBUY_PKCE             por defecto false
 * GUBUY_DOCUMENT_CLAIM   por defecto numero_documento
 * GUBUY_LEVEL_CLAIM      por defecto nid
 * GUBUY_COOKIE_SECURE    por defecto true
 * </pre>
 * Los nombres de claims y scopes por defecto deben confirmarse con la documentacion tecnica de ID Uruguay.
 */
// beans.xml esta en modo "annotated": sin esta anotacion CDI no descubre el @Produces
@ApplicationScoped
public class GubUyConfigProducer {

    @Produces
    @Singleton
    public GubUyConfig gubUyConfig() {
        return fromLookup(GubUyConfigProducer::lookup);
    }

    static GubUyConfig fromLookup(Function<String, String> get) {
        String scopes = orDefault(
            get.apply("GUBUY_SCOPES"),
            "openid personal_info document email auth_info"
        );
        String levelClaim = orDefault(get.apply("GUBUY_LEVEL_CLAIM"), "nid");
        String flowKey = get.apply("AUTH_FLOW_KEY");
        if (flowKey == null || flowKey.isBlank()) {
            throw new IllegalStateException(
                "Falta configuracion: AUTH_FLOW_KEY"
            );
        }
        String allowed = get.apply("GUBUY_ALLOWED_RETURN");
        if (allowed == null || allowed.isBlank()) {
            throw new IllegalStateException(
                "Falta configuracion: GUBUY_ALLOWED_RETURN"
            );
        }
        return new GubUyConfig(
            get.apply("GUBUY_ISSUER"),
            get.apply("GUBUY_CLIENT_ID"),
            get.apply("GUBUY_CLIENT_SECRET"),
            get.apply("GUBUY_REDIRECT_URI"),
            Arrays.asList(scopes.trim().split("\\s+")),
            Arrays.stream(allowed.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList(),
            Base64.getDecoder().decode(flowKey.trim()),
            Boolean.parseBoolean(orDefault(get.apply("GUBUY_PKCE"), "false")),
            orDefault(get.apply("GUBUY_DOCUMENT_CLAIM"), "numero_documento"),
            levelClaim.isBlank() ? null : levelClaim,
            Duration.ofSeconds(3),
            Duration.ofSeconds(5),
            5,
            Duration.ofSeconds(30),
            Duration.ofMinutes(5),
            Boolean.parseBoolean(
                orDefault(get.apply("GUBUY_COOKIE_SECURE"), "true")
            )
        );
    }

    private static String lookup(String name) {
        String v = System.getenv(name);
        return v != null ? v : System.getProperty(name);
    }

    private static String orDefault(String v, String def) {
        return v == null || v.isBlank() ? def : v;
    }
}
