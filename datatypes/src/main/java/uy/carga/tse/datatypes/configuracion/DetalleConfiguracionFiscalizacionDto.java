package uy.carga.tse.datatypes.configuracion;

import java.math.BigDecimal;

/**
 * Parametros de la fiscalizacion automatica vigentes. Los porcentajes van de
 * 0 a 100 y los periodos en la unidad que indica el nombre del campo.
 */
public record DetalleConfiguracionFiscalizacionDto(
    BigDecimal toleranciaExcesoCarga,
    BigDecimal toleranciaDesvioRutaMetros,
    BigDecimal toleranciaDiferenciaPesoBalanzas,
    long periodoDescargoHoras,
    long periodoMaximoViajeHoras,
    int cantidadInfraccionesRecurrentes,
    long periodoInfraccionRecurrenteDias
) {}
