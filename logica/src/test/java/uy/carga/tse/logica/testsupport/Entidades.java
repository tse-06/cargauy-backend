package uy.carga.tse.logica.testsupport;

import java.lang.reflect.Field;
import java.util.UUID;
import uy.carga.tse.entidades.EntidadBase;

/** Utilidades para armar entidades en las pruebas sin base de datos. */
public final class Entidades {

    private Entidades() {}

    /** El id lo genera Hibernate y no tiene setter. */
    public static <T extends EntidadBase> T conId(T entidad) {
        try {
            Field id = EntidadBase.class.getDeclaredField("id");
            id.setAccessible(true);
            id.set(entidad, UUID.randomUUID());
            return entidad;
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
