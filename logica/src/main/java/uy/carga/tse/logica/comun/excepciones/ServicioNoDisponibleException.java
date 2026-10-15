package uy.carga.tse.logica.comun.excepciones;

import uy.carga.tse.datatypes.error.CodigoError;

/**
 * Un servicio externo (gub.uy, la PDI) no responde: timeout, error de red,
 * 5xx o circuit breaker abierto (HTTP 503). Solo se degrada la funcionalidad
 * que depende de ese servicio; el resto de la plataforma sigue funcionando
 * (AC10).
 */
public class ServicioNoDisponibleException extends ExcepcionNegocio {

    public ServicioNoDisponibleException(CodigoError codigo, String mensaje) {
        super(codigo, mensaje);
    }

    public ServicioNoDisponibleException(
        CodigoError codigo,
        String mensaje,
        Throwable causa
    ) {
        super(codigo, mensaje, causa);
    }
}
