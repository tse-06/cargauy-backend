package uy.carga.tse.logica.autenticacion;

import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.NoAutenticadoException;

/**
 * El token no se puede aceptar: firma o emisor incorrectos, vencido, o con un
 * subject o rol que no se pueden interpretar.
 */
public class TokenInvalidoException extends NoAutenticadoException {

    public TokenInvalidoException(Throwable causa) {
        super(CodigoError.TOKEN_INVALIDO, "Token invalido", causa);
    }

    public TokenInvalidoException(String mensaje) {
        super(CodigoError.TOKEN_INVALIDO, mensaje);
    }
}
