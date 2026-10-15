package uy.carga.tse.logica.autenticacion;

import jakarta.ejb.Local;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import uy.carga.tse.datatypes.usuario.DetalleSesionDto;
import uy.carga.tse.datatypes.usuario.IniciarSesionDto;
import uy.carga.tse.entidades.enums.Rol;

@Local
public interface AutenticacionServicioLocal {
    /** Usuario registrado y todos sus roles. */
    record Cuenta(UUID id, Set<Rol> roles) {}

    DetalleSesionDto iniciarSesion(IniciarSesionDto dto)
        throws CredencialesInvalidasException;

    /**
     * Datos de la sesion que ya tiene el usuario, sin emitir un token nuevo
     * (el token del resultado es null). Es lo que consulta el frontend al
     * abrirse, porque el token vive en una cookie httpOnly que no puede leer.
     *
     * @param expiraEn vencimiento del token con el que se hizo la llamada
     * @throws RolRevocadoException si el usuario no existe
     */
    DetalleSesionDto detalleSesion(UUID usuarioId, Rol rol, Instant expiraEn)
        throws RolRevocadoException;

    /** Usuario con esa cedula, si esta registrado. */
    Optional<Cuenta> cuentaPorCi(String ci);

    /**
     * Verifica que el usuario todavia tiene el rol con el que se emitio su
     * token.
     *
     * @throws RolRevocadoException si el usuario no existe o ya no tiene el rol
     */
    void exigirRol(UUID usuarioId, Rol rol) throws RolRevocadoException;
}
