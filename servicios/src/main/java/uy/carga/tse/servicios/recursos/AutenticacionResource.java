package uy.carga.tse.servicios.recursos;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import uy.carga.tse.datatypes.usuario.DetalleSesionDto;
import uy.carga.tse.datatypes.usuario.IniciarSesionDto;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.autenticacion.AutenticacionServicioLocal;
import uy.carga.tse.logica.autenticacion.Identidad;
import uy.carga.tse.logica.autenticacion.ServicioJwt;
import uy.carga.tse.servicios.seguridad.CookieSesion;
import uy.carga.tse.servicios.seguridad.MecanismoJwt;

/**
 * Sesion de los fronts web: el token no viaja en el cuerpo sino en la cookie
 * httpOnly {@link CookieSesion}, que el front no puede leer. Por eso existe
 * GET /auth/me (el front pregunta quien es) y POST /auth/logout (solo el
 * backend puede borrar la cookie).
 */
@Path("/auth")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AutenticacionResource {

    @Inject
    AutenticacionServicioLocal autenticacionServicio;

    @Inject
    ServicioJwt servicioJwt;

    /**
     * Login interno de administradores y autoridades (R09): deja la sesion en
     * la cookie y responde los datos para mostrar, sin el token.
     */
    @POST
    @Path("/login")
    public Response iniciarSesion(@Valid IniciarSesionDto dto) {
        DetalleSesionDto sesion = autenticacionServicio.iniciarSesion(dto);
        return Response.ok(sinToken(sesion))
            .cookie(CookieSesion.emitir(sesion.token(), sesion.expiraEn()))
            .header("Cache-Control", "no-store")
            .build();
    }

    /** Datos de la sesion de la cookie (o del Bearer); 401 si no hay ninguna. */
    @GET
    @Path("/me")
    @RolesAllowed(
        {
            Roles.ADMINISTRADOR,
            Roles.AUTORIDAD,
            Roles.FUNCIONARIO,
            Roles.ENCARGADO_EMPRESA,
            Roles.CHOFER,
        }
    )
    public Response sesionActual(@Context HttpServletRequest request) {
        String token = (String) request.getAttribute(
            MecanismoJwt.ATRIBUTO_TOKEN
        );
        Identidad identidad = servicioJwt.verificar(token);
        DetalleSesionDto sesion = autenticacionServicio.detalleSesion(
            identidad.usuarioId(),
            identidad.rol(),
            servicioJwt.vencimiento(token)
        );
        return Response.ok(sinToken(sesion))
            .header("Cache-Control", "no-store")
            .build();
    }

    /**
     * Borra la cookie. Es publico y siempre responde 204, haya o no sesion. El
     * token en si no se revoca (es un JWT sin estado): deja de usarse en el
     * navegador y vence solo.
     */
    @POST
    @Path("/logout")
    public Response cerrarSesion() {
        return Response.noContent()
            .cookie(CookieSesion.borrar())
            .header("Cache-Control", "no-store")
            .build();
    }

    /** Solo los datos para mostrar: ni el token ni el tipo de esquema. */
    private static DetalleSesionDto sinToken(DetalleSesionDto sesion) {
        return new DetalleSesionDto(
            null,
            null,
            sesion.expiraEn(),
            sesion.rol(),
            sesion.ci(),
            sesion.nombre(),
            sesion.apellido()
        );
    }
}
