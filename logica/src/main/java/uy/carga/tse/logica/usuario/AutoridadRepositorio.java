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
import uy.carga.tse.entidades.Autoridad;
import uy.carga.tse.entidades.Usuario;

@Transactional
@Repository
public interface AutoridadRepositorio extends CrudRepository<Autoridad, UUID> {
    @Find
    Optional<Autoridad> porUsuario(@By("usuario") Usuario usuario);

    @Query(
        "select r.usuario.id from Autoridad r where r.usuario.id in :usuarioIds"
    )
    List<UUID> usuariosConRol(Collection<UUID> usuarioIds);
}
