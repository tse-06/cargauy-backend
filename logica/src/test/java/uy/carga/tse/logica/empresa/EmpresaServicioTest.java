package uy.carga.tse.logica.empresa;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uy.carga.tse.logica.testsupport.Entidades.conId;

import jakarta.ejb.SessionContext;
import java.io.ByteArrayInputStream;
import java.security.Principal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.empresa.CrearEmpresaDto;
import uy.carga.tse.datatypes.empresa.DetalleEmpresaDto;
import uy.carga.tse.datatypes.empresa.ModificarEmpresaDto;
import uy.carga.tse.datatypes.empresa.ResumenEmpresaDto;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.permiso.DetallePermisoDto;
import uy.carga.tse.datatypes.permiso.ReemplazarPermisoDto;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.EntidadBase;
import uy.carga.tse.entidades.Permiso;
import uy.carga.tse.entidades.enums.Rubro;
import uy.carga.tse.logica.archivos.Adjuntos;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoSubido;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;
import uy.carga.tse.logica.usuario.EncargadoEmpresaRepositorio;

class EmpresaServicioTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    private static final ReemplazarPermisoDto DATOS = new ReemplazarPermisoDto(
        "ABC123",
        "2027-12-31"
    );
    private static final ArchivoSubido SUBIDO = new ArchivoSubido(
        "permiso.pdf",
        new ByteArrayInputStream(new byte[0])
    );

    private static final String URL_ARCHIVO =
        "http://localhost:8080/api/archivos/firmado";

    private final EmpresaServicio servicio = new EmpresaServicio();
    private final Empresa empresa = conId(new Empresa());

    @BeforeEach
    void preparar() {
        empresa.setRubro(Rubro.OTRO);
        servicio.empresaRepositorio = mock(EmpresaRepositorio.class);
        servicio.permisoRepositorio = mock(PermisoRepositorio.class);
        servicio.encargadoRepositorio = mock(EncargadoEmpresaRepositorio.class);
        servicio.servicioArchivos = mock(ArchivoServicioLocal.class);
        servicio.contexto = mock(SessionContext.class);
        servicio.accesoEmpresa = mock(AccesoEmpresa.class);
        servicio.empresaMapper = new EmpresaMapperImpl();

        Principal caller = () -> USUARIO_ID.toString();
        when(servicio.contexto.getCallerPrincipal()).thenReturn(caller);
        when(servicio.empresaRepositorio.findById(empresa.getId())).thenReturn(
            Optional.of(empresa)
        );
        when(
            servicio.servicioArchivos.adjuntos(
                anyString(),
                any(EntidadBase[].class)
            )
        ).thenReturn(Adjuntos.vacio());
        when(servicio.permisoRepositorio.insert(any())).thenAnswer(i ->
            conId(i.getArgument(0))
        );
        when(servicio.permisoRepositorio.update(any())).thenAnswer(i ->
            i.getArgument(0)
        );
    }

    @Test
    void creaElPermisoSiNoTiene() {
        comoEncargado(true);
        archivoSubidoDeTipo("application/pdf");

        DetallePermisoDto detalle = servicio.reemplazarPermiso(
            empresa.getId(),
            DATOS,
            SUBIDO
        );

        assertEquals("ABC123", detalle.codigoValidacion());
        assertEquals(LocalDate.of(2027, 12, 31), detalle.fechaVencimiento());
        assertEquals("permiso.pdf", detalle.nombreArchivo());
        assertEquals(URL_ARCHIVO, detalle.urlArchivo());
        verify(servicio.permisoRepositorio).insert(any());
        verify(servicio.permisoRepositorio, never()).update(any());
    }

    @Test
    void reemplazaElPermisoExistente() {
        comoEncargado(true);
        archivoSubidoDeTipo("application/pdf");
        Permiso existente = conId(new Permiso());
        existente.setEmpresa(empresa);
        empresa.setPermiso(existente);

        DetallePermisoDto detalle = servicio.reemplazarPermiso(
            empresa.getId(),
            DATOS,
            SUBIDO
        );

        assertEquals(existente.getId(), detalle.id());
        assertEquals("ABC123", existente.getCodigoValidacion());
        verify(servicio.permisoRepositorio).update(existente);
        verify(servicio.permisoRepositorio, never()).insert(any());
        verify(servicio.servicioArchivos).reemplazar(
            existente,
            Permiso.ADJUNTO_ARCHIVO,
            SUBIDO
        );
    }

    @Test
    void rechazaArchivosQueNoSonPdf() {
        comoEncargado(true);
        archivoSubidoDeTipo("image/png");

        assertThrows(ValidacionNegocioException.class, () ->
            servicio.reemplazarPermiso(empresa.getId(), DATOS, SUBIDO)
        );
    }

    @Test
    void respondeNoEncontradaAlEncargadoDeOtraEmpresa() {
        comoEncargado(false);

        RecursoNoEncontradoException e = assertThrows(
            RecursoNoEncontradoException.class,
            () -> servicio.reemplazarPermiso(empresa.getId(), DATOS, SUBIDO)
        );
        assertEquals(CodigoError.EMPRESA_NO_ENCONTRADA, e.codigo());
        verifyNoInteractions(servicio.servicioArchivos);
        verify(servicio.permisoRepositorio, never()).insert(any());
    }

    @Test
    void listarDelEncargadoDevuelveSusEmpresas() {
        empresa.setNombre("Transportes SA");
        when(servicio.encargadoRepositorio.empresasDe(USUARIO_ID)).thenReturn(
            List.of(empresa)
        );

        List<ResumenEmpresaDto> empresas = servicio.listarDelEncargado();

        assertEquals(1, empresas.size());
        assertEquals(empresa.getId(), empresas.get(0).id());
        assertEquals("Transportes SA", empresas.get(0).nombre());
    }

    @Test
    void eliminarEmpresaBorraSuPermisoYArchivos() {
        Permiso permiso = conId(new Permiso());
        empresa.setPermiso(permiso);

        servicio.eliminarEmpresa(empresa.getId());

        verify(servicio.servicioArchivos).eliminarTodos(permiso);
        verify(servicio.permisoRepositorio).delete(permiso);
        verify(servicio.empresaRepositorio).delete(empresa);
    }

    @Test
    void obtenerIncluyeElPermisoConLaUrlDeSuArchivo() {
        Permiso permiso = permisoConArchivo();

        DetalleEmpresaDto detalle = servicio.obtener(empresa.getId());

        assertEquals(permiso.getId(), detalle.permiso().id());
        assertEquals("ABC123", detalle.permiso().codigoValidacion());
        assertEquals("permiso.pdf", detalle.permiso().nombreArchivo());
        assertEquals(URL_ARCHIVO, detalle.permiso().urlArchivo());
    }

    @Test
    void obtenerSinPermisoNoLoIncluye() {
        DetalleEmpresaDto detalle = servicio.obtener(empresa.getId());

        assertNull(detalle.permiso());
        verify(servicio.servicioArchivos, never()).url(any());
    }

    @Test
    void obtenerPermisoSinArchivoNoTieneUrl() {
        Permiso permiso = conId(new Permiso());
        empresa.setPermiso(permiso);

        DetalleEmpresaDto detalle = servicio.obtener(empresa.getId());

        assertEquals(permiso.getId(), detalle.permiso().id());
        assertNull(detalle.permiso().nombreArchivo());
        assertNull(detalle.permiso().urlArchivo());
        verify(servicio.servicioArchivos, never()).url(any());
    }

    @Test
    void modificarEmpresaIncluyeElPermiso() {
        permisoConArchivo();
        when(servicio.empresaRepositorio.update(empresa)).thenReturn(empresa);

        DetalleEmpresaDto detalle = servicio.modificarEmpresa(
            empresa.getId(),
            new ModificarEmpresaDto(
                "Nueva",
                "Nueva SA",
                "123",
                "Calle 1",
                "otro"
            )
        );

        assertEquals("Nueva", detalle.nombre());
        assertEquals(URL_ARCHIVO, detalle.permiso().urlArchivo());
    }

    @Test
    void crearEmpresaNoTienePermiso() {
        when(servicio.empresaRepositorio.insert(any())).thenAnswer(i ->
            conId(i.getArgument(0))
        );

        DetalleEmpresaDto detalle = servicio.crearEmpresa(
            new CrearEmpresaDto("Nueva", "Nueva SA", "123", "Calle 1", "otro")
        );

        assertNull(detalle.permiso());
        verifyNoInteractions(servicio.permisoRepositorio);
    }

    /** Permiso de la empresa con un archivo adjunto. */
    private Permiso permisoConArchivo() {
        Permiso permiso = conId(new Permiso());
        permiso.setCodigoValidacion("ABC123");
        empresa.setPermiso(permiso);
        Archivo archivo = conId(new Archivo());
        archivo.setNombre("permiso.pdf");
        when(
            servicio.servicioArchivos.adjuntos(Permiso.ADJUNTO_ARCHIVO, permiso)
        ).thenReturn(
            Adjuntos.vacio().con(
                permiso,
                Permiso.ADJUNTO_ARCHIVO,
                archivo,
                URL_ARCHIVO
            )
        );
        return permiso;
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
