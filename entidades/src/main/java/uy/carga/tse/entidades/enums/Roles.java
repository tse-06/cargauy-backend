package uy.carga.tse.entidades.enums;

/**
 * Nombres de {@link Rol} como constantes, para usarlos en
 * {@code @RolesAllowed}, que solo acepta constantes de compilacion.
 */
public final class Roles {

    public static final String ADMINISTRADOR = "ADMINISTRADOR";
    public static final String AUTORIDAD = "AUTORIDAD";
    public static final String CHOFER = "CHOFER";
    public static final String ENCARGADO_EMPRESA = "ENCARGADO_EMPRESA";
    public static final String FUNCIONARIO = "FUNCIONARIO";

    private Roles() {}
}
