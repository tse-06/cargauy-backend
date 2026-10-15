package uy.carga.tse.logica.autenticacion;

import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;

/** Se pidio iniciar sesion con un rol que el usuario no tiene. */
public class RolNoAsignadoException extends AccesoDenegadoException {

    public RolNoAsignadoException(Rol rol) {
        super(CodigoError.ROL_NO_ASIGNADO, "El usuario no tiene el rol " + rol);
    }
}
