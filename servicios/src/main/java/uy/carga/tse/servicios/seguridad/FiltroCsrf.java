package uy.carga.tse.servicios.seguridad;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;

/**
 * Defensa CSRF de la sesion por cookie: una request que cambia datos y viene
 * autenticada solo por la cookie tiene que traer
 * {@code X-Requested-With: cargauy}. Un formulario o una imagen de otro sitio
 * no pueden agregar ese header, y un fetch cross-origin que lo agregue necesita
 * antes un preflight que {@link uy.carga.tse.servicios.CorsFilter} solo
 * aprueba para los fronts. Se complementa con SameSite=Lax en la cookie.
 * <p>
 * No aplica a los metodos de solo lectura ni a las requests con
 * {@code Authorization} (movil, scripts): el navegador no lo manda solo, asi
 * que no hay nada que falsificar. Si hay cookie de sesion y falta el header,
 * se rechaza con 403 aunque el endpoint sea publico (por ejemplo el logout).
 */
@Provider
public class FiltroCsrf implements ContainerRequestFilter {

    static final String ENCABEZADO = "X-Requested-With";
    static final String VALOR = "cargauy";

    private static final Set<String> METODOS_SEGUROS = Set.of(
        "GET",
        "HEAD",
        "OPTIONS"
    );

    @Override
    public void filter(ContainerRequestContext request) {
        boolean traeCookie = request
            .getCookies()
            .containsKey(CookieSesion.NOMBRE);
        boolean traeAuthorization =
            request.getHeaderString("Authorization") != null;
        if (
            debeRechazar(
                request.getMethod(),
                traeCookie,
                traeAuthorization,
                request.getHeaderString(ENCABEZADO)
            )
        ) {
            throw new AccesoDenegadoException(
                CodigoError.CSRF_INVALIDO,
                "Falta el encabezado " + ENCABEZADO + ": " + VALOR
            );
        }
    }

    static boolean debeRechazar(
        String metodo,
        boolean traeCookieSesion,
        boolean traeAuthorization,
        String encabezado
    ) {
        return (
            traeCookieSesion &&
            !traeAuthorization &&
            !METODOS_SEGUROS.contains(metodo) &&
            !VALOR.equals(encabezado)
        );
    }
}
