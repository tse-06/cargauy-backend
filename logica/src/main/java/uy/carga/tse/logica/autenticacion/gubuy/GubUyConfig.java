package uy.carga.tse.logica.autenticacion.gubuy;

import java.time.Duration;
import java.util.List;

/**
 * Configuracion del cliente OIDC (Relying Party) frente a ID Uruguay.
 * Se carga desde variables de entorno (ver {@link GubUyConfigProducer}); nada de esto
 * va en el repositorio (R16).
 *
 * @param issuer                 URL base del OpenID Provider; de ahi se descubren los endpoints
 * @param clientId               client_id asignado por AGESIC al registrar la RP
 * @param clientSecret           client_secret asignado por AGESIC
 * @param redirectUri            redirect_uri registrada en AGESIC (apunta a /auth/gubuy/callback)
 * @param scopes                 scopes solicitados; siempre debe incluir "openid"
 * @param allowedReturnPrefixes  destinos permitidos para la redireccion final (frontends y deep link movil)
 * @param flowKey                clave HMAC (>= 32 bytes) para firmar la cookie de flujo
 * @param usePkce                si se envia code_challenge/code_verifier (confirmar soporte con AGESIC)
 * @param documentClaim          nombre del claim con el numero de documento
 * @param levelClaim             nombre del claim con el nivel de seguridad de la identidad (puede ser null)
 * @param connectTimeout         timeout de conexion hacia gub.uy
 * @param requestTimeout         timeout por pedido hacia gub.uy
 * @param breakerFailureThreshold fallas consecutivas que abren el circuit breaker
 * @param breakerOpenDuration    tiempo que el breaker permanece abierto antes de probar de nuevo
 * @param flowTtl                vigencia de la cookie de flujo (state/nonce)
 * @param cookieSecure           marca Secure en la cookie; solo desactivar en desarrollo local sin TLS
 */
public record GubUyConfig(
    String issuer,
    String clientId,
    String clientSecret,
    String redirectUri,
    List<String> scopes,
    List<String> allowedReturnPrefixes,
    byte[] flowKey,
    boolean usePkce,
    String documentClaim,
    String levelClaim,
    Duration connectTimeout,
    Duration requestTimeout,
    int breakerFailureThreshold,
    Duration breakerOpenDuration,
    Duration flowTtl,
    boolean cookieSecure
) {
    public GubUyConfig {
        require(issuer, "issuer");
        require(clientId, "clientId");
        require(clientSecret, "clientSecret");
        require(redirectUri, "redirectUri");
        if (scopes == null || !scopes.contains("openid")) {
            throw new IllegalArgumentException("scopes debe incluir 'openid'");
        }
        if (allowedReturnPrefixes == null || allowedReturnPrefixes.isEmpty()) {
            throw new IllegalArgumentException(
                "allowedReturnPrefixes no puede ser vacio"
            );
        }
        if (flowKey == null || flowKey.length < 32) {
            throw new IllegalArgumentException(
                "flowKey debe tener al menos 32 bytes"
            );
        }
        scopes = List.copyOf(scopes);
        allowedReturnPrefixes = List.copyOf(allowedReturnPrefixes);
        flowKey = flowKey.clone();
    }

    private static void require(String v, String name) {
        if (v == null || v.isBlank()) {
            throw new IllegalArgumentException("Falta configuracion: " + name);
        }
    }

    /** Evita que el secreto y la clave terminen en logs. */
    @Override
    public String toString() {
        return (
            "GubUyConfig[issuer=" +
            issuer +
            ", clientId=" +
            clientId +
            ", redirectUri=" +
            redirectUri +
            ", scopes=" +
            scopes +
            ", usePkce=" +
            usePkce +
            ", secretos=<ocultos>]"
        );
    }
}
