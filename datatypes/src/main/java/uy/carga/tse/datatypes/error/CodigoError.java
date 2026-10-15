package uy.carga.tse.datatypes.error;

/**
 * Motivo de un error de la API, estable para que el frontend lo traduzca a
 * su propio texto. Viaja en {@link ErrorDto#codigo()} y en el fragmento
 * {@code #error=} del login con gub.uy.
 * <p>
 * Es parte del contrato con los clientes: no renombrar ni quitar valores,
 * solo agregar.
 */
public enum CodigoError {
    // Generales
    DATOS_INVALIDOS,
    SOLICITUD_INVALIDA,
    NO_AUTENTICADO,
    OPERACION_NO_PERMITIDA,
    ERROR_INTERNO,

    // Sesion y roles
    CREDENCIALES_INVALIDAS,
    TOKEN_INVALIDO,
    ROL_REVOCADO,
    ROL_INVALIDO,
    ROL_REQUERIDO,
    ROL_NO_ASIGNADO,
    USUARIO_SIN_ROLES,
    USUARIO_DADO_DE_BAJA,
    CSRF_INVALIDO,

    // Recursos inexistentes
    USUARIO_NO_ENCONTRADO,
    EMPRESA_NO_ENCONTRADA,
    VEHICULO_NO_ENCONTRADO,
    ARCHIVO_NO_ENCONTRADO,
    CHOFER_NO_ENCONTRADO,
    CONFIGURACION_NO_ENCONTRADA,

    // Conflictos y permisos sobre los datos
    CI_EN_USO,
    EMAIL_EN_USO,
    EMPRESA_AJENA,
    ENLACE_ARCHIVO_VENCIDO,

    // Validaciones de negocio
    CI_INVALIDA,
    RUBRO_INVALIDO,
    FECHA_INVALIDA,
    PERMISO_NO_PDF,
    ARCHIVO_SIN_CONTENIDO,
    ADJUNTO_INVALIDO,
    AUTO_BAJA_NO_PERMITIDA,
    CONTRASENIA_REQUERIDA,
    CONTRASENIA_NO_PERMITIDA,
    ARCHIVO_NO_PDF,
    TIPO_CARGA_INVALIDO,
    FECHA_PASADA,
    ORIGEN_IGUAL_DESTINO,
    CAPACIDAD_EXCEDIDA,
    ITV_NO_VIGENTE,
    PNC_NO_VIGENTE,

    // Login con gub.uy
    GUBUY_CANCELADO,
    GUBUY_ERROR,
    GUBUY_ESTADO_INVALIDO,
    GUBUY_TOKEN_INVALIDO,
    GUBUY_CODIGO_RECHAZADO,
    GUBUY_NO_DISPONIBLE,
    RETURN_TO_INVALIDO,
    ROL_NO_PERMITIDO_GUBUY,
    EMAIL_REQUERIDO,
    PERSONA_NO_ENCONTRADA,
    MENOR_DE_EDAD,
    PDI_NO_DISPONIBLE,
}
