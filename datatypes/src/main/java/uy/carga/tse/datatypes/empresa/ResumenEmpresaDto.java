package uy.carga.tse.datatypes.empresa;

import java.util.UUID;

public record ResumenEmpresaDto(
    UUID id,
    String nombre,
    String numero,
    String rubro
) {}
