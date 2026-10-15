package uy.carga.tse.datatypes.empresa;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ModificarEmpresaDto(
    @NotBlank @Size(max = 255) String nombre,
    @NotBlank @Size(max = 255) String razonSocial,
    @NotBlank @Size(max = 255) String numero,
    @NotBlank @Size(max = 255) String direccion,
    @NotBlank String rubro
) {}
