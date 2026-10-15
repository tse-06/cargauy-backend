package uy.carga.tse.logica.archivos;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import uy.carga.tse.entidades.Archivo;

@Transactional
@Repository
public interface ArchivoRepositorio extends CrudRepository<Archivo, UUID> {
    /** Ids de los archivos creados antes de {@code limite} que no estan adjuntos a nada. */
    @Query(
        "select a.id from Archivo a where a.creado < :limite" +
            " and not exists (select 1 from ArchivoAdjunto aa where aa.archivo = a)"
    )
    List<UUID> huerfanos(Instant limite);
}
