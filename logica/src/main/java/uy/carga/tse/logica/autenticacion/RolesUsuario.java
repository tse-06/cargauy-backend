package uy.carga.tse.logica.autenticacion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import uy.carga.tse.entidades.Usuario;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.usuario.AdministradorRepositorio;
import uy.carga.tse.logica.usuario.AutoridadRepositorio;
import uy.carga.tse.logica.usuario.ChoferRepositorio;
import uy.carga.tse.logica.usuario.EncargadoEmpresaRepositorio;
import uy.carga.tse.logica.usuario.FuncionarioRepositorio;

/**
 * Roles de un usuario y eleccion del rol activo de una sesion. Lo comparten
 * el inicio de sesion con contrasenia y el de gub.uy.
 */
@ApplicationScoped
public class RolesUsuario {

    @Inject
    AdministradorRepositorio administradorRepositorio;

    @Inject
    AutoridadRepositorio autoridadRepositorio;

    @Inject
    ChoferRepositorio choferRepositorio;

    @Inject
    EncargadoEmpresaRepositorio encargadoEmpresaRepositorio;

    @Inject
    FuncionarioRepositorio funcionarioRepositorio;

    public Set<Rol> de(Usuario usuario) {
        Set<Rol> roles = EnumSet.noneOf(Rol.class);
        for (Rol rol : Rol.values()) {
            if (tiene(usuario, rol)) {
                roles.add(rol);
            }
        }
        return roles;
    }

    public Map<UUID, Set<Rol>> de(Collection<UUID> usuarioIds) {
        Map<UUID, Set<Rol>> porUsuario = new HashMap<>();
        if (usuarioIds.isEmpty()) {
            return porUsuario;
        }
        for (UUID id : usuarioIds) {
            porUsuario.put(id, EnumSet.noneOf(Rol.class));
        }
        for (Rol rol : Rol.values()) {
            for (UUID id : usuariosConRol(rol, usuarioIds)) {
                porUsuario.get(id).add(rol);
            }
        }
        return porUsuario;
    }

    private List<UUID> usuariosConRol(Rol rol, Collection<UUID> ids) {
        return switch (rol) {
            case ADMINISTRADOR -> administradorRepositorio.usuariosConRol(ids);
            case AUTORIDAD -> autoridadRepositorio.usuariosConRol(ids);
            case CHOFER -> choferRepositorio.usuariosConRol(ids);
            case ENCARGADO_EMPRESA -> encargadoEmpresaRepositorio.usuariosConRol(
                ids
            );
            case FUNCIONARIO -> funcionarioRepositorio.usuariosConRol(ids);
        };
    }

    public boolean tiene(Usuario usuario, Rol rol) {
        return switch (rol) {
            case ADMINISTRADOR -> administradorRepositorio
                .porUsuario(usuario)
                .isPresent();
            case AUTORIDAD -> autoridadRepositorio
                .porUsuario(usuario)
                .isPresent();
            case CHOFER -> choferRepositorio.porUsuario(usuario).isPresent();
            case ENCARGADO_EMPRESA -> encargadoEmpresaRepositorio
                .porUsuario(usuario)
                .isPresent();
            case FUNCIONARIO -> funcionarioRepositorio
                .porUsuario(usuario)
                .isPresent();
        };
    }

    /**
     * Rol activo de la sesion entre los habilitados.
     *
     * @param pedido rol con el que se quiere entrar; null si no se indico,
     *               valido solo si hay un unico rol habilitado
     */
    public static Rol elegir(Set<Rol> habilitados, Rol pedido) {
        if (habilitados.isEmpty()) {
            throw new UsuarioSinRolesException();
        }
        if (pedido == null) {
            if (habilitados.size() > 1) {
                throw new RolRequeridoException(habilitados);
            }
            return habilitados.iterator().next();
        }
        if (!habilitados.contains(pedido)) {
            throw new RolNoAsignadoException(pedido);
        }
        return pedido;
    }

    /** Rol escrito por el cliente; null si no lo indico. */
    public static Rol parsear(String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        try {
            return Rol.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new RolInvalidoException(valor);
        }
    }
}
