package uy.carga.tse.logica.archivos;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import jakarta.transaction.Status;
import jakarta.transaction.Synchronization;
import jakarta.transaction.TransactionSynchronizationRegistry;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.ArgumentCaptor;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.ArchivoAdjunto;
import uy.carga.tse.entidades.EntidadBase;
import uy.carga.tse.entidades.InspeccionTecnicaVehicular;
import uy.carga.tse.entidades.Permiso;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoDescarga;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.testsupport.Entidades;

class ArchivoServicioTest {

    @Test
    void nombreArchivoDescartaLosDirectorios() {
        assertEquals(
            "itv.pdf",
            ArchivoServicio.nombreArchivo("C:\\docs\\itv.pdf")
        );
        assertEquals("itv.pdf", ArchivoServicio.nombreArchivo("../../itv.pdf"));
        assertEquals("archivo", ArchivoServicio.nombreArchivo("carpeta/"));
        assertEquals("archivo", ArchivoServicio.nombreArchivo(null));
    }

    @Test
    void nombreArchivoLargoConservaLaExtension() {
        String nombre = ArchivoServicio.nombreArchivo("a".repeat(300) + ".pdf");

        assertEquals(ArchivoServicio.LARGO_MAXIMO_NOMBRE, nombre.length());
        assertTrue(nombre.endsWith(".pdf"));
    }

    @Test
    void tipoMimeSeDetectaPorElContenido(@TempDir Path dir) throws IOException {
        Path png = dir.resolve("png");
        Files.write(png, new byte[] {
            (byte) 0x89,
            'P',
            'N',
            'G',
            '\r',
            '\n',
            0x1a,
            '\n',
            0,
            0,
            0,
            0x0d,
        });
        Path pdf = dir.resolve("pdf");
        Files.writeString(pdf, "%PDF-1.7\n");

        // El nombre no pisa lo que dice el contenido
        assertEquals("image/png", ArchivoServicio.tipoMime(png, "foto.txt"));
        assertEquals(
            "application/pdf",
            ArchivoServicio.tipoMime(pdf, "archivo")
        );
    }

    @Test
    void tipoMimeSinFirmaUsaElNombre(@TempDir Path dir) throws IOException {
        Path csv = dir.resolve("csv");
        Files.writeString(csv, "matricula,peso\nABC1234,1000\n");

        assertEquals("text/csv", ArchivoServicio.tipoMime(csv, "carga.csv"));
    }

    @Test
    void tipoMimePorDefecto(@TempDir Path dir) throws IOException {
        Path binario = dir.resolve("bin");
        Files.write(binario, new byte[] {
            0,
            1,
            2,
            3,
            (byte) 0xfe,
            (byte) 0xff,
        });

        assertEquals(
            "application/octet-stream",
            ArchivoServicio.tipoMime(binario, "archivo")
        );
        assertEquals(
            "application/octet-stream",
            ArchivoServicio.tipoMime(dir.resolve("no-existe"), "archivo")
        );
    }

    @Test
    void eliminarHuerfanoBorraElContenidoAlConfirmar(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);
        UUID archivoId = UUID.randomUUID();
        Archivo archivo = guardado(servicio);
        Path ruta = servicio.almacenamiento.ruta(archivo.getClave());
        when(servicio.archivoRepositorio.findById(archivoId)).thenReturn(
            Optional.of(archivo)
        );

        assertTrue(servicio.eliminarHuerfano(archivoId));

