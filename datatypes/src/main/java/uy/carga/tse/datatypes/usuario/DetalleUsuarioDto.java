package uy.carga.tse.datatypes.usuario;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record DetalleUsuarioDto(
    UUID id,
    String ci,
    String nombre,
    String apellido,
    String email,
    LocalDate fechaNacimiento,
    Instant fechaBaja,
    List<String> roles,
    Instant creado,
    Instant actualizado
) {}
