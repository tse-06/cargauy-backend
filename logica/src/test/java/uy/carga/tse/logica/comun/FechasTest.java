package uy.carga.tse.logica.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

class FechasTest {

    @Test
    void parsearFechaIso() {
        assertEquals(
            LocalDate.of(2027, 12, 31),
            Fechas.parsear(" 2027-12-31 ")
        );
    }

    @Test
    void parsearFechaInvalida() {
        assertThrows(ValidacionNegocioException.class, () ->
            Fechas.parsear("31/12/2027")
        );
        assertThrows(ValidacionNegocioException.class, () ->
            Fechas.parsear("2027-02-30")
        );
    }
}
