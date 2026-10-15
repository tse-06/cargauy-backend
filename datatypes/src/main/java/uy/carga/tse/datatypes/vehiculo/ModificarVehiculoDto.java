package uy.carga.tse.datatypes.vehiculo;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

public record ModificarVehiculoDto(
    @NotBlank @Size(max = 255) String matricula,
    @NotBlank @Size(max = 255) String marca,
    @NotBlank @Size(max = 255) String modelo,
    @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal peso,
    @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal capacidad
) {}
