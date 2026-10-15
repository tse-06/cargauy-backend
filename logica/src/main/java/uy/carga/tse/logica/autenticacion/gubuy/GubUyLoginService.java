package uy.carga.tse.logica.autenticacion.gubuy;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Optional;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

/**
 * Orquesta el Authorization Code Flow contra ID Uruguay sin guardar estado en el servidor:
 * {@link #start} arma el pedido de autorizacion y el "flowToken" que va a la cookie;
 * {@link #complete} lo verifica al volver de gub.uy, canjea el code y valida el id_token.
 */
@ApplicationScoped
public class GubUyLoginService {

    private static final SecureRandom RANDOM = new SecureRandom();

    public record LoginStart(
        URI authorizationUri,
        String flowToken,
        Duration flowTtl
    ) {}

    public record LoginResult(
        GubUyIdentity identity,
        String returnTo,
        String rol
    ) {}

    private final GubUyConfig config;
    private final GubUyClient client;
    private final FlowStateService flows;
    private final IdTokenValidator validator;

    protected GubUyLoginService() {
        this.config = null;
        this.client = null;
        this.flows = null;
        this.validator = null;
    }

    @Inject
    public GubUyLoginService(
        GubUyConfig config,
        GubUyClient client,
        FlowStateService flows,
        IdTokenValidator validator
    ) {
        this.config = config;
        this.client = client;
        this.flows = flows;
        this.validator = validator;
    }

    public LoginStart start(String requestedReturnTo) {
        return start(requestedReturnTo, null);
    }

    /**
     * @param rolSolicitado rol con el que el usuario quiere entrar (un usuario puede tener varios, DA001);
     *                      null si el cliente no lo indica. Se valida contra los roles de gub.uy recien en {@link SesionEmisorJwt}.
     */
    public LoginStart start(String requestedReturnTo, String rolSolicitado) {
        String returnTo = resolveReturnTo(requestedReturnTo);
        String state = randomToken();
        String nonce = randomToken();
        String verifier = config.usePkce() ? randomToken() : null;

        StringBuilder query = new StringBuilder()
            .append("response_type=code")
            .append("&client_id=")
            .append(enc(config.clientId()))
            .append("&redirect_uri=")
            .append(enc(config.redirectUri()))
            .append("&scope=")
            .append(enc(String.join(" ", config.scopes())))
            .append("&state=")
            .append(enc(state))
            .append("&nonce=")
            .append(enc(nonce));
        if (verifier != null) {
            query
                .append("&code_challenge=")
                .append(enc(s256(verifier)))
                .append("&code_challenge_method=S256");
        }
        String authEndpoint = client.discover().authorizationEndpoint();
        URI uri = URI.create(
            authEndpoint + (authEndpoint.contains("?") ? "&" : "?") + query
        );

        String flowToken = flows.sign(
            new FlowStateService.Flow(
                state,
                nonce,
                verifier,
                returnTo,
                rolSolicitado
            )
        );
        return new LoginStart(uri, flowToken, config.flowTtl());
    }

    public LoginResult complete(String code, String state, String flowToken) {
        FlowStateService.Flow flow = flows.verify(flowToken);
        // la cookie de flujo ata el callback al navegador que inicio el login (evita login CSRF)
        if (
            state == null ||
            !MessageDigest.isEqual(
                state.getBytes(StandardCharsets.UTF_8),
                flow.state().getBytes(StandardCharsets.UTF_8)
            )
        ) {
            throw new GubUyAuthException(
                CodigoError.GUBUY_ESTADO_INVALIDO,
                "El parametro state no coincide"
            );
        }
        if (code == null || code.isBlank()) {
            throw new GubUyAuthException(
                CodigoError.GUBUY_ERROR,
                "Falta el codigo de autorizacion"
            );
        }
        GubUyClient.TokenResponse tokens = client.exchangeCode(
            code,
            flow.codeVerifier()
        );
        GubUyIdentity identity = validator.validate(
            tokens.idToken(),
            flow.nonce()
        );
        return new LoginResult(identity, flow.returnTo(), flow.rol());
    }

    /** Destino al que volver si gub.uy informo un error (p. ej. el usuario cancelo). */
    public Optional<String> returnToFrom(String flowToken) {
        try {
            return Optional.of(flows.verify(flowToken).returnTo());
        } catch (GubUyAuthException e) {
            return Optional.empty();
        }
    }

    // ---- returnTo (anti open-redirect) ----

    String resolveReturnTo(String requested) {
        if (requested == null || requested.isBlank()) {
            return config.allowedReturnPrefixes().get(0);
        }
        if (
            requested.contains("#") || requested.chars().anyMatch(c -> c < 0x20)
        ) {
            throw new ValidacionNegocioException(
                CodigoError.RETURN_TO_INVALIDO,
                "returnTo invalido"
            );
        }
        for (String prefix : config.allowedReturnPrefixes()) {
            if (matches(requested, prefix)) {
                return requested;
            }
        }
        throw new ValidacionNegocioException(
            CodigoError.RETURN_TO_INVALIDO,
            "returnTo no permitido"
        );
    }

    private static boolean matches(String url, String prefix) {
        if (!url.startsWith(prefix)) {
            return false;
        }
        if (url.length() == prefix.length() || prefix.endsWith("/")) {
            return true;
        }
        char next = url.charAt(prefix.length()); // evita https://app.carga.uy.evil.com o https://app.carga.uy@evil.com
        return next == '/' || next == '?';
    }

    // ---- utilidades ----

    private static String randomToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    static String s256(String verifier) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(
                verifier.getBytes(StandardCharsets.US_ASCII)
            );
            return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private static String enc(String s) {
        return URLEncoder.encode(s, StandardCharsets.UTF_8);
    }
}
