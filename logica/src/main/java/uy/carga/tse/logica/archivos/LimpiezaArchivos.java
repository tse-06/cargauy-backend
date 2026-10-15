package uy.carga.tse.logica.archivos;

import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.ejb.TransactionAttribute;
import jakarta.ejb.TransactionAttributeType;
import jakarta.inject.Inject;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Borra todos los dias los archivos que quedaron sin adjuntar a ninguna
 * entidad (ej. por {@link ArchivoServicioLocal#quitar}). Solo toma los creados hace
 * mas de {@link #ANTIGUEDAD_MINIMA}, para no borrar uno que se este por
 * adjuntar.
 */
@Singleton
public class LimpiezaArchivos {

    static final Duration ANTIGUEDAD_MINIMA = Duration.ofDays(1);

    private static final Logger log = LoggerFactory.getLogger(
        LimpiezaArchivos.class
    );

    @Inject
    ArchivoRepositorio archivoRepositorio;

    @Inject
    ArchivoServicioLocal servicioArchivos;

    // No persistente: si el servidor estaba caido a esa hora, se limpia en la
    // corrida siguiente. Sin transaccion propia, para que cada archivo se borre
    // en la suya y un error no deshaga los demas.
    @Schedule(hour = "3", timezone = "America/Montevideo", persistent = false)
    @TransactionAttribute(TransactionAttributeType.NOT_SUPPORTED)
    void limpiar() {
        Instant limite = Instant.now().minus(ANTIGUEDAD_MINIMA);
        int eliminados = 0;
        for (UUID archivoId : archivoRepositorio.huerfanos(limite)) {
            try {
                if (servicioArchivos.eliminarHuerfano(archivoId)) {
                    eliminados++;
                }
            } catch (RuntimeException e) {
                log.warn(
                    "No se pudo eliminar el archivo huerfano {}",
                    archivoId,
                    e
                );
            }
        }
        if (eliminados > 0) {
            log.info("Se eliminaron {} archivos sin adjuntar", eliminados);
        }
    }
}
