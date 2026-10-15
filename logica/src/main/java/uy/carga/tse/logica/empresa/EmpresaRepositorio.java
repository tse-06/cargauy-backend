package uy.carga.tse.logica.empresa;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import uy.carga.tse.entidades.Empresa;

/**
 * El permiso es un @OneToOne inverso: se carga siempre. findById lo trae con
 * join; las consultas tienen que traerlo con join fetch para no hacer una
 * consulta extra por empresa.
 */
@Transactional
@Repository
public interface EmpresaRepositorio extends CrudRepository<Empresa, UUID> {
    @Query("from Empresa e left join fetch e.permiso")
    Page<Empresa> todas(PageRequest pageRequest, Order<Empresa> orden);
}
