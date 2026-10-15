package uy.carga.tse.logica.autenticacion.gubuy;

import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.NoAutenticadoException;

/**
 * gub.uy rechazo la autenticacion o el flujo es invalido (state, nonce,
 * id_token, codigo de autorizacion). El mensaje es para el log; el cliente
 * recibe solo el {@link CodigoError}.
 */
public class GubUyAuthException extends NoAutenticadoException {

    public GubUyAuthException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    public GubUyAuthException(
        CodigoError codigo,
        String mensaje,
        Throwable causa
    ) {
        super(codigo, mensaje, causa);
    }
}
