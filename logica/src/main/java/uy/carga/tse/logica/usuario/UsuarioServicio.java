package uy.carga.tse.logica.usuario;

import jakarta.annotation.Resource;
import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.usuario.CrearUsuarioDto;
import uy.carga.tse.datatypes.usuario.DetalleUsuarioDto;
import uy.carga.tse.datatypes.usuario.ModificarUsuarioDto;
import uy.carga.tse.datatypes.usuario.ResumenUsuarioDto;
import uy.carga.tse.entidades.Administrador;
import uy.carga.tse.entidades.Autoridad;
import uy.carga.tse.entidades.Chofer;
import uy.carga.tse.entidades.EncargadoEmpresa;
import uy.carga.tse.entidades.Funcionario;
import uy.carga.tse.entidades.Usuario;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.autenticacion.HashContrasenia;
import uy.carga.tse.logica.autenticacion.RolesUsuario;
import uy.carga.tse.logica.comun.Persistencia;
import uy.carga.tse.logica.comun.excepciones.ConflictoException;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

@Stateless
public class UsuarioServicio implements UsuarioServicioLocal {

    @Inject
    UsuarioRepositorio usuarioRepositorio;

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

    @Inject
    HashContrasenia hashContrasenia;

    @Inject
    RolesUsuario roles;

    @Inject
    UsuarioMapper usuarioMapper;

    @Resource
    SessionContext contexto;

    Clock reloj = Clock.systemUTC();

    @Override
    public DetalleUsuarioDto crearUsuarioInterno(CrearUsuarioDto dto) {
        Set<Rol> rolesPedidos = usuarioMapper.aRoles(dto);
        boolean requiereContrasenia =
            rolesPedidos.contains(Rol.ADMINISTRADOR) ||
            rolesPedidos.contains(Rol.AUTORIDAD);
        boolean hayContrasenia =
            dto.contrasenia() != null && !dto.contrasenia().isBlank();
        if (requiereContrasenia && !hayContrasenia) {
            throw new ValidacionNegocioException(
                CodigoError.CONTRASENIA_REQUERIDA,
                "Los roles ADMINISTRADOR y AUTORIDAD requieren contrasenia"
            );
        }
        if (!requiereContrasenia && hayContrasenia) {
            throw new ValidacionNegocioException(
                CodigoError.CONTRASENIA_NO_PERMITIDA,
                "Solo los roles ADMINISTRADOR y AUTORIDAD llevan contrasenia"
            );
        }
        Usuario usuario = usuarioMapper.aEntidad(dto);
        if (requiereContrasenia) {
            usuario.setContrasenia(
                hashContrasenia.hashear(dto.contrasenia().toCharArray())
            );
        }
        Usuario creado = insertar(usuario);
        rolesPedidos.forEach(rol -> asignarRol(creado, rol));
        return usuarioMapper.aDetalle(creado, rolesPedidos);
    }

    @Override
    public UUID registrarCiudadano(NuevoUsuario datos) {
        // sin contrasenia: se autentica solo con gub.uy (migracion V3)
        return insertar(usuarioMapper.aEntidad(datos)).getId();
    }

    @Override
    public DetalleUsuarioDto modificarUsuario(
        UUID id,
        ModificarUsuarioDto dto
    ) {
        Usuario usuario = buscar(id);
        // Sin persistence context (StatelessSession): cambiarlo antes de
        // validar no llega a la base si la validacion falla
        usuarioMapper.actualizar(dto, usuario);
        verificarUnicos(usuario.getCi(), usuario.getEmail(), id);
        return detalle(usuarioRepositorio.update(usuario));
    }

    @Override
    public DetalleUsuarioDto darDeBaja(UUID id) {
        if (id.toString().equals(contexto.getCallerPrincipal().getName())) {
            throw new ValidacionNegocioException(
                CodigoError.AUTO_BAJA_NO_PERMITIDA,
                "Un administrador no puede darse de baja a si mismo"
            );
        }
        Usuario usuario = buscar(id);
        if (!usuario.estaDeBaja()) {
            usuario.setFechaBaja(Instant.now(reloj));
            usuario = usuarioRepositorio.update(usuario);
        }
        return detalle(usuario);
    }

