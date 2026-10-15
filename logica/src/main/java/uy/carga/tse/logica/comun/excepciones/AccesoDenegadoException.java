package uy.carga.tse.logica.comun.excepciones;

import uy.carga.tse.datatypes.error.CodigoError;

/** Rechazo por falta de permisos (HTTP 403). */
public class AccesoDenegadoException extends ExcepcionNegocio {

    public AccesoDenegadoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }
}
