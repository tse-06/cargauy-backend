package uy.carga.tse.servicios.errores;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.core.SecurityContext;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import uy.carga.tse.datatypes.error.CodigoError;

/**
 * RESTEasy lanza ForbiddenException cuando {@code @RolesAllowed} rechaza la
 * llamada, tanto si no hay usuario autenticado (401) como si el rol activo no
 * alcanza (403).
 */
@Provider
public class ForbiddenMapper implements ExceptionMapper<ForbiddenException> {

    @Context
    SecurityContext securityContext;

    @Override
    public Response toResponse(ForbiddenException e) {
        if (securityContext.getUserPrincipal() == null) {
            return Response.fromResponse(
                ExcepcionNegocioMapper.respuesta(
                    Status.UNAUTHORIZED,
                    CodigoError.NO_AUTENTICADO,
                    "No autenticado"
                )
            )
                .header("WWW-Authenticate", "Bearer")
                .build();
        }
        return ExcepcionNegocioMapper.respuesta(
            Status.FORBIDDEN,
            CodigoError.OPERACION_NO_PERMITIDA,
            "El rol de la sesion no permite esta operacion"
        );
    }
}
