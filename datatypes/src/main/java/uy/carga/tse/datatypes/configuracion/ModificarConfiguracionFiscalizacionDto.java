package uy.carga.tse.datatypes.configuracion;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import java.math.BigDecimal;

/** Reemplaza todos los parametros: no hay actualizaciones parciales. */
public record ModificarConfiguracionFiscalizacionDto(
    @NotNull
    @PositiveOrZero
    @DecimalMax("100")
    @Digits(integer = 3, fraction = 3)
    BigDecimal toleranciaExcesoCarga,
    @NotNull
    @PositiveOrZero
    @Digits(integer = 8, fraction = 2)
    BigDecimal toleranciaDesvioRutaMetros,
    @NotNull
    @PositiveOrZero
    @DecimalMax("100")
    @Digits(integer = 3, fraction = 3)
    BigDecimal toleranciaDiferenciaPesoBalanzas,
    @NotNull @Positive @Max(8760) Long periodoDescargoHoras,
    @NotNull @Positive @Max(8760) Long periodoMaximoViajeHoras,
    @NotNull @Positive @Max(1000) Integer cantidadInfraccionesRecurrentes,
    @NotNull @Positive @Max(3650) Long periodoInfraccionRecurrenteDias
) {}
