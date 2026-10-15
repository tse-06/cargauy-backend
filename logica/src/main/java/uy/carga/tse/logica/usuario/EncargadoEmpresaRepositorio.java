package uy.carga.tse.logica.usuario;

import jakarta.data.repository.By;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.EncargadoEmpresa;
import uy.carga.tse.entidades.Usuario;

@Transactional
@Repository
public interface EncargadoEmpresaRepositorio
    extends CrudRepository<EncargadoEmpresa, UUID>
{
    @Find
    Optional<EncargadoEmpresa> porUsuario(@By("usuario") Usuario usuario);

    /** Si el usuario es encargado de la empresa. */
    @Query(
        "select count(e) > 0 from EncargadoEmpresa e join e.empresas emp" +
            " where e.usuario.id = :usuarioId and emp.id = :empresaId"
    )
    boolean gestiona(UUID usuarioId, UUID empresaId);

    @Query(
        "select r.usuario.id from EncargadoEmpresa r where r.usuario.id in :usuarioIds"
    )
    List<UUID> usuariosConRol(Collection<UUID> usuarioIds);

    /** Empresas que gestiona el usuario, ordenadas por nombre. */
    @Query(
        "select emp from EncargadoEmpresa e join e.empresas emp" +
            " left join fetch emp.permiso" +
            " where e.usuario.id = :usuarioId order by emp.nombre"
    )
    List<Empresa> empresasDe(UUID usuarioId);
}
