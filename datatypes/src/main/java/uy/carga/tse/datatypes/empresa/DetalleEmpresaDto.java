package uy.carga.tse.datatypes.empresa;

import java.util.UUID;
import uy.carga.tse.datatypes.permiso.DetallePermisoDto;

public record DetalleEmpresaDto(
    UUID id,
    String nombre,
    String razonSocial,
    String numero,
    String direccion,
    String rubro,
    // null si la empresa todavia no cargo su permiso
    DetallePermisoDto permiso
) {}
