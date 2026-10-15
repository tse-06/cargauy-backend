package uy.carga.tse.entidades.enums;

/**
 * Roles que puede tener un usuario (uno por subclase de RolUsuario). Una
 * sesion tiene exactamente un rol activo. Los nombres coinciden con las
 * constantes de {@link Roles}.
 */
public enum Rol {
    ADMINISTRADOR,
    AUTORIDAD,
    CHOFER,
    ENCARGADO_EMPRESA,
    FUNCIONARIO,
}