        verify(servicio.archivoRepositorio).delete(archivo);
        assertTrue(Files.exists(ruta));
        alTerminar(servicio, Status.STATUS_COMMITTED);
        assertFalse(Files.exists(ruta));
    }

    @Test
    void eliminarHuerfanoConservaElContenidoSiHayRollback(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);
        UUID archivoId = UUID.randomUUID();
        Archivo archivo = guardado(servicio);
        when(servicio.archivoRepositorio.findById(archivoId)).thenReturn(
            Optional.of(archivo)
        );

        servicio.eliminarHuerfano(archivoId);

        alTerminar(servicio, Status.STATUS_ROLLEDBACK);
        assertTrue(
            Files.exists(servicio.almacenamiento.ruta(archivo.getClave()))
        );
    }

    @Test
    void eliminarHuerfanoNoBorraUnArchivoAdjunto(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);
        UUID archivoId = UUID.randomUUID();
        when(servicio.adjuntoRepositorio.referencias(archivoId)).thenReturn(1L);

        assertFalse(servicio.eliminarHuerfano(archivoId));

        verify(servicio.archivoRepositorio, never()).delete(any());
        verifyNoInteractions(servicio.transacciones);
    }

    @Test
    void eliminarHuerfanoInexistente(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);

        assertFalse(servicio.eliminarHuerfano(UUID.randomUUID()));

        verify(servicio.archivoRepositorio, never()).delete(any());
    }

    @Test
    void urlLlevaLaBaseYUnIdFirmado(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);
        Archivo archivo = guardado(servicio);
        when(servicio.archivoRepositorio.findById(archivo.getId())).thenReturn(
            Optional.of(archivo)
        );

        String url = servicio.url(archivo);

        String prefijo = FirmaArchivosTest.URL_BASE + "/archivos/";
        assertTrue(url.startsWith(prefijo), url);
        String idFirmado = url.substring(prefijo.length());
        assertEquals(archivo.getId(), servicio.firma.verificar(idFirmado));
    }

    @Test
    void abrirFirmadoDevuelveElContenidoYLosMetadatos(@TempDir Path dir)
        throws IOException {
        ArchivoServicio servicio = servicio(dir);
        Archivo archivo = guardado(servicio);
        when(servicio.archivoRepositorio.findById(archivo.getId())).thenReturn(
            Optional.of(archivo)
        );

        ArchivoDescarga descarga = servicio.abrirFirmado(
            servicio.firma.firmar(archivo.getId())
        );

        assertEquals("permiso.pdf", descarga.nombre());
        assertEquals("application/pdf", descarga.tipoMime());
        assertEquals(3L, descarga.tamano());
        assertTrue(descarga.seMuestraEnNavegador());
        try (InputStream contenido = descarga.contenido()) {
            assertArrayEquals(new byte[] { 1, 2, 3 }, contenido.readAllBytes());
        }
    }

    @Test
    void abrirFirmadoDeUnArchivoBorradoDa404(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);
        String idFirmado = servicio.firma.firmar(UUID.randomUUID());

        RecursoNoEncontradoException e = assertThrows(
            RecursoNoEncontradoException.class,
            () -> servicio.abrirFirmado(idFirmado)
        );
        assertEquals(CodigoError.ARCHIVO_NO_ENCONTRADO, e.codigo());
    }

    @Test
    void abrirFirmadoInvalidoNoConsultaLaBase(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);

        assertThrows(RecursoNoEncontradoException.class, () ->
            servicio.abrirFirmado("no-firmado")
        );
        verifyNoInteractions(servicio.archivoRepositorio);
    }

    @Test
    void abrirFirmadoVencidoNoConsultaLaBase(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);
        String idFirmado = servicio.firma.firmar(UUID.randomUUID());
        servicio.firma.reloj = Clock.offset(
            servicio.firma.reloj,
            FirmaArchivos.VIGENCIA_POR_DEFECTO
        );

        assertThrows(AccesoDenegadoException.class, () ->
            servicio.abrirFirmado(idFirmado)
        );
        verifyNoInteractions(servicio.archivoRepositorio);
    }

    @Test
    void seMuestraEnNavegadorSoloParaTiposSeguros() {
        assertTrue(ArchivoServicio.seMuestraEnNavegador("application/pdf"));
        assertTrue(ArchivoServicio.seMuestraEnNavegador("image/png"));
        assertFalse(ArchivoServicio.seMuestraEnNavegador("text/html"));
        assertFalse(ArchivoServicio.seMuestraEnNavegador("image/svg+xml"));
        assertFalse(
            ArchivoServicio.seMuestraEnNavegador("application/octet-stream")
        );
        assertFalse(ArchivoServicio.seMuestraEnNavegador(null));
    }

    @Test
    void adjuntosBuscaTodasLasEntidadesEnUnaConsulta(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);
        Permiso permiso = Entidades.conId(new Permiso());
        InspeccionTecnicaVehicular itv = Entidades.conId(
            new InspeccionTecnicaVehicular()
        );
        Archivo primero = archivo("primero.pdf");
        Archivo segundo = archivo("segundo.pdf");
        Archivo deItv = archivo("itv.pdf");
        when(
            servicio.adjuntoRepositorio.deEntidades(
                List.of(permiso.getId(), itv.getId()),
                "archivo"
            )
        ).thenReturn(
            List.of(
                vinculo(permiso, primero),
                vinculo(permiso, segundo),
                vinculo(itv, deItv)
            )
        );

        Adjuntos adjuntos = servicio.adjuntos("archivo", permiso, null, itv);

        // De cada relacion queda el primero en orden de alta
        assertEquals("primero.pdf", adjuntos.nombreArchivo(permiso, "archivo"));
        assertEquals(servicio.url(primero), adjuntos.url(permiso, "archivo"));
        assertEquals("itv.pdf", adjuntos.nombreArchivo(itv, "archivo"));
        assertNull(adjuntos.nombreArchivo(itv, "otra"));
        assertNull(adjuntos.url(null, "archivo"));
    }

    @Test
    void adjuntosSinEntidadesNoConsulta(@TempDir Path dir) {
        ArchivoServicio servicio = servicio(dir);

        Adjuntos adjuntos = servicio.adjuntos("archivo", null, null);

        assertNull(adjuntos.url(Entidades.conId(new Permiso()), "archivo"));
        verifyNoInteractions(servicio.adjuntoRepositorio);
    }

    private static Archivo archivo(String nombre) {
        Archivo archivo = Entidades.conId(new Archivo());
        archivo.setNombre(nombre);
        return archivo;
    }

    private static ArchivoAdjunto vinculo(
        EntidadBase entidad,
        Archivo archivo
    ) {
        ArchivoAdjunto vinculo = new ArchivoAdjunto();
        vinculo.setTipoEntidad(entidad.getClass().getSimpleName());
        vinculo.setEntidadId(entidad.getId());
        vinculo.setNombre("archivo");
        vinculo.setArchivo(archivo);
        return vinculo;
    }

    private static ArchivoServicio servicio(Path dir) {
        ArchivoServicio servicio = new ArchivoServicio();
        servicio.archivoRepositorio = mock(ArchivoRepositorio.class);
        servicio.adjuntoRepositorio = mock(ArchivoAdjuntoRepositorio.class);
        servicio.transacciones = mock(TransactionSynchronizationRegistry.class);
        servicio.almacenamiento = new AlmacenamientoDisco();
        servicio.almacenamiento.iniciar(
            Map.of(AlmacenamientoDisco.VARIABLE_DIRECTORIO, dir.toString())::get
        );
        servicio.firma = FirmaArchivosTest.firma(FirmaArchivosTest.entorno());
        return servicio;
    }

    /** Archivo con contenido en disco. */
    private static Archivo guardado(ArchivoServicio servicio) {
        Archivo archivo = Entidades.conId(new Archivo());
        archivo.setClave(servicio.almacenamiento.generarClave());
        archivo.setNombre("permiso.pdf");
        archivo.setTipoMime("application/pdf");
        archivo.setTamano(
            servicio.almacenamiento.guardar(
                archivo.getClave(),
                new ByteArrayInputStream(new byte[] { 1, 2, 3 })
            )
        );
        return archivo;
    }

    /** Termina la transaccion con el estado dado, ejecutando los callbacks registrados. */
    private static void alTerminar(ArchivoServicio servicio, int estado) {
        ArgumentCaptor<Synchronization> sincronizacion =
            ArgumentCaptor.forClass(Synchronization.class);
        verify(servicio.transacciones).registerInterposedSynchronization(
            sincronizacion.capture()
        );
        sincronizacion.getValue().afterCompletion(estado);
    }
}
