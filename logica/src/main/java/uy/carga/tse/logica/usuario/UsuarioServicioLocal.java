package uy.carga.tse.logica.usuario;

import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.ejb.Local;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import uy.carga.tse.datatypes.usuario.CrearUsuarioDto;
import uy.carga.tse.datatypes.usuario.DetalleUsuarioDto;
import uy.carga.tse.datatypes.usuario.ModificarUsuarioDto;
import uy.carga.tse.datatypes.usuario.ResumenUsuarioDto;
import uy.carga.tse.logica.comun.excepciones.ConflictoException;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

@Local
public interface UsuarioServicioLocal {
    /** Datos de un ciudadano que entra por primera vez con gub.uy. */
    record NuevoUsuario(
        String ci,
        String nombre,
        String apellido,
        String email,
        LocalDate fechaNacimiento
    ) {}

    /**
     * @throws ConflictoException si la cedula o el correo ya estan registrados
     */
    DetalleUsuarioDto crearUsuarioInterno(CrearUsuarioDto dto);

    /**
     * Registra a un ciudadano sin contrasenia y sin roles: los roles los
     * asignan los funcionarios (empresas, choferes) y los administradores.
     *
     * @return id del usuario creado
     * @throws ConflictoException con CI_EN_USO o EMAIL_EN_USO si la cedula o
     *         el correo ya estan registrados
     */
    UUID registrarCiudadano(NuevoUsuario datos);

    /**
     * @throws ConflictoException si la cedula o el correo los usa otro usuario
     */
    DetalleUsuarioDto modificarUsuario(UUID id, ModificarUsuarioDto dto);

    /**
     * Baja logica: el usuario ya no puede iniciar sesion ni usar sus tokens.
     * Es idempotente: si ya estaba de baja conserva la fecha original.
     *
     * @throws ValidacionNegocioException con AUTO_BAJA_NO_PERMITIDA si un
     *         administrador intenta darse de baja a si mismo
     */
    DetalleUsuarioDto darDeBaja(UUID id);

    /** Revierte la baja logica. Es idempotente. */
    DetalleUsuarioDto darDeAlta(UUID id);

    DetalleUsuarioDto obtener(UUID id) throws RecursoNoEncontradoException;

    /** Listado paginado. La pagina empieza en 1. */
    Page<ResumenUsuarioDto> listar(
        PageRequest pageRequest,
        List<Sort<Object>> sorts
    );
}
