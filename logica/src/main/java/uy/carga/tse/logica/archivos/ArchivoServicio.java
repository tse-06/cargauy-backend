package uy.carga.tse.logica.archivos;

import jakarta.annotation.Resource;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;
import org.apache.tika.Tika;
import org.hibernate.Hibernate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.ArchivoAdjunto;
import uy.carga.tse.entidades.EntidadBase;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

/**
 * Los cambios en disco se coordinan con la transaccion: un archivo guardado se
 * borra si la transaccion hace rollback, y uno eliminado se borra del disco
 * solo cuando la transaccion se confirma.
 */
@Stateless
public class ArchivoServicio implements ArchivoServicioLocal {

    static final String TIPO_MIME_POR_DEFECTO = "application/octet-stream";
    static final String NOMBRE_POR_DEFECTO = "archivo";
    static final int LARGO_MAXIMO_NOMBRE = 255;
    static final String RUTA_DESCARGA = "/archivos/";

    // Tipos que el navegador puede mostrar sin riesgo. El resto se descarga,
    // asi un HTML o SVG subido no ejecuta scripts en el origen de la API
    static final Set<String> TIPOS_MOSTRABLES_EN_NAVEGADOR = Set.of(
        "application/pdf",
        "image/png",
        "image/jpeg",
        "image/gif",
        "image/webp"
    );

    // Tika es thread-safe para detect()
    private static final Tika TIKA = new Tika();
    private static final Logger log = LoggerFactory.getLogger(
        ArchivoServicio.class
    );

    @Inject
    ArchivoRepositorio archivoRepositorio;

    @Inject
    ArchivoAdjuntoRepositorio adjuntoRepositorio;

    @Inject
    AlmacenamientoDisco almacenamiento;

    @Inject
    FirmaArchivos firma;

    @Resource
    TransactionSynchronizationRegistry transacciones;

    @Override
    public Archivo adjuntar(
        EntidadBase entidad,
        String nombre,
        ArchivoSubido subido
    ) {
        String tipoEntidad = tipoDe(entidad);
        validarRelacion(nombre);
        if (subido == null || subido.contenido() == null) {
            throw new ValidacionNegocioException(
                CodigoError.ARCHIVO_SIN_CONTENIDO,
                "Falta el contenido del archivo"
            );
        }

        String clave = almacenamiento.generarClave();
        Path ruta = almacenamiento.ruta(clave);
        alTerminarTransaccion(confirmada -> {
            if (!confirmada) {
                AlmacenamientoDisco.borrar(ruta);
            }
        });
        long tamano = almacenamiento.guardar(clave, subido.contenido());
        String nombreArchivo = nombreArchivo(subido.nombre());

        Archivo archivo = new Archivo();
        archivo.setClave(clave);
        archivo.setNombre(nombreArchivo);
        archivo.setTipoMime(tipoMime(ruta, nombreArchivo));
        archivo.setTamano(tamano);
        archivoRepositorio.insert(archivo);

        ArchivoAdjunto adjunto = new ArchivoAdjunto();
        adjunto.setTipoEntidad(tipoEntidad);
        adjunto.setEntidadId(entidad.getId());
        adjunto.setNombre(nombre);
        adjunto.setArchivo(archivo);
        adjuntoRepositorio.insert(adjunto);

        return archivo;
    }

    @Override
    public Archivo reemplazar(
        EntidadBase entidad,
        String nombre,
        ArchivoSubido subido
    ) {
        validarRelacion(nombre);
        eliminarAdjuntos(
            adjuntoRepositorio.deRelacion(
                tipoDe(entidad),
                entidad.getId(),
                nombre
            )
        );
        return adjuntar(entidad, nombre, subido);
    }

    @Override
    public List<Archivo> listar(EntidadBase entidad, String nombre) {
        validarRelacion(nombre);
        return adjuntoRepositorio.archivos(
            tipoDe(entidad),
            entidad.getId(),
            nombre
        );
    }

    @Override
    public Optional<Archivo> obtener(EntidadBase entidad, String nombre) {
        return listar(entidad, nombre).stream().findFirst();
    }

    @Override
    public Adjuntos adjuntos(String nombre, EntidadBase... entidades) {
        validarRelacion(nombre);
        List<UUID> ids = Arrays.stream(entidades)
            .filter(Objects::nonNull)
            .map(EntidadBase::getId)
            .filter(Objects::nonNull)
            .toList();
        if (ids.isEmpty()) {
            return Adjuntos.vacio();
        }
        return Adjuntos.de(
            adjuntoRepositorio.deEntidades(ids, nombre),
            this::url
        );
    }

    @Override
    public InputStream abrir(Archivo archivo) {
        return almacenamiento.abrir(archivo.getClave());
    }

    @Override
    public String url(Archivo archivo) {
        return firma.urlBase() + RUTA_DESCARGA + firma.firmar(archivo.getId());
    }

    @Override
    public ArchivoDescarga abrirFirmado(String idFirmado) {
        UUID archivoId = firma.verificar(idFirmado);
        Archivo archivo = archivoRepositorio
            .findById(archivoId)
            .orElseThrow(() ->
                new RecursoNoEncontradoException(
                    CodigoError.ARCHIVO_NO_ENCONTRADO,
                    "Archivo no encontrado: " + archivoId
                )
            );
        return new ArchivoDescarga(
            archivo.getNombre(),
            archivo.getTipoMime(),
            archivo.getTamano(),
            seMuestraEnNavegador(archivo.getTipoMime()),
            almacenamiento.abrir(archivo.getClave())
        );
    }

