package uy.carga.tse.logica.archivos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;

class AlmacenamientoDiscoTest {

    @TempDir
    Path raiz;

    private final Map<String, String> entorno = new HashMap<>();
    private AlmacenamientoDisco almacenamiento;

    @BeforeEach
    void preparar() {
        entorno.put(AlmacenamientoDisco.VARIABLE_DIRECTORIO, raiz.toString());
        almacenamiento = new AlmacenamientoDisco();
        almacenamiento.iniciar(entorno::get);
    }

    @Test
    void laClaveEsBase36DeLargoFijo() {
        String clave = almacenamiento.generarClave();

        assertTrue(clave.matches("[0-9a-z]{28}"), clave);
        assertNotEquals(clave, almacenamiento.generarClave());
    }

    @Test
    void laRutaSeReparteSegunLosPrimerosCaracteres() {
        String clave = "abcdefghijklmnopqrstuvwxyz01";

        assertEquals(
            raiz.resolve("ab").resolve("cd").resolve(clave),
            almacenamiento.ruta(clave)
        );
    }

    @Test
    void guardarAbrirYEliminar() throws IOException {
        String clave = almacenamiento.generarClave();
        byte[] contenido = "contenido de prueba".getBytes(
            StandardCharsets.UTF_8
        );

        long bytes = almacenamiento.guardar(
            clave,
            new ByteArrayInputStream(contenido)
        );

        assertEquals(contenido.length, bytes);
        Path ruta = almacenamiento.ruta(clave);
        assertTrue(Files.isRegularFile(ruta));
        assertFalse(Files.exists(ruta.resolveSibling(clave + ".tmp")));
        try (InputStream leido = almacenamiento.abrir(clave)) {
            assertArrayEquals(contenido, leido.readAllBytes());
        }

        almacenamiento.eliminar(clave);

        assertFalse(Files.exists(ruta));
        assertThrows(RecursoNoEncontradoException.class, () ->
            almacenamiento.abrir(clave)
        );
    }

    @Test
    void noSobrescribeUnArchivoExistente() {
        String clave = almacenamiento.generarClave();
        almacenamiento.guardar(
            clave,
            new ByteArrayInputStream(new byte[] { 1 })
        );

        assertThrows(IllegalStateException.class, () ->
            almacenamiento.guardar(
                clave,
                new ByteArrayInputStream(new byte[] { 2 })
            )
        );
    }

    @Test
    void rechazaClavesInvalidas() {
        assertThrows(IllegalArgumentException.class, () ->
            almacenamiento.ruta("../../etc/passwd")
        );
        assertThrows(IllegalArgumentException.class, () ->
            almacenamiento.ruta("ABC")
        );
        assertThrows(IllegalArgumentException.class, () ->
            almacenamiento.ruta(null)
        );
    }

    @Test
    void fallaSinDirectorioConfigurado() {
        entorno.remove(AlmacenamientoDisco.VARIABLE_DIRECTORIO);

        assertThrows(IllegalStateException.class, () ->
            new AlmacenamientoDisco().iniciar(entorno::get)
        );
    }

    @Test
    void creaElDirectorioSiNoExiste() {
        Path nuevo = raiz.resolve("no").resolve("existe");
        entorno.put(AlmacenamientoDisco.VARIABLE_DIRECTORIO, nuevo.toString());

        new AlmacenamientoDisco().iniciar(entorno::get);

        assertTrue(Files.isDirectory(nuevo));
    }
}
