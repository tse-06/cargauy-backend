package uy.carga.tse.logica.autenticacion.gubuy;

/**
 * Identidad verificada por ID Uruguay.
 *
 * @param subject   claim "sub" (identificador del usuario en ID Uruguay)
 * @param documento numero de documento (clave para buscar al usuario en carga.uy)
 * @param email     correo electronico (puede ser null si no se solicito el scope)
 * @param nivel     nivel de seguridad de la identidad (puede ser null)
 */
public record GubUyIdentity(
    String subject,
    String documento,
    String email,
    String nivel
) {}
