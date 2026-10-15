package uy.carga.tse.servicios.errores;

import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.error.ErrorDto;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;
import uy.carga.tse.logica.comun.excepciones.ConflictoException;
import uy.carga.tse.logica.comun.excepciones.ExcepcionNegocio;
import uy.carga.tse.logica.comun.excepciones.NoAutenticadoException;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

/**
 * Traduce las excepciones de negocio a {@link ErrorDto}: la categoria de la
 * excepcion define el estado HTTP y su {@link CodigoError} viaja tal cual.
 */
@Provider
public class ExcepcionNegocioMapper
    implements ExceptionMapper<ExcepcionNegocio>
{

    private static final Logger log = LoggerFactory.getLogger(
        ExcepcionNegocioMapper.class
    );

    @Override
    public Response toResponse(ExcepcionNegocio e) {
        Status estado = estadoDe(e);
        String mensaje = e.getMessage();
        if (e instanceof ServicioNoDisponibleException) {
            // el detalle (URL, status del servicio externo) queda solo en el log
            log.warn("Servicio externo no disponible: {}", e.getMessage());
            mensaje = "El servicio no esta disponible en este momento";
        } else if (estado == Status.UNAUTHORIZED) {
            log.warn(
                "Autenticacion rechazada: {} {}",
                e.codigo(),
                e.getMessage()
            );
        }
        return respuesta(estado, e.codigo(), mensaje);
    }

    static Status estadoDe(ExcepcionNegocio e) {
        return switch (e) {
            case ValidacionNegocioException v -> Status.BAD_REQUEST;
            case NoAutenticadoException n -> Status.UNAUTHORIZED;
            case AccesoDenegadoException a -> Status.FORBIDDEN;
            case RecursoNoEncontradoException r -> Status.NOT_FOUND;
            case ConflictoException c -> Status.CONFLICT;
            case ServicioNoDisponibleException s -> Status.SERVICE_UNAVAILABLE;
            default -> Status.INTERNAL_SERVER_ERROR;
        };
    }

    static Response respuesta(
        Status estado,
        CodigoError codigo,
        String mensaje
    ) {
        return respuesta(estado.getStatusCode(), codigo, mensaje);
    }

    static Response respuesta(int estado, CodigoError codigo, String mensaje) {
        return Response.status(estado)
            .type(MediaType.APPLICATION_JSON)
            .entity(new ErrorDto(estado, codigo, mensaje))
            .build();
    }
}
