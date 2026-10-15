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
import uy.carga.tse.entidades.Chofer;
import uy.carga.tse.entidades.Usuario;

@Transactional
@Repository
public interface ChoferRepositorio extends CrudRepository<Chofer, UUID> {
    @Find
    Optional<Chofer> porUsuario(@By("usuario") Usuario usuario);

    @Query(
        "select r.usuario.id from Chofer r where r.usuario.id in :usuarioIds"
    )
    List<UUID> usuariosConRol(Collection<UUID> usuarioIds);

    /** Chofer del usuario, solo si trabaja para la empresa. */
    @Query(
        "select c from Chofer c join c.empresas emp" +
            " where c.usuario.id = :usuarioId and emp.id = :empresaId"
    )
    Optional<Chofer> deEmpresa(UUID usuarioId, UUID empresaId);
}
