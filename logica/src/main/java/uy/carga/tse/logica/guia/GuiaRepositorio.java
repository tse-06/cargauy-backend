package uy.carga.tse.logica.guia;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import uy.carga.tse.entidades.Guia;

@Transactional
@Repository
public interface GuiaRepositorio extends CrudRepository<Guia, UUID> {}
