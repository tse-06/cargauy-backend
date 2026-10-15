package uy.carga.tse.logica.comun.excepciones;

import uy.carga.tse.datatypes.error.CodigoError;

/** Fallo de autenticacion (HTTP 401). */
public class NoAutenticadoException extends ExcepcionNegocio {

    public NoAutenticadoException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    public NoAutenticadoException(
        CodigoError codigo,
        String mensaje,
        Throwable causa
    ) {
        super(codigo, mensaje, causa);
    }
}
