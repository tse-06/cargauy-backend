package uy.carga.tse.logica.usuario;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.usuario.CrearUsuarioDto;
import uy.carga.tse.datatypes.usuario.DetalleSesionDto;
import uy.carga.tse.datatypes.usuario.DetalleUsuarioDto;
import uy.carga.tse.datatypes.usuario.ModificarUsuarioDto;
import uy.carga.tse.datatypes.usuario.ResumenUsuarioDto;
import uy.carga.tse.entidades.Usuario;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.autenticacion.Sesion;
import uy.carga.tse.logica.comun.Cedula;
import uy.carga.tse.logica.comun.ConfiguracionMappers;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;
import uy.carga.tse.logica.usuario.UsuarioServicioLocal.NuevoUsuario;

/** Los roles estan en otras tablas: se buscan aparte y se pasan como fuente. */
@Mapper(config = ConfiguracionMappers.class)
public abstract class UsuarioMapper {

    /** Sin contrasenia: la asigna el servicio. */
    @Mapping(target = "ci", qualifiedByName = "cedula")
    @Mapping(target = "email", qualifiedByName = "email")
    @Mapping(target = "contrasenia", ignore = true)
    @Mapping(target = "fechaBaja", ignore = true)
    public abstract Usuario aEntidad(CrearUsuarioDto dto);

    @Mapping(target = "ci", qualifiedByName = "cedula")
    @Mapping(target = "email", qualifiedByName = "email")
    @Mapping(target = "contrasenia", ignore = true)
    @Mapping(target = "fechaBaja", ignore = true)
    public abstract Usuario aEntidad(NuevoUsuario datos);

    @Mapping(target = "ci", qualifiedByName = "cedula")
    @Mapping(target = "email", qualifiedByName = "email")
    @Mapping(target = "contrasenia", ignore = true)
    @Mapping(target = "fechaBaja", ignore = true)
    public abstract void actualizar(
        ModificarUsuarioDto dto,
        @MappingTarget Usuario usuario
    );

    @Mapping(target = "roles", qualifiedByName = "roles")
    public abstract ResumenUsuarioDto aResumen(Usuario usuario, Set<Rol> roles);

    @Mapping(target = "roles", qualifiedByName = "roles")
    public abstract DetalleUsuarioDto aDetalle(Usuario usuario, Set<Rol> roles);

    @Mapping(target = "tipo", constant = "Bearer")
    public abstract DetalleSesionDto aDetalle(Sesion sesion, Usuario usuario);

    public Set<Rol> aRoles(CrearUsuarioDto dto) {
        Set<Rol> roles = EnumSet.noneOf(Rol.class);
        if (dto.roles() == null) {
            return roles;
        }
        for (String valor : dto.roles()) {
            String nombre = valor.trim().toUpperCase();
            if (nombre.equals("NINGUNO")) {
                continue;
            }
            try {
                roles.add(Rol.valueOf(nombre));
            } catch (IllegalArgumentException e) {
                throw new ValidacionNegocioException(
                    CodigoError.ROL_INVALIDO,
                    "Rol invalido: " +
                        valor +
                        ". Valores posibles: " +
                        Arrays.toString(Rol.values())
                );
            }
        }
        return roles;
    }

    @Named("roles")
    protected List<String> roles(Set<Rol> roles) {
        return roles.stream().sorted().map(Rol::name).toList();
    }

    @Named("cedula")
    protected String cedula(String ci) {
        String cedula = Cedula.normalizar(ci);
        if (cedula.isEmpty()) {
            throw new ValidacionNegocioException(
                CodigoError.CI_INVALIDA,
                "La cedula no tiene digitos: " + ci
            );
        }
        return cedula;
    }

    @Named("email")
    protected String email(String email) {
        return email.trim();
    }
}
