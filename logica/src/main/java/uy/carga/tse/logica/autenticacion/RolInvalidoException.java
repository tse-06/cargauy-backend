package uy.carga.tse.logica.autenticacion;

import java.util.Arrays;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

/** El rol indicado en el inicio de sesion no existe (HTTP 400). */
public class RolInvalidoException extends ValidacionNegocioException {

    public RolInvalidoException(String valor) {
        super(
            CodigoError.ROL_INVALIDO,
            "Rol invalido: " +
                valor +
                ". Valores posibles: " +
                Arrays.toString(Rol.values())
        );
    }
}
