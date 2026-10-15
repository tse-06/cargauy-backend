package uy.carga.tse.logica.autenticacion.gubuy;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.logica.comun.Cedula;

/**
 * Sustituto de la PDI (Servicio Basico de Informacion de la DNIC), que no es publica.
 * Los datos salen de la variable de entorno {@code PDI_PERSONAS}, con el formato
 * {@code cedula;nombre;apellido;aaaa-mm-dd} y varias personas separadas por {@code |}.
 * La cedula se compara solo por sus digitos, asi que "1.234.567-8" y "12345678" coinciden.
 * Si la variable esta vacia la PDI no conoce a nadie.
 */
@ApplicationScoped
@Alternative
@Priority(100)
public class PersonasPdiSimulada implements PersonasPdi {

    private static final Logger log = LoggerFactory.getLogger(
        PersonasPdiSimulada.class
    );

    private final Map<String, DatosPersona> personas;

    public PersonasPdiSimulada() {
        this(System.getenv("PDI_PERSONAS"));
    }

    PersonasPdiSimulada(String definicion) {
        this.personas = parsear(definicion);
        log.info("PDI simulada: {} persona(s) cargada(s)", personas.size());
    }

    @Override
    public Optional<DatosPersona> obtener(String ci) {
        String clave = normalizar(ci);
        Optional<DatosPersona> persona = Optional.ofNullable(
            personas.get(clave)
        );
        if (persona.isEmpty()) {
            // solo la cantidad de digitos, nunca la cedula: sirve para detectar diferencias de formato
            log.warn(
                "PDI simulada: no se conoce una cedula de {} digitos",
                clave.length()
            );
        }
        return persona;
    }

    static Map<String, DatosPersona> parsear(String definicion) {
        if (definicion == null || definicion.isBlank()) {
            return Map.of();
        }
        Map<String, DatosPersona> mapa = new HashMap<>();
        for (String entrada : definicion.split("\\|")) {
            String[] campos = entrada.split(";");
            if (campos.length != 4) {
                log.warn("PDI simulada: se ignora una entrada mal formada");
                continue;
            }
            try {
                mapa.put(
                    normalizar(campos[0]),
                    new DatosPersona(
                        campos[1].trim(),
                        campos[2].trim(),
                        LocalDate.parse(campos[3].trim())
                    )
                );
            } catch (RuntimeException e) {
                log.warn(
                    "PDI simulada: se ignora una entrada invalida ({})",
                    e.getMessage()
                );
            }
        }
        return Map.copyOf(mapa);
    }

    static String normalizar(String ci) {
        return ci == null ? "" : Cedula.normalizar(ci);
    }
}
