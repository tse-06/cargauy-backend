package uy.carga.tse.logica.archivos;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.hibernate.Hibernate;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.ArchivoAdjunto;
import uy.carga.tse.entidades.EntidadBase;

/**
 * Archivos de varias entidades, ya cargados y con su URL firmada, para que los
 * mappers los usen como {@code @Context}. Los archivos se vinculan sin FK y no
 * se pueden traer con un join: se obtienen con
 * {@link ArchivoServicioLocal#adjuntos} en una sola consulta.
 */
public final class Adjuntos {

    private static final Adjuntos VACIO = new Adjuntos(Map.of());

    private record Clave(String tipoEntidad, UUID entidadId, String nombre) {}

    private record Entrada(String nombreArchivo, String url) {}

    private final Map<Clave, Entrada> entradas;

    private Adjuntos(Map<Clave, Entrada> entradas) {
        this.entradas = entradas;
    }

    /** Sin archivos (ej. una entidad recien creada). */
    public static Adjuntos vacio() {
        return VACIO;
    }

    /**
     * Copia con un archivo ya conocido (ej. el que se acaba de subir) como el
     * de la relacion {@code nombre} de la entidad.
     */
    public Adjuntos con(
        EntidadBase entidad,
        String nombre,
        Archivo archivo,
        String url
    ) {
        Map<Clave, Entrada> copia = new HashMap<>(entradas);
        copia.put(
            clave(entidad, nombre),
            new Entrada(archivo.getNombre(), url)
        );
        return new Adjuntos(copia);
    }

    /**
     * A partir de los vinculos en orden de alta. Para cada relacion queda el
     * primero, como en {@link ArchivoServicioLocal#obtener}.
     */
    static Adjuntos de(
        List<ArchivoAdjunto> vinculos,
        Function<Archivo, String> url
    ) {
        Map<Clave, Entrada> entradas = new HashMap<>();
        for (ArchivoAdjunto a : vinculos) {
            entradas.computeIfAbsent(
                new Clave(a.getTipoEntidad(), a.getEntidadId(), a.getNombre()),
                c ->
                    new Entrada(
                        a.getArchivo().getNombre(),
                        url.apply(a.getArchivo())
                    )
            );
        }
        return new Adjuntos(entradas);
    }

    /** Nombre del archivo de la relacion; null si no tiene. */
    public String nombreArchivo(EntidadBase entidad, String nombre) {
        Entrada e = buscar(entidad, nombre);
        return e == null ? null : e.nombreArchivo();
    }

    /** URL firmada del archivo de la relacion; null si no tiene. */
    public String url(EntidadBase entidad, String nombre) {
        Entrada e = buscar(entidad, nombre);
        return e == null ? null : e.url();
    }

    private Entrada buscar(EntidadBase entidad, String nombre) {
        return entidad == null ? null : entradas.get(clave(entidad, nombre));
    }

    private static Clave clave(EntidadBase entidad, String nombre) {
        return new Clave(
            Hibernate.getClass(entidad).getSimpleName(),
            entidad.getId(),
            nombre
        );
    }
}
