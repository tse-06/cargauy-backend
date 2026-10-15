package uy.carga.tse.logica.comun.excepciones;

import uy.carga.tse.datatypes.error.CodigoError;

/**
 * Un dato de entrada no cumple una regla de negocio (valor de enum invalido,
 * etc.) (HTTP 400).
 */
public class ValidacionNegocioException extends ExcepcionNegocio {

    public ValidacionNegocioException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
