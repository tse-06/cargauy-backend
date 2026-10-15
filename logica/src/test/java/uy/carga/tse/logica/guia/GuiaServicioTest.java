package uy.carga.tse.logica.guia;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
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
import java.math.BigDecimal;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.guia.CoordenadaDto;
import uy.carga.tse.datatypes.guia.CrearGuiaDto;
import uy.carga.tse.datatypes.guia.DetalleGuiaDto;
import uy.carga.tse.entidades.Chofer;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.InspeccionTecnicaVehicular;
import uy.carga.tse.entidades.PermisoNacionalDeCirculacion;
import uy.carga.tse.entidades.Usuario;
import uy.carga.tse.entidades.Vehiculo;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.comun.excepciones.ExcepcionNegocio;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;
import uy.carga.tse.logica.empresa.EmpresaRepositorio;
import uy.carga.tse.logica.testsupport.MutableClock;
import uy.carga.tse.logica.usuario.ChoferRepositorio;
import uy.carga.tse.logica.usuario.EncargadoEmpresaRepositorio;
import uy.carga.tse.logica.vehiculo.VehiculoRepositorio;

class GuiaServicioTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();
    // 12:00 en Montevideo (UTC-3)
    private static final Instant AHORA = Instant.parse("2026-10-12T15:00:00Z");
    private static final LocalDate HOY = LocalDate.of(2026, 10, 12);
    private static final CoordenadaDto MONTEVIDEO = new CoordenadaDto(
        -34.9058,
        -56.1913
    );
    private static final CoordenadaDto SALTO = new CoordenadaDto(
        -31.3833,
        -57.9667
    );

    private final GuiaServicio servicio = new GuiaServicio();
    private final MutableClock reloj = new MutableClock(AHORA);
    private final Empresa empresa = conId(new Empresa());
    private final Vehiculo vehiculo = conId(new Vehiculo());
    private final Chofer chofer = conId(new Chofer());
    private final InspeccionTecnicaVehicular itv = conId(
        new InspeccionTecnicaVehicular()
    );
    private final PermisoNacionalDeCirculacion pnc = conId(
        new PermisoNacionalDeCirculacion()
    );

    @BeforeEach
    void preparar() {
        servicio.guiaRepositorio = mock(GuiaRepositorio.class);
        servicio.empresaRepositorio = mock(EmpresaRepositorio.class);
        servicio.vehiculoRepositorio = mock(VehiculoRepositorio.class);
        servicio.choferRepositorio = mock(ChoferRepositorio.class);
        servicio.encargadoRepositorio = mock(EncargadoEmpresaRepositorio.class);
        servicio.contexto = mock(SessionContext.class);
        servicio.reloj = reloj;
        servicio.guiaMapper = new GuiaMapperImpl();

        vehiculo.setEmpresa(empresa);
        vehiculo.setCapacidad(new BigDecimal("20000"));
        itv.setFechaVencimiento(HOY.plusYears(1));
        pnc.setFechaVencimiento(HOY.plusYears(1));
        chofer.setUsuario(conId(new Usuario()));
        vehiculo.setInspeccionTecnicaVehicular(itv);
        vehiculo.setPermisoNacionalDeCirculacion(pnc);

        Principal caller = () -> USUARIO_ID.toString();
        when(servicio.contexto.getCallerPrincipal()).thenReturn(caller);
        when(
            servicio.contexto.isCallerInRole(Roles.ENCARGADO_EMPRESA)
        ).thenReturn(true);
        when(
            servicio.encargadoRepositorio.gestiona(USUARIO_ID, empresa.getId())
        ).thenReturn(true);
        when(servicio.empresaRepositorio.findById(empresa.getId())).thenReturn(
            Optional.of(empresa)
        );
        when(
            servicio.vehiculoRepositorio.findById(vehiculo.getId())
        ).thenReturn(Optional.of(vehiculo));
        when(
            servicio.choferRepositorio.deEmpresa(
                chofer.getUsuario().getId(),
                empresa.getId()
            )
        ).thenReturn(Optional.of(chofer));
        when(servicio.guiaRepositorio.insert(any())).thenAnswer(i ->
            conId(i.getArgument(0))
        );
    }

    @Test
    void creaLaGuiaIncompletaSinVehiculoNiChofer() {
        DetalleGuiaDto detalle = servicio.crearGuia(datos(null, null));

        assertEquals("INCOMPLETA", detalle.estado());
        assertEquals(empresa.getId(), detalle.empresaId());
        assertEquals("ALIMENTOS", detalle.rubroCliente());
        assertEquals("GENERAL", detalle.tipoCarga());
        assertEquals("Montevideo", detalle.origen());
        assertEquals(MONTEVIDEO, detalle.ubicacionOrigen());
        assertEquals(SALTO, detalle.ubicacionDestino());
        assertNull(detalle.vehiculoId());
        assertNull(detalle.choferId());
        verifyNoInteractions(
            servicio.vehiculoRepositorio,
            servicio.choferRepositorio
        );
    }

    @Test
    void creaLaGuiaPendienteConVehiculoYChofer() {
        DetalleGuiaDto detalle = servicio.crearGuia(
            datos(vehiculo.getId(), chofer.getUsuario().getId())
        );

        assertEquals("PENDIENTE", detalle.estado());
        assertEquals(vehiculo.getId(), detalle.vehiculoId());
        assertEquals(chofer.getUsuario().getId(), detalle.choferId());
    }

    @Test
    void creaLaGuiaIncompletaConSoloVehiculo() {
        DetalleGuiaDto detalle = servicio.crearGuia(
            datos(vehiculo.getId(), null)
        );

        assertEquals("INCOMPLETA", detalle.estado());
        assertEquals(vehiculo.getId(), detalle.vehiculoId());
    }

    @Test
    void noEncuentraLaEmpresaSiNoEsEncargado() {
        when(
            servicio.encargadoRepositorio.gestiona(USUARIO_ID, empresa.getId())
        ).thenReturn(false);

        rechaza(
            RecursoNoEncontradoException.class,
            CodigoError.EMPRESA_NO_ENCONTRADA,
            () -> servicio.crearGuia(datos(null, null))
        );
    }

    @Test
    void noEncuentraLaEmpresaSiElCallerNoTieneRolEncargado() {
        when(servicio.contexto.isCallerInRole(anyString())).thenReturn(false);

        rechaza(
            RecursoNoEncontradoException.class,
            CodigoError.EMPRESA_NO_ENCONTRADA,
            () -> servicio.crearGuia(datos(null, null))
        );
        verifyNoInteractions(servicio.encargadoRepositorio);
    }

    @Test
    void rechazaFechaAnteriorAHoy() {
        rechaza(
            ValidacionNegocioException.class,
            CodigoError.FECHA_PASADA,
            () -> servicio.crearGuia(conFecha(HOY.minusDays(1)))
        );
    }

    @Test
    void aceptaFechaDeHoyEnMontevideo() {
        // 22:00 del 12 en Montevideo, ya 13 en UTC
        reloj.set(Instant.parse("2026-10-13T01:00:00Z"));

        DetalleGuiaDto detalle = servicio.crearGuia(conFecha(HOY));

        assertEquals(HOY, detalle.fecha());
    }

    @Test
    void rechazaOrigenIgualADestino() {
        CrearGuiaDto dto = new CrearGuiaDto(
            empresa.getId(),
            "alimentos",
            "general",
            new BigDecimal("15000"),
            HOY,
            "Montevideo",
            "Montevideo",
            MONTEVIDEO,
            new CoordenadaDto(-34.9058, -56.1913),
            null,
            null
        );

        rechaza(
            ValidacionNegocioException.class,
            CodigoError.ORIGEN_IGUAL_DESTINO,
            () -> servicio.crearGuia(dto)
        );
    }

    @Test
    void noEncuentraElVehiculoDeOtraEmpresa() {
        vehiculo.setEmpresa(conId(new Empresa()));

        rechaza(
            RecursoNoEncontradoException.class,
            CodigoError.VEHICULO_NO_ENCONTRADO,
            () -> servicio.crearGuia(datos(vehiculo.getId(), null))
        );
    }

    @Test
    void noEncuentraElVehiculoInexistente() {
        rechaza(
            RecursoNoEncontradoException.class,
            CodigoError.VEHICULO_NO_ENCONTRADO,
            () -> servicio.crearGuia(datos(UUID.randomUUID(), null))
        );
    }

    @Test
    void rechazaPesoMayorALaCapacidad() {
        vehiculo.setCapacidad(new BigDecimal("14999.999"));

        rechaza(
            ValidacionNegocioException.class,
            CodigoError.CAPACIDAD_EXCEDIDA,
            () -> servicio.crearGuia(datos(vehiculo.getId(), null))
        );
    }

    @Test
    void aceptaPesoIgualALaCapacidad() {
        vehiculo.setCapacidad(new BigDecimal("15000.000"));

        DetalleGuiaDto detalle = servicio.crearGuia(
            datos(vehiculo.getId(), null)
        );

        assertEquals(vehiculo.getId(), detalle.vehiculoId());
    }

    @Test
    void rechazaSinItv() {
        vehiculo.setInspeccionTecnicaVehicular(null);

        rechaza(
            ValidacionNegocioException.class,
            CodigoError.ITV_NO_VIGENTE,
            () -> servicio.crearGuia(datos(vehiculo.getId(), null))
        );
    }

    @Test
    void rechazaItvVencidaParaLaFecha() {
        itv.setFechaVencimiento(HOY.plusDays(4));

        rechaza(
            ValidacionNegocioException.class,
            CodigoError.ITV_NO_VIGENTE,
            () -> servicio.crearGuia(conVehiculoYFecha(HOY.plusDays(5)))
        );
    }

    @Test
    void aceptaItvQueVenceElDiaDeLaGuia() {
        itv.setFechaVencimiento(HOY.plusDays(5));

        DetalleGuiaDto detalle = servicio.crearGuia(
            conVehiculoYFecha(HOY.plusDays(5))
        );

        assertEquals(vehiculo.getId(), detalle.vehiculoId());
    }

    @Test
    void rechazaSinPnc() {
        vehiculo.setPermisoNacionalDeCirculacion(null);

        rechaza(
            ValidacionNegocioException.class,
            CodigoError.PNC_NO_VIGENTE,
            () -> servicio.crearGuia(datos(vehiculo.getId(), null))
        );
    }

    @Test
    void rechazaPncVencidaParaLaFecha() {
        pnc.setFechaVencimiento(HOY.plusDays(4));

        rechaza(
            ValidacionNegocioException.class,
            CodigoError.PNC_NO_VIGENTE,
            () -> servicio.crearGuia(conVehiculoYFecha(HOY.plusDays(5)))
        );
    }

    @Test
    void noEncuentraElChoferDeOtraEmpresa() {
        // deEmpresa no devuelve nada para un chofer que no es de la empresa
        rechaza(
            RecursoNoEncontradoException.class,
            CodigoError.CHOFER_NO_ENCONTRADO,
            () -> servicio.crearGuia(datos(null, UUID.randomUUID()))
        );
    }

    @Test
    void rechazaTipoCargaInvalido() {
        CrearGuiaDto dto = new CrearGuiaDto(
            empresa.getId(),
            "alimentos",
            "liquida",
            new BigDecimal("15000"),
            HOY,
            "Montevideo",
            "Salto",
            MONTEVIDEO,
            SALTO,
            null,
            null
        );

        rechaza(
            ValidacionNegocioException.class,
            CodigoError.TIPO_CARGA_INVALIDO,
            () -> servicio.crearGuia(dto)
        );
    }

    private void rechaza(
        Class<? extends ExcepcionNegocio> tipo,
        CodigoError codigo,
        Executable accion
    ) {
        ExcepcionNegocio e = assertThrows(tipo, accion);
        assertEquals(codigo, e.codigo());
        verify(servicio.guiaRepositorio, never()).insert(any());
    }

    private CrearGuiaDto datos(UUID vehiculoId, UUID choferId) {
        return armar(HOY.plusDays(1), vehiculoId, choferId);
    }

    private CrearGuiaDto conFecha(LocalDate fecha) {
        return armar(fecha, null, null);
    }

    private CrearGuiaDto conVehiculoYFecha(LocalDate fecha) {
        return armar(fecha, vehiculo.getId(), null);
    }

    private CrearGuiaDto armar(
        LocalDate fecha,
        UUID vehiculoId,
        UUID choferId
    ) {
        return new CrearGuiaDto(
            empresa.getId(),
            " alimentos ",
            "General",
            new BigDecimal("15000"),
            fecha,
            " Montevideo ",
            "Salto",
            MONTEVIDEO,
            SALTO,
            vehiculoId,
            choferId
        );
    }
}
