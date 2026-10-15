package uy.carga.tse.servicios;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.container.PreMatching;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * CORS para los fronts (FRONTOFFICE_URL y BACKOFFICE_URL, ver .env.example).
 * Contesta el preflight (OPTIONS) sin
 * pasar por la seguridad y agrega los headers a toda respuesta, errores
 * incluidos, cuando el Origin esta en la lista permitida.
 * <p>
 * Los fronts llaman con credenciales porque la sesion web viaja en una cookie
 * httpOnly (ver CookieSesion): el navegador solo la acepta y la manda si la
 * respuesta trae Access-Control-Allow-Credentials y un origen exacto, no "*".
 */
@Provider
@PreMatching
public class CorsFilter
    implements ContainerRequestFilter, ContainerResponseFilter
{

    private static final Set<String> ORIGENES_PERMITIDOS = Stream.of(
        System.getenv("FRONTOFFICE_URL"),
        System.getenv("BACKOFFICE_URL")
    )
        .filter(url -> url != null && !url.isBlank())
        .map(url -> url.strip().replaceAll("/+$", ""))
        .collect(Collectors.toUnmodifiableSet());

    @Override
    public void filter(ContainerRequestContext request) {
        if ("OPTIONS".equals(request.getMethod())) {
            request.abortWith(Response.noContent().build());
        }
    }

    @Override
    public void filter(
        ContainerRequestContext request,
        ContainerResponseContext response
    ) {
        String origen = request.getHeaderString("Origin");
        if (origen == null || !ORIGENES_PERMITIDOS.contains(origen)) {
            return;
        }
        var headers = response.getHeaders();
        headers.putSingle("Access-Control-Allow-Origin", origen);
        headers.putSingle("Access-Control-Allow-Credentials", "true");
        headers.putSingle("Vary", "Origin");
        headers.putSingle(
            "Access-Control-Allow-Methods",
            "GET, POST, PUT, PATCH, DELETE, OPTIONS"
        );
        headers.putSingle(
            "Access-Control-Allow-Headers",
            "Content-Type, Authorization, X-Requested-With"
        );
        headers.putSingle("Access-Control-Max-Age", "3600");
    }
}
