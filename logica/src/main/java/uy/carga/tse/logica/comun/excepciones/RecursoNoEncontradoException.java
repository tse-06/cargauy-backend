package uy.carga.tse.logica.comun.excepciones;

import uy.carga.tse.datatypes.error.CodigoError;

/** No existe la entidad pedida (HTTP 404). */
public class RecursoNoEncontradoException extends ExcepcionNegocio {

    public RecursoNoEncontradoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
