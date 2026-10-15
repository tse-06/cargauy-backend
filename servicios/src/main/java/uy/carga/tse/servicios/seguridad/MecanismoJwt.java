package uy.carga.tse.servicios.seguridad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.AuthenticationStatus;
import jakarta.security.enterprise.authentication.mechanism.http.HttpAuthenticationMechanism;
import jakarta.security.enterprise.authentication.mechanism.http.HttpMessageContext;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStoreHandler;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Autentica cada request con un JWT, que puede venir de dos lugares: el header
 * {@code Authorization: Bearer} (app movil y scripts) o la cookie httpOnly
 * {@link CookieSesion#NOMBRE} (fronts web). Si hay header se usa solo ese y la
 * cookie se ignora. No hay sesion HTTP: el mecanismo corre en todas las
 * requests. Sin token o con uno invalido la request sigue como anonima: los
 * recursos con {@code @RolesAllowed} la rechazan con 401 (ver ForbiddenMapper).
 */
@ApplicationScoped
public class MecanismoJwt implements HttpAuthenticationMechanism {

    private static final String ESQUEMA = "Bearer ";

    /** Atributo de la request con el token ya validado (lo lee GET /auth/me). */
    public static final String ATRIBUTO_TOKEN = "uy.carga.tse.token";

    @Inject
    IdentityStoreHandler identityStoreHandler;

    @Override
    public AuthenticationStatus validateRequest(
        HttpServletRequest request,
        HttpServletResponse response,
        HttpMessageContext contexto
    ) {
        String token = extraerToken(
            request.getHeader("Authorization"),
            valorCookie(request)
        );
        if (token == null) {
            return contexto.doNothing();
        }
        CredentialValidationResult resultado = identityStoreHandler.validate(
            new CredencialJwt(token)
        );
        if (resultado.getStatus() != CredentialValidationResult.Status.VALID) {
            return contexto.doNothing();
        }
        request.setAttribute(ATRIBUTO_TOKEN, token);
        return contexto.notifyContainerAboutLogin(resultado);
    }

    /**
     * Con header Authorization manda ese (si no es Bearer la request es
     * anonima); sin header, la cookie de sesion. Null si no hay token.
     */
    static String extraerToken(String authorization, String cookie) {
        if (authorization != null) {
            return authorization.regionMatches(
                    true,
                    0,
                    ESQUEMA,
                    0,
                    ESQUEMA.length()
                )
                ? authorization.substring(ESQUEMA.length()).trim()
                : null;
        }
        return cookie == null || cookie.isBlank() ? null : cookie;
    }

    private static String valorCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (CookieSesion.NOMBRE.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }
}
