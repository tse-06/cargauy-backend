package uy.carga.tse.logica.comun;

/**
 * Formato unico de la cedula en la plataforma: solo digitos, asi
 * "1.234.567-8", " 12345678 " y "12345678" son la misma persona. Se aplica en
 * cada punto de entrada (altas, login con contrasenia, id_token de gub.uy, PDI)
 * para que las busquedas por cedula no dependan del formato.
 */
public final class Cedula {

    private Cedula() {}

    /** Los digitos de la cedula; null si es null. */
    public static String normalizar(String ci) {
        return ci == null ? null : ci.replaceAll("\\D", "");
    }
}
