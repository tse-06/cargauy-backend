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
import uy.carga.tse.entidades.Administrador;
import uy.carga.tse.entidades.Usuario;

@Transactional
@Repository
public interface AdministradorRepositorio
    extends CrudRepository<Administrador, UUID>
{
    @Find
    Optional<Administrador> porUsuario(@By("usuario") Usuario usuario);

    /** Ids de los usuarios dados que tienen este rol (una sola consulta). */
    @Query("select r.usuario.id from Administrador r where r.usuario.id in :usuarioIds")
    List<UUID> usuariosConRol(Collection<UUID> usuarioIds);
}
