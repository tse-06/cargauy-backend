package uy.carga.tse.logica.empresa;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import uy.carga.tse.entidades.Permiso;

@Transactional
@Repository
public interface PermisoRepositorio extends CrudRepository<Permiso, UUID> {}
