package uy.carga.tse.datatypes.usuario;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record ModificarUsuarioDto(
    @NotBlank @Size(max = 255) String ci,
    @NotBlank @Size(max = 255) String nombre,
    @NotBlank @Size(max = 255) String apellido,
    @NotBlank @Email @Size(max = 255) String email,
    @NotNull @Past LocalDate fechaNacimiento
) {}
