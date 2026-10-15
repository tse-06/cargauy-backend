package uy.carga.tse.entidades.enums;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class RolesTest {

    @Test
    void cadaRolTieneSuConstanteConElMismoNombre() throws Exception {
        Set<String> constantes = Arrays.stream(Roles.class.getDeclaredFields())
            .filter(f -> f.getType() == String.class)
            .map(RolesTest::valor)
            .collect(Collectors.toSet());
        Set<String> nombres = Arrays.stream(Rol.values())
            .map(Rol::name)
            .collect(Collectors.toSet());
        assertEquals(nombres, constantes);
    }

    private static String valor(Field campo) {
        try {
            assertEquals(campo.getName(), campo.get(null));
            return (String) campo.get(null);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
