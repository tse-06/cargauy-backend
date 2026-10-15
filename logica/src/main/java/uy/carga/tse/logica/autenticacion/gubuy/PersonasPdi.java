package uy.carga.tse.logica.autenticacion.gubuy;

import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;

/**
 * Puerto hacia la PDI (Servicio Basico de Informacion de la DNIC, SOAP, R03).
 * Se consulta solo en la primera autenticacion de un ciudadano: de ahi salen
 * los datos que {@code Usuario} exige y la fecha de nacimiento para verificar
 * que es mayor de edad.
 * <p>
 * Si la PDI no responde, la implementacion debe lanzar
 * {@link ServicioNoDisponibleException} con
 * {@code PDI_NO_DISPONIBLE}: el usuario nuevo no puede registrarse
 * hasta poder verificar su edad, pero los ya registrados siguen entrando (AC10).
 * <p>
 * Para usar la implementacion real, declararla como
 * {@code @Alternative @Priority(...)}: reemplaza a {@link PersonasPdiNoConfigurada}.
 */
public interface PersonasPdi {
    record DatosPersona(
        String nombre,
        String apellido,
        LocalDate fechaNacimiento
    ) {
        public DatosPersona {
            Objects.requireNonNull(nombre, "nombre");
            Objects.requireNonNull(apellido, "apellido");
            Objects.requireNonNull(fechaNacimiento, "fechaNacimiento");
        }
    }

    /** Vacio si la PDI no conoce la cedula. */
    Optional<DatosPersona> obtener(String ci);
}
