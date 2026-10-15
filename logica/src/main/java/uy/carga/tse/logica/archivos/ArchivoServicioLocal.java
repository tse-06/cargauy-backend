package uy.carga.tse.logica.archivos;

import jakarta.ejb.Local;
import java.io.InputStream;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.EntidadBase;

/**
 * Adjunta archivos a cualquier entidad. Cada entidad puede tener varias
 * relaciones con archivos, identificadas por nombre (ej.
 * {@code Permiso.ADJUNTO_ARCHIVO}). La entidad ya tiene que estar persistida.
 */
@Local
public interface ArchivoServicioLocal {
    /**
     * Archivo recibido (ej. una parte de un multipart). El tipo MIME no se
     * recibe: se detecta a partir del contenido.
     */
    record ArchivoSubido(String nombre, InputStream contenido) {}

    /**
     * Archivo listo para descargar. {@code seMuestraEnNavegador} indica si el
     * navegador puede mostrarlo (si no, se descarga). Quien lo recibe tiene
     * que cerrar el contenido.
     */
    record ArchivoDescarga(
        String nombre,
        String tipoMime,
        long tamano,
        boolean seMuestraEnNavegador,
        InputStream contenido
    ) {}

    /** Guarda el archivo y lo agrega a la relacion {@code nombre} de la entidad. */
    Archivo adjuntar(EntidadBase entidad, String nombre, ArchivoSubido archivo);

    /**
     * Igual que {@link #adjuntar}, pero antes elimina los archivos que ya tenia
     * la relacion. Para relaciones de un solo archivo.
     */
    Archivo reemplazar(
        EntidadBase entidad,
        String nombre,
        ArchivoSubido archivo
    );

    /** Archivos de la relacion {@code nombre} de la entidad, en orden de alta. */
    List<Archivo> listar(EntidadBase entidad, String nombre);

    /** Primer archivo de la relacion. Para relaciones de un solo archivo. */
    Optional<Archivo> obtener(EntidadBase entidad, String nombre);

    /**
     * Archivos de la relacion {@code nombre} de varias entidades, con su URL
     * firmada, en una sola consulta. Las entidades null se ignoran. Para
     * pasarle a los mappers como {@code @Context}.
     */
    Adjuntos adjuntos(String nombre, EntidadBase... entidades);

    /** Contenido en disco del archivo. Quien lo llama tiene que cerrarlo. */
    InputStream abrir(Archivo archivo);

    /**
     * URL absoluta de descarga del archivo. Lleva el id firmado y vence segun
     * {@code ARCHIVOS_URL_VIGENCIA_MINUTOS}, asi que no hay que guardarla.
     */
    String url(Archivo archivo);

    /**
     * Archivo de un id firmado por {@link #url}. Un id invalido da 404 y uno
     * vencido 403 ({@code ENLACE_ARCHIVO_VENCIDO}).
     */
    ArchivoDescarga abrirFirmado(String idFirmado);

    /** Quita el vinculo; el Archivo y su contenido se conservan. */
    void quitar(EntidadBase entidad, String nombre, UUID archivoId);

    /**
     * Quita el vinculo y, si el Archivo ya no esta adjunto a ninguna entidad,
     * lo borra de la base y (al confirmarse la transaccion) del disco.
     */
    void eliminar(EntidadBase entidad, String nombre, UUID archivoId);

    /**
     * Elimina todos los archivos de la entidad. Hay que llamarlo al borrar la
     * entidad: la asociacion es polimorfica y no hay FK que haga cascade.
     */
    void eliminarTodos(EntidadBase entidad);

    /**
     * Borra el Archivo de la base y (al confirmarse la transaccion) del disco,
     * si no esta adjunto a ninguna entidad. Devuelve si lo borro.
     */
    boolean eliminarHuerfano(UUID archivoId);
}
