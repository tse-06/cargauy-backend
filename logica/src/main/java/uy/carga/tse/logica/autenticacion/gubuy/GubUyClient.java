package uy.carga.tse.logica.autenticacion.gubuy;

/**
 * Acceso a los endpoints de ID Uruguay. Es una interfaz para poder simular a gub.uy
 * en las pruebas (incluido el escenario "gub.uy no responde").
 */
public interface GubUyClient {
    /** Metadatos del OpenID Provider (/.well-known/openid-configuration). */
    OidcEndpoints discover();

    /** Intercambia el authorization code por tokens. El code es de un solo uso: no se reintenta. */
    TokenResponse exchangeCode(String code, String codeVerifier);

    /** Descarga el JWKS (JSON) con las claves publicas que firman los id_token. */
    String fetchJwks();

    record OidcEndpoints(
        String issuer,
        String authorizationEndpoint,
        String tokenEndpoint,
        String jwksUri
    ) {}

    record TokenResponse(String idToken) {}
}
