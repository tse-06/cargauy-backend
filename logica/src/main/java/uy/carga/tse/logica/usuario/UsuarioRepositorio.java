package uy.carga.tse.logica.usuario;

import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Find;
import jakarta.data.repository.Repository;
import jakarta.transaction.Transactional;
import java.util.Optional;
import java.util.UUID;
import uy.carga.tse.entidades.Usuario;

@Transactional
@Repository
public interface UsuarioRepositorio extends CrudRepository<Usuario, UUID> {
    @Find
    Optional<Usuario> porCi(String ci);

    @Find
    Optional<Usuario> porEmail(String email);
}