    @Override
    public DetalleUsuarioDto darDeAlta(UUID id) {
        Usuario usuario = buscar(id);
        if (usuario.estaDeBaja()) {
            usuario.setFechaBaja(null);
            usuario = usuarioRepositorio.update(usuario);
        }
        return detalle(usuario);
    }

    @Override
    public DetalleUsuarioDto obtener(UUID id)
        throws RecursoNoEncontradoException {
        return detalle(buscar(id));
    }

    /** Listado paginado. La pagina empieza en 1. */
    @Override
    public Page<ResumenUsuarioDto> listar(
        PageRequest pageRequest,
        List<Sort<Object>> sorts
    ) {
        Page<Usuario> pagina = usuarioRepositorio.findAll(
            pageRequest,
            Persistencia.orden(sorts)
        );
        // roles de toda la pagina en una consulta por tipo de rol
        Map<UUID, Set<Rol>> rolesPorUsuario = roles.de(
            pagina.content().stream().map(Usuario::getId).toList()
        );
        return Persistencia.mapear(pagina, u ->
            usuarioMapper.aResumen(u, rolesPorUsuario.get(u.getId()))
        );
    }

    private Usuario insertar(Usuario usuario) {
        verificarUnicos(usuario.getCi(), usuario.getEmail(), null);
        return usuarioRepositorio.insert(usuario);
    }

    /**
     * Rechaza una cedula o un correo que ya usa otro usuario. La constraint
     * unique de la base sigue siendo la garantia ante escrituras concurrentes.
     *
     * @param excluir usuario que se esta modificando; null en un alta
     */
    private void verificarUnicos(String ci, String email, UUID excluir) {
        if (usadoPorOtro(usuarioRepositorio.porCi(ci), excluir)) {
            throw new ConflictoException(
                CodigoError.CI_EN_USO,
                "Ya existe un usuario con esa cedula"
            );
        }
        if (usadoPorOtro(usuarioRepositorio.porEmail(email), excluir)) {
            throw new ConflictoException(
                CodigoError.EMAIL_EN_USO,
                "Ya existe un usuario con ese correo electronico"
            );
        }
    }

    private static boolean usadoPorOtro(
        Optional<Usuario> existente,
        UUID excluir
    ) {
        return existente.filter(u -> !u.getId().equals(excluir)).isPresent();
    }

    private void asignarRol(Usuario usuario, Rol rol) {
        switch (rol) {
            case ADMINISTRADOR -> {
                Administrador r = new Administrador();
                r.setUsuario(usuario);
                administradorRepositorio.insert(r);
            }
            case AUTORIDAD -> {
                Autoridad r = new Autoridad();
                r.setUsuario(usuario);
                autoridadRepositorio.insert(r);
            }
            case CHOFER -> {
                Chofer r = new Chofer();
                r.setUsuario(usuario);
                choferRepositorio.insert(r);
            }
            case ENCARGADO_EMPRESA -> {
                EncargadoEmpresa r = new EncargadoEmpresa();
                r.setUsuario(usuario);
                encargadoEmpresaRepositorio.insert(r);
            }
            case FUNCIONARIO -> {
                Funcionario r = new Funcionario();
                r.setUsuario(usuario);
                funcionarioRepositorio.insert(r);
            }
        }
    }

    private DetalleUsuarioDto detalle(Usuario usuario) {
        return usuarioMapper.aDetalle(usuario, roles.de(usuario));
    }

    private Usuario buscar(UUID id) {
        return usuarioRepositorio
            .findById(id)
            .orElseThrow(() ->
                new RecursoNoEncontradoException(
                    CodigoError.USUARIO_NO_ENCONTRADO,
                    "Usuario no encontrado: " + id
                )
            );
    }
}
