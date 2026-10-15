package uy.carga.tse.logica.comun.excepciones;

import jakarta.ejb.ApplicationException;
import uy.carga.tse.datatypes.error.CodigoError;

/**
 * Base de las excepciones de negocio lanzadas por los session beans.
 *
 * Al ser {@code @ApplicationException} el contenedor las deja pasar tal cual
 * (si no, las envuelve en EJBException) y hace rollback de la transaccion.
 * Las subclases heredan la anotacion.
 * <p>
 * El {@link CodigoError} identifica el motivo para el cliente; el mensaje es
 * solo para desarrolladores. La subclase define el estado HTTP (ver
 * ExcepcionNegocioMapper en servicios).
 */
@ApplicationException(rollback = true)
public abstract class ExcepcionNegocio extends RuntimeException {

    private final CodigoError codigo;

    protected ExcepcionNegocio(CodigoError codigo, String mensaje) {
        super(mensaje);
        this.codigo = codigo;
    }

    protected ExcepcionNegocio(
        CodigoError codigo,
        String mensaje,
        Throwable causa
    ) {
        super(mensaje, causa);
        this.codigo = codigo;
    }

    public CodigoError codigo() {
        return codigo;
    }
}
