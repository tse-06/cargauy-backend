package uy.carga.tse.logica.vehiculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uy.carga.tse.logica.testsupport.Entidades.conId;

import jakarta.data.page.PageRequest;
import java.io.ByteArrayInputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.vehiculo.DetalleItvDto;
import uy.carga.tse.datatypes.vehiculo.DetallePncDto;
import uy.carga.tse.datatypes.vehiculo.DetalleVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ReemplazarItvDto;
import uy.carga.tse.datatypes.vehiculo.ReemplazarPncDto;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.EntidadBase;
import uy.carga.tse.entidades.InspeccionTecnicaVehicular;
import uy.carga.tse.entidades.PermisoNacionalDeCirculacion;
import uy.carga.tse.entidades.Vehiculo;
import uy.carga.tse.logica.archivos.Adjuntos;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoSubido;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;
import uy.carga.tse.logica.empresa.AccesoEmpresa;
import uy.carga.tse.logica.empresa.EmpresaRepositorio;

class VehiculoServicioTest {

    private static final ReemplazarItvDto DATOS_ITV = new ReemplazarItvDto(
        "2027-12-31"
    );
    private static final ReemplazarPncDto DATOS_PNC = new ReemplazarPncDto(
        " PNC-123 ",
        "2028-06-30"
    );
    private static final ArchivoSubido SUBIDO = new ArchivoSubido(
        "documento.pdf",
        new ByteArrayInputStream(new byte[0])
    );

    private static final String URL_ARCHIVO =
        "http://localhost:8080/api/archivos/firmado";

    private final VehiculoServicio servicio = new VehiculoServicio();
    private final Empresa empresa = conId(new Empresa());
    private final Vehiculo vehiculo = conId(new Vehiculo());

    @BeforeEach
    void preparar() {
        vehiculo.setEmpresa(empresa);
        servicio.vehiculoRepositorio = mock(VehiculoRepositorio.class);
        servicio.empresaRepositorio = mock(EmpresaRepositorio.class);
        servicio.itvRepositorio = mock(ItvRepositorio.class);
        servicio.pncRepositorio = mock(PncRepositorio.class);
        servicio.servicioArchivos = mock(ArchivoServicioLocal.class);
        servicio.accesoEmpresa = mock(AccesoEmpresa.class);
        servicio.vehiculoMapper = new VehiculoMapperImpl();

        when(
            servicio.vehiculoRepositorio.findById(vehiculo.getId())
        ).thenReturn(Optional.of(vehiculo));
        when(
            servicio.servicioArchivos.adjuntos(
                anyString(),
                any(EntidadBase[].class)
            )
        ).thenReturn(Adjuntos.vacio());
        when(servicio.itvRepositorio.insert(any())).thenAnswer(i ->
            conId(i.getArgument(0))
        );
        when(servicio.itvRepositorio.update(any())).thenAnswer(i ->
            i.getArgument(0)
        );
        when(servicio.pncRepositorio.insert(any())).thenAnswer(i ->
            conId(i.getArgument(0))
        );
        when(servicio.pncRepositorio.update(any())).thenAnswer(i ->
            i.getArgument(0)
        );
    }

    @Test
    void creaLaItvSiNoTiene() {
        comoEncargado(true);
        archivoSubidoDeTipo("application/pdf");

        DetalleItvDto detalle = servicio.reemplazarItv(
            vehiculo.getId(),
            DATOS_ITV,
            SUBIDO
        );

        assertEquals(LocalDate.of(2027, 12, 31), detalle.fechaVencimiento());
        assertEquals("documento.pdf", detalle.nombreArchivo());
        assertEquals(URL_ARCHIVO, detalle.urlArchivo());
        verify(servicio.itvRepositorio).insert(any());
        verify(servicio.itvRepositorio, never()).update(any());
    }

    @Test
    void reemplazaLaPncExistente() {
        comoEncargado(true);
        archivoSubidoDeTipo("application/pdf");
        PermisoNacionalDeCirculacion existente = conId(
            new PermisoNacionalDeCirculacion()
        );
        existente.setVehiculo(vehiculo);
        vehiculo.setPermisoNacionalDeCirculacion(existente);

        DetallePncDto detalle = servicio.reemplazarPnc(
            vehiculo.getId(),
            DATOS_PNC,
            SUBIDO
        );

        assertEquals(existente.getId(), detalle.id());
        assertEquals("PNC-123", detalle.numero());
        assertEquals(LocalDate.of(2028, 6, 30), detalle.fechaVencimiento());
        verify(servicio.pncRepositorio).update(existente);
        verify(servicio.pncRepositorio, never()).insert(any());
        verify(servicio.servicioArchivos).reemplazar(
            existente,
            PermisoNacionalDeCirculacion.ADJUNTO_ARCHIVO,
            SUBIDO
        );
    }

    @Test
    void rechazaArchivosQueNoSonPdf() {
        comoEncargado(true);
        archivoSubidoDeTipo("image/png");

        assertThrows(ValidacionNegocioException.class, () ->
            servicio.reemplazarItv(vehiculo.getId(), DATOS_ITV, SUBIDO)
        );
        assertThrows(ValidacionNegocioException.class, () ->
            servicio.reemplazarPnc(vehiculo.getId(), DATOS_PNC, SUBIDO)
        );
    }

