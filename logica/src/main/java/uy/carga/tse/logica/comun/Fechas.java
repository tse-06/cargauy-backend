package uy.carga.tse.logica.comun;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

/** Fechas recibidas como texto (ej. campos de un multipart). */
public final class Fechas {

    private Fechas() {}

    /** Fecha en formato ISO (yyyy-MM-dd). */
    public static LocalDate parsear(String valor) {
        try {
            return LocalDate.parse(valor.trim());
        } catch (DateTimeParseException e) {
            throw new ValidacionNegocioException(
                CodigoError.FECHA_INVALIDA,
                "Fecha invalida: " + valor + ". Formato esperado: yyyy-MM-dd"
            );
        }
    }
}
