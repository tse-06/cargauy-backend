package uy.carga.tse.logica.comun.excepciones;

import uy.carga.tse.datatypes.error.CodigoError;

/**
 * La operacion choca con el estado actual de los datos: valor unico repetido,
 * entidad referenciada por otras, etc. (HTTP 409).
 */
public class ConflictoException extends ExcepcionNegocio {

    public ConflictoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
