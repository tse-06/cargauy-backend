package uy.carga.tse.logica.autenticacion;

import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.NoAutenticadoException;

public class CredencialesInvalidasException extends NoAutenticadoException {

    public CredencialesInvalidasException() {
        super(CodigoError.CREDENCIALES_INVALIDAS, "Credenciales invalidas");
    }
}
