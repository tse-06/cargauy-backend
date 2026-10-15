package uy.carga.tse.logica.autenticacion;

import java.util.Collection;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

/**
 * El usuario tiene varios roles y el inicio de sesion no indico con cual
 * quiere trabajar (HTTP 400).
 */
public class RolRequeridoException extends ValidacionNegocioException {

    public RolRequeridoException(Collection<Rol> roles) {
        super(
            CodigoError.ROL_REQUERIDO,
            "El usuario tiene varios roles, indique cual usar: " + roles
        );
    }
}