    @Test
    void respondeNoEncontradoAlEncargadoDeOtraEmpresa() {
        comoEncargado(false);

        RecursoNoEncontradoException itv = assertThrows(
            RecursoNoEncontradoException.class,
            () -> servicio.reemplazarItv(vehiculo.getId(), DATOS_ITV, SUBIDO)
        );
        RecursoNoEncontradoException pnc = assertThrows(
            RecursoNoEncontradoException.class,
            () -> servicio.reemplazarPnc(vehiculo.getId(), DATOS_PNC, SUBIDO)
        );
        assertEquals(CodigoError.VEHICULO_NO_ENCONTRADO, itv.codigo());
        assertEquals(CodigoError.VEHICULO_NO_ENCONTRADO, pnc.codigo());
        verifyNoInteractions(servicio.servicioArchivos);
        verify(servicio.itvRepositorio, never()).insert(any());
        verify(servicio.pncRepositorio, never()).insert(any());
    }

    @Test
    void listarPorEmpresaExigeAcceso() {
        doThrow(
            new RecursoNoEncontradoException(
                CodigoError.EMPRESA_NO_ENCONTRADA,
                "Empresa no encontrada"
            )
        )
            .when(servicio.accesoEmpresa)
            .exigirAcceso(empresa.getId());

        assertThrows(RecursoNoEncontradoException.class, () ->
            servicio.listarPorEmpresa(
                empresa.getId(),
                PageRequest.ofPage(1),
                List.of()
            )
        );
        verify(servicio.vehiculoRepositorio, never()).porEmpresa(
            any(),
            any(),
            any()
        );
    }

    @Test
    void obtenerIncluyeItvYPncConSusUrls() {
        InspeccionTecnicaVehicular itv = conId(
            new InspeccionTecnicaVehicular()
        );
        vehiculo.setInspeccionTecnicaVehicular(itv);
        PermisoNacionalDeCirculacion pnc = conId(
            new PermisoNacionalDeCirculacion()
        );
        pnc.setNumero("PNC-123");
        vehiculo.setPermisoNacionalDeCirculacion(pnc);
        // ITV y PNC comparten el nombre de la relacion: una sola consulta
        when(
            servicio.servicioArchivos.adjuntos(
                InspeccionTecnicaVehicular.ADJUNTO_ARCHIVO,
                itv,
                pnc
            )
        ).thenReturn(
            Adjuntos.vacio()
                .con(
                    itv,
                    InspeccionTecnicaVehicular.ADJUNTO_ARCHIVO,
                    archivo("itv.pdf"),
                    URL_ARCHIVO + "/itv.pdf"
                )
                .con(
                    pnc,
                    PermisoNacionalDeCirculacion.ADJUNTO_ARCHIVO,
                    archivo("pnc.pdf"),
                    URL_ARCHIVO + "/pnc.pdf"
                )
        );

        DetalleVehiculoDto detalle = servicio.obtener(vehiculo.getId());

        assertEquals(itv.getId(), detalle.itv().id());
        assertEquals("itv.pdf", detalle.itv().nombreArchivo());
        assertEquals(URL_ARCHIVO + "/itv.pdf", detalle.itv().urlArchivo());
        assertEquals(pnc.getId(), detalle.pnc().id());
        assertEquals("PNC-123", detalle.pnc().numero());
        assertEquals("pnc.pdf", detalle.pnc().nombreArchivo());
        assertEquals(URL_ARCHIVO + "/pnc.pdf", detalle.pnc().urlArchivo());
    }

    @Test
    void obtenerSinItvNiPncNoLosIncluye() {
        DetalleVehiculoDto detalle = servicio.obtener(vehiculo.getId());

        assertNull(detalle.itv());
        assertNull(detalle.pnc());
        verify(servicio.servicioArchivos, never()).url(any());
    }

    @Test
    void obtenerItvSinArchivoNoTieneUrl() {
        InspeccionTecnicaVehicular itv = conId(
            new InspeccionTecnicaVehicular()
        );
        vehiculo.setInspeccionTecnicaVehicular(itv);

        DetalleVehiculoDto detalle = servicio.obtener(vehiculo.getId());

        assertEquals(itv.getId(), detalle.itv().id());
        assertNull(detalle.itv().nombreArchivo());
        assertNull(detalle.itv().urlArchivo());
        verify(servicio.servicioArchivos, never()).url(any());
    }

    @Test
    void eliminarVehiculoBorraItvPncYArchivos() {
        InspeccionTecnicaVehicular itv = conId(
            new InspeccionTecnicaVehicular()
        );
        PermisoNacionalDeCirculacion pnc = conId(
            new PermisoNacionalDeCirculacion()
        );
        vehiculo.setInspeccionTecnicaVehicular(itv);
        vehiculo.setPermisoNacionalDeCirculacion(pnc);

        servicio.eliminarVehiculo(vehiculo.getId());

        verify(servicio.servicioArchivos).eliminarTodos(itv);
        verify(servicio.itvRepositorio).delete(itv);
        verify(servicio.servicioArchivos).eliminarTodos(pnc);
        verify(servicio.pncRepositorio).delete(pnc);
        verify(servicio.vehiculoRepositorio).delete(vehiculo);
    }

    private static Archivo archivo(String nombre) {
        Archivo archivo = conId(new Archivo());
        archivo.setNombre(nombre);
        return archivo;
    }

    private void comoEncargado(boolean gestiona) {
        when(servicio.accesoEmpresa.esEncargadoDe(empresa.getId())).thenReturn(
            gestiona
        );
    }

    private void archivoSubidoDeTipo(String tipoMime) {
        when(
            servicio.servicioArchivos.reemplazar(any(), anyString(), any())
        ).thenAnswer(i -> {
            Archivo archivo = new Archivo();
            archivo.setNombre(((ArchivoSubido) i.getArgument(2)).nombre());
            archivo.setTipoMime(tipoMime);
            return archivo;
        });
        when(servicio.servicioArchivos.url(any())).thenReturn(URL_ARCHIVO);
    }
}
