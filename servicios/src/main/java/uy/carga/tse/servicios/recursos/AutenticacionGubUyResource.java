package uy.carga.tse.servicios.recursos;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.autenticacion.RolesUsuario;
import uy.carga.tse.logica.autenticacion.Sesion;
import uy.carga.tse.logica.autenticacion.gubuy.GubUyAuthException;
import uy.carga.tse.logica.autenticacion.gubuy.GubUyConfig;
import uy.carga.tse.logica.autenticacion.gubuy.GubUyLoginService;
import uy.carga.tse.logica.autenticacion.gubuy.SesionEmisorJwt;
import uy.carga.tse.logica.comun.excepciones.ExcepcionNegocio;
import uy.carga.tse.servicios.seguridad.CookieSesion;

/**
 * Login con gub.uy (ID Uruguay, OpenID Connect). Endpoints publicos: no llevan
 * {@code @RolesAllowed}. Con el context-root /api, la redirect_uri que se
 * registra en AGESIC es {@code https://<host>/api/auth/gubuy/callback}.
 * <ul>
 *   <li>GET /auth/gubuy/login?returnTo=...&amp;rol=CHOFER redirige a ID Uruguay;
 *       "rol" es el rol con el que se quiere entrar (CHOFER desde el movil,
 *       ENCARGADO_EMPRESA o FUNCIONARIO desde la web)</li>
 *   <li>GET /auth/gubuy/callback vuelve de ID Uruguay</li>
 * </ul>
 * Al terminar redirige a returnTo, o con {@code #error=<CodigoError>} si no se
 * pudo iniciar sesion. Si returnTo es una pagina web (http/https) la sesion
 * queda en la cookie httpOnly {@link CookieSesion} y el token no pasa por la URL
 * ni por el JavaScript del front, que consulta GET /auth/me. Si es el deep link
 * de la app movil, el JWT de la plataforma va en el fragmento
 * ({@code #token=...}), que no se envia a servidores ni queda en logs de acceso.
 */
@Path("/auth/gubuy")
@RequestScoped
public class AutenticacionGubUyResource {

    private static final Logger log = LoggerFactory.getLogger(
        AutenticacionGubUyResource.class
    );

    static final String COOKIE_FLUJO = "gubuy_flow";
    private static final String RUTA_COOKIE = "/api/auth/gubuy";

    @Inject
    GubUyLoginService login;

    @Inject
    SesionEmisorJwt sesiones;

    @Inject
    GubUyConfig config;

    @GET
    @Path("/login")
    public Response iniciar(
        @QueryParam("returnTo") String returnTo,
        @QueryParam("rol") String rol
    ) {
        RolesUsuario.parsear(rol); // falla antes de mandar al usuario a gub.uy
        GubUyLoginService.LoginStart inicio = login.start(returnTo, rol);
        return Response.seeOther(inicio.authorizationUri())
            .cookie(
                cookieFlujo(
                    inicio.flowToken(),
                    (int) inicio.flowTtl().toSeconds()
                )
            )
            .header("Cache-Control", "no-store")
            .build();
    }

    @GET
    @Path("/callback")
    public Response callback(
        @QueryParam("code") String code,
        @QueryParam("state") String state,
        @QueryParam("error") String error,
        @CookieParam(COOKIE_FLUJO) String flujo
    ) {
        NewCookie borrar = cookieFlujo("", 0); // la cookie de flujo es de un solo uso

        if (error != null) {
            // gub.uy informo un error (p. ej. el usuario cancelo)
            CodigoError codigo = "access_denied".equals(error)
                ? CodigoError.GUBUY_CANCELADO
                : CodigoError.GUBUY_ERROR;
            return volverConError(
                flujo,
                borrar,
                new GubUyAuthException(codigo, "gub.uy informo un error")
            );
        }
        try {
            GubUyLoginService.LoginResult resultado = login.complete(
                code,
                state,
                flujo
            );
            Sesion sesion = sesiones.emitirSesion(
                resultado.identity(),
                RolesUsuario.parsear(resultado.rol())
            );
            if (esDestinoWeb(resultado.returnTo())) {
                return redirigir(
                    resultado.returnTo(),
                    borrar,
                    CookieSesion.emitir(sesion.token(), sesion.expiraEn())
                );
            }
            return redirigir(
                resultado.returnTo() + "#token=" + codificar(sesion.token()),
                borrar
            );
        } catch (ExcepcionNegocio e) {
            return volverConError(flujo, borrar, e);
        }
    }

    /**
     * Vuelve al frontend con el motivo, para que muestre un mensaje en lugar de
     * un JSON en pantalla. Si la cookie de flujo no es valida no se sabe a donde
     * volver y se propaga la excepcion (la traducen los mappers).
     */
    private Response volverConError(
        String flujo,
        NewCookie borrar,
        ExcepcionNegocio causa
    ) {
        // sin esto un fallo del flujo no deja rastro en el servidor (el usuario solo ve #error=...)
        log.warn(
            "Login con gub.uy no completado: codigo={} detalle={}",
            causa.codigo(),
            causa.getMessage()
        );
        return login
            .returnToFrom(flujo)
            .map(returnTo ->
                redirigir(returnTo + "#error=" + causa.codigo().name(), borrar)
            )
            .orElseThrow(() -> causa);
    }

    private static Response redirigir(String destino, NewCookie... cookies) {
        return Response.seeOther(URI.create(destino))
            .cookie(cookies)
            .header("Cache-Control", "no-store")
            .build();
    }

    /** Las paginas web reciben la sesion en cookie; el deep link movil, el token. */
    static boolean esDestinoWeb(String destino) {
        String esquema = URI.create(destino).getScheme();
        return (
            "http".equalsIgnoreCase(esquema) ||
            "https".equalsIgnoreCase(esquema)
        );
    }

    private NewCookie cookieFlujo(String valor, int maxAgeSegundos) {
        return new NewCookie.Builder(COOKIE_FLUJO)
            .value(valor)
            .path(RUTA_COOKIE)
            .maxAge(maxAgeSegundos)
            .httpOnly(true)
            .secure(config.cookieSecure())
            // el callback es una navegacion top-level GET desde gub.uy
            .sameSite(NewCookie.SameSite.LAX)
            .build();
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }
}
