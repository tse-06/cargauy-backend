package uy.carga.tse.logica.autenticacion;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.usuario.DetalleSesionDto;
import uy.carga.tse.datatypes.usuario.IniciarSesionDto;
import uy.carga.tse.entidades.Usuario;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.comun.Cedula;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;
import uy.carga.tse.logica.usuario.UsuarioMapper;
import uy.carga.tse.logica.usuario.UsuarioRepositorio;

@Stateless
public class AutenticacionServicio implements AutenticacionServicioLocal {

    @Inject
    UsuarioRepositorio usuarioRepositorio;

    @Inject
    RolesUsuario roles;

    @Inject
    HashContrasenia hashContrasenia;

    @Inject
    ServicioJwt servicioJwt;

    @Inject
    UsuarioMapper usuarioMapper;

    @Override
    public DetalleSesionDto iniciarSesion(IniciarSesionDto dto)
        throws CredencialesInvalidasException {
        Rol pedido = RolesUsuario.parsear(dto.rol());
        Usuario usuario = usuarioRepositorio
            .porCi(Cedula.normalizar(dto.ci()))
            .orElse(null);

        // Se verifica aunque el usuario no exista, para que el tiempo de
        // respuesta no revele si la ci esta registrada
        boolean contraseniaCorrecta = hashContrasenia.verificar(
            dto.contrasenia().toCharArray(),
            usuario == null ? null : usuario.getContrasenia()
        );

        if (usuario == null || !contraseniaCorrecta) {
            throw new CredencialesInvalidasException();
        }

        exigirActivo(usuario);
        Rol rol = RolesUsuario.elegir(roles.de(usuario), pedido);
        return usuarioMapper.aDetalle(
            servicioJwt.emitir(usuario.getId(), rol),
            usuario
        );
    }

    @Override
    public DetalleSesionDto detalleSesion(
        UUID usuarioId,
        Rol rol,
        Instant expiraEn
    ) throws RolRevocadoException {
        Usuario usuario = usuarioRepositorio
            .findById(usuarioId)
            .orElseThrow(() -> new RolRevocadoException(usuarioId, rol));
        return usuarioMapper.aDetalle(new Sesion(null, expiraEn, rol), usuario);
    }

    @Override
    public Optional<Cuenta> cuentaPorCi(String ci) {
        return usuarioRepositorio.porCi(Cedula.normalizar(ci)).map(usuario -> {
            exigirActivo(usuario);
            return new Cuenta(usuario.getId(), roles.de(usuario));
        });
    }

    private static void exigirActivo(Usuario usuario) {
        if (usuario.estaDeBaja()) {
            throw new AccesoDenegadoException(
                CodigoError.USUARIO_DADO_DE_BAJA,
                "El usuario esta dado de baja"
            );
        }
    }

    @Override
    public void exigirRol(UUID usuarioId, Rol rol) throws RolRevocadoException {
        Usuario usuario = usuarioRepositorio.findById(usuarioId).orElse(null);
        if (
            usuario == null ||
            usuario.estaDeBaja() ||
            !roles.tiene(usuario, rol)
        ) {
            throw new RolRevocadoException(usuarioId, rol);
        }
    }
}
