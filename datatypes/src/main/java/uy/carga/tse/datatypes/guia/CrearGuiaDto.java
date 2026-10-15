package uy.carga.tse.datatypes.guia;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CrearGuiaDto(
    @NotNull UUID empresaId,
    @NotBlank String rubroCliente,
    @NotBlank String tipoCarga,
    // Peso declarado de la carga, en kg
    @NotNull @Positive @Digits(integer = 9, fraction = 3) BigDecimal pesoCarga,
    @NotNull LocalDate fecha,
    @NotBlank @Size(max = 255) String origen,
    @NotBlank @Size(max = 255) String destino,
    @NotNull @Valid CoordenadaDto ubicacionOrigen,
    @NotNull @Valid CoordenadaDto ubicacionDestino,
    // Opcional
    UUID vehiculoId,
    // Opcional. Id del usuario chofer
    UUID choferId
) {}
