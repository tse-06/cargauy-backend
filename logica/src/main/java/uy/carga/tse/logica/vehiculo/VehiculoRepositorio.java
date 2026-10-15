package uy.carga.tse.logica.vehiculo;

import jakarta.data.Order;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.util.UUID;
import uy.carga.tse.entidades.Vehiculo;

/**
 * ITV y PNC son @OneToOne inversos: se cargan siempre. findById los trae con
 * join; las consultas tienen que traerlos con join fetch para no hacer una
 * consulta extra por vehiculo.
 */
@Transactional
@Repository
public interface VehiculoRepositorio extends CrudRepository<Vehiculo, UUID> {
    @Query(
        "from Vehiculo v" +
            " left join fetch v.inspeccionTecnicaVehicular" +
            " left join fetch v.permisoNacionalDeCirculacion"
    )
    Page<Vehiculo> todos(PageRequest pageRequest, Order<Vehiculo> orden);

    @Query(
        "from Vehiculo v" +
            " left join fetch v.inspeccionTecnicaVehicular" +
            " left join fetch v.permisoNacionalDeCirculacion" +
            " where v.empresa.id = :empresaId"
    )
    Page<Vehiculo> porEmpresa(
        UUID empresaId,
        PageRequest pageRequest,
        Order<Vehiculo> orden
    );
}
