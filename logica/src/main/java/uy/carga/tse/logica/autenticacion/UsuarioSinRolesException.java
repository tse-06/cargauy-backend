package uy.carga.tse.logica.autenticacion;

import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;

/** Las credenciales son correctas pero el usuario no tiene ningun rol. */
public class UsuarioSinRolesException extends AccesoDenegadoException {

    public UsuarioSinRolesException() {
        super(
            CodigoError.USUARIO_SIN_ROLES,
            "El usuario no tiene ningun rol asignado"
        );
    }
}
