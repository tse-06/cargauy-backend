package uy.carga.tse.logica.archivos;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class LimpiezaArchivosTest {

    private final LimpiezaArchivos limpieza = new LimpiezaArchivos();

    @BeforeEach
    void preparar() {
        limpieza.archivoRepositorio = mock(ArchivoRepositorio.class);
        limpieza.servicioArchivos = mock(ArchivoServicioLocal.class);
    }

    @Test
    void buscaLosHuerfanosDeMasDeUnDia() {
        Instant antes = Instant.now().minus(LimpiezaArchivos.ANTIGUEDAD_MINIMA);

        limpieza.limpiar();

        Instant despues = Instant.now().minus(
            LimpiezaArchivos.ANTIGUEDAD_MINIMA
        );
        ArgumentCaptor<Instant> limite = ArgumentCaptor.forClass(Instant.class);
        verify(limpieza.archivoRepositorio).huerfanos(limite.capture());
        assertFalse(limite.getValue().isBefore(antes));
        assertFalse(limite.getValue().isAfter(despues));
    }

    @Test
    void unErrorNoFrenaLaLimpiezaDeLosDemas() {
        UUID falla = UUID.randomUUID();
        UUID siguiente = UUID.randomUUID();
        when(limpieza.archivoRepositorio.huerfanos(any())).thenReturn(
            List.of(falla, siguiente)
        );
        when(limpieza.servicioArchivos.eliminarHuerfano(falla)).thenThrow(
            new IllegalStateException("falla")
        );
        when(limpieza.servicioArchivos.eliminarHuerfano(siguiente)).thenReturn(
            true
        );

        limpieza.limpiar();

        verify(limpieza.servicioArchivos).eliminarHuerfano(siguiente);
    }
}
