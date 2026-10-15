package uy.carga.tse.logica.autenticacion.gubuy;

import jakarta.enterprise.context.ApplicationScoped;
import java.util.Optional;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;

/**
 * Implementacion por defecto mientras no exista el cliente real de la PDI:
 * permite que el EAR despliegue (CDI exige una implementacion de
 * {@link PersonasPdi}) y hace que el alta de usuarios nuevos falle con 503.
 * Los usuarios ya registrados no la usan y entran con normalidad.
 */
@ApplicationScoped
public class PersonasPdiNoConfigurada implements PersonasPdi {

    @Override
    public Optional<DatosPersona> obtener(String ci) {
        throw new ServicioNoDisponibleException(
            CodigoError.PDI_NO_DISPONIBLE,
            "El cliente de la PDI no esta configurado"
        );
    }
}
