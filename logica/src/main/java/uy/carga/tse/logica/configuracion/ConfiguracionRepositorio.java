package uy.carga.tse.logica.configuracion;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import uy.carga.tse.entidades.ConfiguracionSistema;

@Transactional
@Repository
public interface ConfiguracionRepositorio
    extends CrudRepository<ConfiguracionSistema, UUID> {}
