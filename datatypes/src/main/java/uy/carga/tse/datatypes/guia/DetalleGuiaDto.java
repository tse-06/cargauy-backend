package uy.carga.tse.datatypes.guia;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DetalleGuiaDto(
    UUID id,
    UUID empresaId,
    String rubroCliente,
    String tipoCarga,
    BigDecimal pesoCarga,
    LocalDate fecha,
    String origen,
    String destino,
    CoordenadaDto ubicacionOrigen,
    CoordenadaDto ubicacionDestino,
    String estado,
    // null si la guia no tiene vehiculo asignado
    UUID vehiculoId,
    // Id del usuario chofer; null si la guia no tiene chofer asignado
    UUID choferId,
    Instant creado
) {}
