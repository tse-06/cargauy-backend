package uy.carga.tse.logica.archivos;

import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.ArchivoAdjunto;

@Transactional
@Repository
public interface ArchivoAdjuntoRepositorio
    extends CrudRepository<ArchivoAdjunto, UUID>
{
    /** Archivos de una relacion de la entidad, en orden de alta. */
    @Query(
        "select a.archivo from ArchivoAdjunto a" +
            " where a.tipoEntidad = :tipoEntidad and a.entidadId = :entidadId" +
            " and a.nombre = :nombre order by a.creado, a.id"
    )
    List<Archivo> archivos(String tipoEntidad, UUID entidadId, String nombre);

    /**
     * Vinculos de una relacion de varias entidades, con su archivo, en orden
     * de alta. Los ids son UUID, asi que no hace falta filtrar por tipo.
     */
    @Query(
        "from ArchivoAdjunto a join fetch a.archivo" +
            " where a.entidadId in :entidadIds and a.nombre = :nombre" +
            " order by a.creado, a.id"
    )
    List<ArchivoAdjunto> deEntidades(List<UUID> entidadIds, String nombre);

    @Find
    List<ArchivoAdjunto> deRelacion(
        String tipoEntidad,
        UUID entidadId,
        String nombre
    );

    @Find
    List<ArchivoAdjunto> deEntidad(String tipoEntidad, UUID entidadId);

    @Find
    Optional<ArchivoAdjunto> vinculo(
        String tipoEntidad,
        UUID entidadId,
        String nombre,
        @By("archivo_id") UUID archivoId
    );

    /** Cantidad de adjuntos que apuntan al archivo. */
    @Query(
        "select count(a) from ArchivoAdjunto a where a.archivo.id = :archivoId"
    )
    long referencias(UUID archivoId);
}
