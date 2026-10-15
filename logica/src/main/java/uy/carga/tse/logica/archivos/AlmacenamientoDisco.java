package uy.carga.tse.logica.archivos;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.SecureRandom;
import java.util.function.Function;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;

/**
 * Guarda el contenido de los archivos en disco, bajo el directorio de
 * {@code ARCHIVOS_DIRECTORIO}. Cada archivo se nombra por su clave y se
 * reparte en subdirectorios segun sus primeros caracteres
 * ({@code raiz/ab/cd/abcd...}).
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class AlmacenamientoDisco {

    static final String VARIABLE_DIRECTORIO = "ARCHIVOS_DIRECTORIO";
    static final int LARGO_CLAVE = 28;

    private static final String ALFABETO =
        "0123456789abcdefghijklmnopqrstuvwxyz";
    private static final Pattern FORMATO_CLAVE = Pattern.compile(
        "[0-9a-z]{" + LARGO_CLAVE + "}"
    );
    private static final Logger log = LoggerFactory.getLogger(
        AlmacenamientoDisco.class
    );

    private final SecureRandom aleatorio = new SecureRandom();
    private Path raiz;

    @PostConstruct
    void iniciar() {
        iniciar(System::getenv);
    }

    void iniciar(Function<String, String> entorno) {
        String directorio = entorno.apply(VARIABLE_DIRECTORIO);
        if (directorio == null || directorio.isBlank()) {
            throw new IllegalStateException(
                VARIABLE_DIRECTORIO + " no esta definida"
            );
        }
        raiz = Path.of(directorio.trim()).toAbsolutePath().normalize();
        try {
            Files.createDirectories(raiz);
        } catch (IOException e) {
            throw new IllegalStateException(
                "No se pudo crear " + VARIABLE_DIRECTORIO + " (" + raiz + ")",
                e
            );
        }
        if (!Files.isWritable(raiz)) {
            throw new IllegalStateException(
                VARIABLE_DIRECTORIO +
                    " (" +
                    raiz +
                    ") no tiene permiso de escritura"
            );
        }
    }

    /** Clave aleatoria de 28 caracteres en base36. */
    public String generarClave() {
        StringBuilder clave = new StringBuilder(LARGO_CLAVE);
        for (int i = 0; i < LARGO_CLAVE; i++) {
            clave.append(ALFABETO.charAt(aleatorio.nextInt(ALFABETO.length())));
        }
        return clave.toString();
    }

    /** Ruta del archivo en disco: {@code raiz/<clave[0..2]>/<clave[2..4]>/<clave>}. */
    public Path ruta(String clave) {
        // La clave la genera el sistema; validarla evita escapar de la raiz
        if (clave == null || !FORMATO_CLAVE.matcher(clave).matches()) {
            throw new IllegalArgumentException(
                "Clave de archivo invalida: " + clave
            );
        }
        return raiz
            .resolve(clave.substring(0, 2))
            .resolve(clave.substring(2, 4))
            .resolve(clave);
    }

    /**
     * Copia el contenido al archivo de la clave y devuelve la cantidad de bytes
     * escritos. Se escribe a un temporal y se mueve, para que nunca quede un
     * archivo a medio escribir con el nombre definitivo.
     */
    public long guardar(String clave, InputStream contenido) {
        Path destino = ruta(clave);
        Path temporal = destino.resolveSibling(clave + ".tmp");
        try {
            Files.createDirectories(destino.getParent());
            if (Files.exists(destino)) {
                throw new IllegalStateException(
                    "Ya existe un archivo con la clave " + clave
                );
            }
            long bytes = Files.copy(contenido, temporal);
            Files.move(temporal, destino, StandardCopyOption.ATOMIC_MOVE);
            return bytes;
        } catch (IOException e) {
            borrar(temporal);
            throw new UncheckedIOException(
                "No se pudo guardar el archivo " + clave,
                e
            );
        }
    }

    public InputStream abrir(String clave) {
        Path archivo = ruta(clave);
        try {
            return Files.newInputStream(archivo);
        } catch (NoSuchFileException e) {
            throw new RecursoNoEncontradoException(
                CodigoError.ARCHIVO_NO_ENCONTRADO,
                "No se encontro el contenido del archivo " + clave
            );
        } catch (IOException e) {
            throw new UncheckedIOException(
                "No se pudo leer el archivo " + clave,
                e
            );
        }
    }

    public void eliminar(String clave) {
        borrar(ruta(clave));
    }

    /**
     * Borra un archivo sin lanzar excepciones (solo registra el error). Es
     * estatico para poder usarlo desde los callbacks de fin de transaccion sin
     * invocar al EJB.
     */
    static void borrar(Path archivo) {
        try {
            Files.deleteIfExists(archivo);
        } catch (IOException e) {
            log.warn("No se pudo borrar el archivo {}", archivo, e);
        }
    }
}
