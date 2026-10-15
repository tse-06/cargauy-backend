package uy.carga.tse.datatypes.vehiculo;

import java.math.BigDecimal;
import java.util.UUID;

public record DetalleVehiculoDto(
    UUID id,
    String matricula,
    String marca,
    String modelo,
    BigDecimal peso,
    BigDecimal capacidad,
    UUID empresaId,
    // null si el vehiculo todavia no cargo su ITV
    DetalleItvDto itv,
    // null si el vehiculo todavia no cargo su PNC
    DetallePncDto pnc
) {}
