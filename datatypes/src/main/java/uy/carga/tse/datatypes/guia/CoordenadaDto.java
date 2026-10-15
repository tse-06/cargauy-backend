package uy.carga.tse.datatypes.guia;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/** Punto geografico en grados decimales (WGS 84). */
public record CoordenadaDto(
    @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
    @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud
) {}
