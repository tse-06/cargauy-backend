package uy.carga.tse.datatypes.error;

import java.util.List;

/**
 * Cuerpo de respuesta de todos los errores de la API.
 *
 * @param estado   codigo HTTP
 * @param codigo   motivo estable del error, para que el cliente elija el texto a mostrar
 * @param mensaje  descripcion legible del error (para desarrolladores, no para el usuario final)
 * @param detalles errores puntuales (ej. un campo invalido por elemento); puede ser vacio
 */
public record ErrorDto(
    int estado,
    CodigoError codigo,
    String mensaje,
    List<String> detalles
) {
    public ErrorDto(int estado, CodigoError codigo, String mensaje) {
        this(estado, codigo, mensaje, List.of());
    }
}
