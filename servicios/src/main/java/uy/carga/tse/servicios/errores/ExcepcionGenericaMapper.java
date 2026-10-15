package uy.carga.tse.servicios.errores;

import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.datatypes.error.CodigoError;

@Provider
public class ExcepcionGenericaMapper implements ExceptionMapper<Exception> {

    private static final Logger log = LoggerFactory.getLogger(
        ExcepcionGenericaMapper.class
    );

    @Override
    public Response toResponse(Exception e) {
        if (
            e instanceof WebApplicationException web &&
            web.getResponse().getStatus() < 500
        ) {
            return ExcepcionNegocioMapper.respuesta(
                web.getResponse().getStatus(),
                CodigoError.SOLICITUD_INVALIDA,
                web.getMessage()
            );
        }
        log.error("Error no controlado", e);
        return ExcepcionNegocioMapper.respuesta(
            Status.INTERNAL_SERVER_ERROR,
            CodigoError.ERROR_INTERNO,
            "Error interno del servidor"
        );
    }
}
