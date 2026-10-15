package uy.carga.tse.logica.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

class CedulaTest {

    @Test
    void dejaSoloLosDigitos() {
        assertEquals("12345678", Cedula.normalizar("1.234.567-8"));
        assertEquals("12345678", Cedula.normalizar(" 12345678 "));
        assertEquals("12345678", Cedula.normalizar("12345678"));
        assertEquals("", Cedula.normalizar("sin digitos"));
        assertNull(Cedula.normalizar(null));
    }
}
