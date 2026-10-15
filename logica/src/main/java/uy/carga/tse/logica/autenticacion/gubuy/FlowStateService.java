package uy.carga.tse.logica.autenticacion.gubuy;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.time.Clock;
import java.util.Date;
import javax.crypto.SecretKey;
import uy.carga.tse.datatypes.error.CodigoError;

/**
 * Firma y valida el "estado del flujo" (state, nonce, code_verifier, returnTo) como un JWT HS256 de vida corta.
 * Viaja en una cookie HttpOnly: el servidor no guarda nada en sesion, asi que sigue siendo stateless (AC06).
 */
@ApplicationScoped
public class FlowStateService {

    private static final String ISSUER = "cargauy-gubuy-flow";

    public record Flow(
        String state,
        String nonce,
        String codeVerifier,
        String returnTo,
        String rol
    ) {}

    private final SecretKey key;
    private final GubUyConfig config;
    private final Clock clock;

    protected FlowStateService() {
        this.key = null;
        this.config = null;
        this.clock = null;
    }

    @Inject
    public FlowStateService(GubUyConfig config) {
        this(config, Clock.systemUTC());
    }

    FlowStateService(GubUyConfig config, Clock clock) {
        this.config = config;
        this.key = Keys.hmacShaKeyFor(config.flowKey());
        this.clock = clock;
    }

    public String sign(Flow flow) {
        Date now = Date.from(clock.instant());
        var builder = Jwts.builder()
            .issuer(ISSUER)
            .issuedAt(now)
            .expiration(Date.from(clock.instant().plus(config.flowTtl())))
            .claim("state", flow.state())
            .claim("nonce", flow.nonce())
            .claim("returnTo", flow.returnTo());
        if (flow.rol() != null) {
            builder.claim("rol", flow.rol());
        }
        if (flow.codeVerifier() != null) {
            builder.claim("cv", flow.codeVerifier());
        }
        return builder.signWith(key).compact();
    }

    public Flow verify(String token) {
        if (token == null || token.isBlank()) {
            throw new GubUyAuthException(
                CodigoError.GUBUY_ESTADO_INVALIDO,
                "Falta el estado del flujo de autenticacion"
            );
        }
        try {
            Claims c = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
            return new Flow(
                c.get("state", String.class),
                c.get("nonce", String.class),
                c.get("cv", String.class),
                c.get("returnTo", String.class),
                c.get("rol", String.class)
            );
        } catch (JwtException | IllegalArgumentException e) {
            throw new GubUyAuthException(
                CodigoError.GUBUY_ESTADO_INVALIDO,
                "El estado del flujo de autenticacion es invalido o expiro",
                e
            );
        }
    }
}
