package uy.carga.tse.logica.vehiculo;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import uy.carga.tse.entidades.PermisoNacionalDeCirculacion;

@Transactional
@Repository
public interface PncRepositorio
    extends CrudRepository<PermisoNacionalDeCirculacion, UUID> {}
