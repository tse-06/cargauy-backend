package uy.carga.tse.logica.autenticacion;

import java.util.UUID;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.comun.excepciones.NoAutenticadoException;

/** El token es valido pero el usuario ya no tiene el rol que lleva. */
public class RolRevocadoException extends NoAutenticadoException {

    public RolRevocadoException(UUID usuarioId, Rol rol) {
        super(
            CodigoError.ROL_REVOCADO,
            "El usuario " + usuarioId + " ya no tiene el rol " + rol
        );
    }
}