    @Override
    public void quitar(EntidadBase entidad, String nombre, UUID archivoId) {
        ArchivoAdjunto adjunto = buscarVinculo(entidad, nombre, archivoId);
        adjuntoRepositorio.delete(adjunto);
    }

    @Override
    public void eliminar(EntidadBase entidad, String nombre, UUID archivoId) {
        eliminarAdjuntos(List.of(buscarVinculo(entidad, nombre, archivoId)));
    }

    @Override
    public void eliminarTodos(EntidadBase entidad) {
        eliminarAdjuntos(
            adjuntoRepositorio.deEntidad(tipoDe(entidad), entidad.getId())
        );
    }

    @Override
    public boolean eliminarHuerfano(UUID archivoId) {
        if (adjuntoRepositorio.referencias(archivoId) > 0) {
            return false;
        }
        Optional<Archivo> archivo = archivoRepositorio.findById(archivoId);
        if (archivo.isEmpty()) {
            return false;
        }
        borrar(archivo.get());
        return true;
    }

    /**
     * Borra los adjuntos y luego los archivos que quedaron sin ningun adjunto.
     */
    private void eliminarAdjuntos(List<ArchivoAdjunto> adjuntos) {
        if (adjuntos.isEmpty()) {
            return;
        }
        // getId() de un proxy lazy no lo inicializa
        Set<UUID> archivoIds = new LinkedHashSet<>();
        adjuntos.forEach(a -> archivoIds.add(a.getArchivo().getId()));
        adjuntoRepositorio.deleteAll(adjuntos);

        for (UUID archivoId : archivoIds) {
            if (adjuntoRepositorio.referencias(archivoId) > 0) {
                continue;
            }
            archivoRepositorio.findById(archivoId).ifPresent(this::borrar);
        }
    }

    /** Borra el Archivo de la base y, si la transaccion se confirma, del disco. */
    private void borrar(Archivo archivo) {
        Path ruta = almacenamiento.ruta(archivo.getClave());
        archivoRepositorio.delete(archivo);
        alTerminarTransaccion(confirmada -> {
            if (confirmada) {
                AlmacenamientoDisco.borrar(ruta);
            }
        });
    }

    private ArchivoAdjunto buscarVinculo(
        EntidadBase entidad,
        String nombre,
        UUID archivoId
    ) {
        validarRelacion(nombre);
        return adjuntoRepositorio
            .vinculo(tipoDe(entidad), entidad.getId(), nombre, archivoId)
            .orElseThrow(() ->
                new RecursoNoEncontradoException(
                    CodigoError.ARCHIVO_NO_ENCONTRADO,
                    "Archivo " +
                        archivoId +
                        " no adjunto a " +
                        entidad +
                        " en '" +
                        nombre +
                        "'"
                )
            );
    }

    /** Ejecuta la accion al terminar la transaccion actual, con su resultado. */
    private void alTerminarTransaccion(Consumer<Boolean> accion) {
        transacciones.registerInterposedSynchronization(
            new Synchronization() {
                @Override
                public void beforeCompletion() {}

                @Override
                public void afterCompletion(int estado) {
                    accion.accept(estado == Status.STATUS_COMMITTED);
                }
            }
        );
    }

    /** Nombre simple de la clase real de la entidad (sin el proxy de Hibernate). */
    static String tipoDe(EntidadBase entidad) {
        if (entidad == null || entidad.getId() == null) {
            throw new ValidacionNegocioException(
                CodigoError.ADJUNTO_INVALIDO,
                "La entidad tiene que estar persistida para adjuntarle archivos"
            );
        }
        return Hibernate.getClass(entidad).getSimpleName();
    }

    private static void validarRelacion(String nombre) {
        if (nombre == null || nombre.isBlank()) {
            throw new ValidacionNegocioException(
                CodigoError.ADJUNTO_INVALIDO,
                "Falta el nombre de la relacion del archivo"
            );
        }
    }

    /** Se queda solo con el nombre (sin directorios) que manda el cliente. */
    static String nombreArchivo(String original) {
        if (original == null) {
            return NOMBRE_POR_DEFECTO;
        }
        String nombre = original.replace('\\', '/');
        nombre = nombre.substring(nombre.lastIndexOf('/') + 1).strip();
        if (nombre.isEmpty()) {
            return NOMBRE_POR_DEFECTO;
        }
        // Si no entra en la columna se recorta el principio, para conservar la
        // extension
        return nombre.length() > LARGO_MAXIMO_NOMBRE
            ? nombre.substring(nombre.length() - LARGO_MAXIMO_NOMBRE)
            : nombre;
    }

    static boolean seMuestraEnNavegador(String tipoMime) {
        // Set.of no acepta contains(null)
        return (
            tipoMime != null && TIPOS_MOSTRABLES_EN_NAVEGADOR.contains(tipoMime)
        );
    }

    /**
     * Detecta el tipo MIME por los primeros bytes del archivo ya guardado. El
     * nombre solo desempata entre tipos con la misma firma (ej. un .docx es un
     * zip) o cuando el contenido no tiene firma (ej. .csv, .json).
     */
    static String tipoMime(Path ruta, String nombre) {
        try (InputStream contenido = Files.newInputStream(ruta)) {
            return TIKA.detect(contenido, nombre);
        } catch (IOException e) {
            log.warn("No se pudo detectar el tipo MIME de {}", ruta, e);
            return TIPO_MIME_POR_DEFECTO;
        }
    }
}
