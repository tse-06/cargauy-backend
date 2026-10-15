package uy.carga.tse.servicios.errores;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.Response.Status;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;
import java.util.List;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.error.ErrorDto;

/** Violaciones de Bean Validation (@Valid en los parametros de los resources). */
@Provider
public class ValidacionMapper
    implements ExceptionMapper<ConstraintViolationException>
{

    @Override
    public Response toResponse(ConstraintViolationException e) {
        List<String> detalles = e
            .getConstraintViolations()
            .stream()
            .map(
                (ConstraintViolation<?> v) ->
                    v.getPropertyPath() + ": " + v.getMessage()
            )
            .sorted()
            .toList();
        return Response.status(Status.BAD_REQUEST)
            .type(MediaType.APPLICATION_JSON)
            .entity(
                new ErrorDto(
                    Status.BAD_REQUEST.getStatusCode(),
                    CodigoError.DATOS_INVALIDOS,
                    "Datos invalidos",
                    detalles
                )
            )
            .build();
    }
}
