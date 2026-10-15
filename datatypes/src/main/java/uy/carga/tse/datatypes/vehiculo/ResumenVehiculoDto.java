package uy.carga.tse.datatypes.vehiculo;

import java.util.UUID;

public record ResumenVehiculoDto(
    UUID id,
    String matricula,
    String marca,
    String modelo,
    UUID empresaId
) {}
