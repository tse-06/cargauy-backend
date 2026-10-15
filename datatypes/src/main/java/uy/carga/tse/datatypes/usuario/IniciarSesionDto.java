package uy.carga.tse.datatypes.usuario;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * @param rol rol con el que se inicia la sesion
 */
public record IniciarSesionDto(
    @NotBlank @Size(max = 255) String ci,
    @NotBlank @Size(max = 128) String contrasenia,
    @NotBlank @Size(max = 64) String rol
) {
    @Override
    public String toString() {
        return "IniciarSesionDto[ci=" + ci + ", rol=" + rol + "]";
    }
}
