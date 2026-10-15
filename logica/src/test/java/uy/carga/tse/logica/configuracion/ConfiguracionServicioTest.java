package uy.carga.tse.logica.configuracion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uy.carga.tse.logica.testsupport.Entidades.conId;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.configuracion.DetalleConfiguracionFiscalizacionDto;
import uy.carga.tse.datatypes.configuracion.ModificarConfiguracionFiscalizacionDto;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.ConfiguracionSistema;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;

class ConfiguracionServicioTest {

    private final ConfiguracionServicio servicio = new ConfiguracionServicio();
    private final ConfiguracionSistema configuracion = conId(
        new ConfiguracionSistema()
    );

    @BeforeEach
    void preparar() {
        configuracion.setToleranciaExcesoCarga(new BigDecimal("5.000"));
        configuracion.setToleranciaDesvioRuta(new BigDecimal("5.00"));
        configuracion.setToleranciaDiferenciaPesoBalanzas(
            new BigDecimal("2.000")
        );
        configuracion.setPeriodoDescargo(Duration.ofDays(5));
        configuracion.setPeriodoMaximoViaje(Duration.ofHours(48));
        configuracion.setCantidadInfraccionesRecurrentes(5);
        configuracion.setPeriodoInfraccionRecurrente(Duration.ofDays(365));

        servicio.configuracionMapper = new ConfiguracionMapperImpl();
        servicio.configuracionRepositorio = mock(
            ConfiguracionRepositorio.class
        );
        when(servicio.configuracionRepositorio.findAll()).thenAnswer(i ->
            Stream.of(configuracion)
        );
    }

    @Test
    void obtieneLosParametrosVigentes() {
        DetalleConfiguracionFiscalizacionDto detalle =
            servicio.obtenerFiscalizacion();

        assertEquals(new BigDecimal("5.000"), detalle.toleranciaExcesoCarga());
        assertEquals(
            new BigDecimal("5.00"),
            detalle.toleranciaDesvioRutaMetros()
        );
        assertEquals(
            new BigDecimal("2.000"),
            detalle.toleranciaDiferenciaPesoBalanzas()
        );
        assertEquals(120, detalle.periodoDescargoHoras());
        assertEquals(48, detalle.periodoMaximoViajeHoras());
        assertEquals(5, detalle.cantidadInfraccionesRecurrentes());
        assertEquals(365, detalle.periodoInfraccionRecurrenteDias());
    }

    @Test
    void modificaTodosLosParametros() {
        when(servicio.configuracionRepositorio.update(configuracion))
            .thenReturn(configuracion);

        DetalleConfiguracionFiscalizacionDto detalle =
            servicio.modificarFiscalizacion(
                new ModificarConfiguracionFiscalizacionDto(
                    new BigDecimal("10.500"),
                    new BigDecimal("30.00"),
                    new BigDecimal("1.250"),
                    72L,
                    24L,
                    3,
                    90L
                )
            );

        assertEquals(
            new BigDecimal("10.500"),
            configuracion.getToleranciaExcesoCarga()
        );
        assertEquals(
            new BigDecimal("30.00"),
            configuracion.getToleranciaDesvioRuta()
        );
        assertEquals(
            new BigDecimal("1.250"),
            configuracion.getToleranciaDiferenciaPesoBalanzas()
        );
        assertEquals(Duration.ofHours(72), configuracion.getPeriodoDescargo());
        assertEquals(
            Duration.ofHours(24),
            configuracion.getPeriodoMaximoViaje()
        );
        assertEquals(3, configuracion.getCantidadInfraccionesRecurrentes());
        assertEquals(
            Duration.ofDays(90),
            configuracion.getPeriodoInfraccionRecurrente()
        );
        assertEquals(72, detalle.periodoDescargoHoras());
        assertEquals(90, detalle.periodoInfraccionRecurrenteDias());
    }

    @Test
    void noGuardaNadaSiNoHayConfiguracion() {
        when(servicio.configuracionRepositorio.findAll()).thenAnswer(i ->
            Stream.empty()
        );

        RecursoNoEncontradoException e = assertThrows(
            RecursoNoEncontradoException.class,
            () ->
                servicio.modificarFiscalizacion(
                    new ModificarConfiguracionFiscalizacionDto(
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        BigDecimal.ONE,
                        1L,
                        1L,
                        1,
                        1L
                    )
                )
        );

        assertEquals(CodigoError.CONFIGURACION_NO_ENCONTRADA, e.codigo());
        verify(servicio.configuracionRepositorio, never()).update(any());
    }
}
