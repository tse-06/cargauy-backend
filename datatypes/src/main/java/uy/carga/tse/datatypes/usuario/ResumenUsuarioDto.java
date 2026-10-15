package uy.carga.tse.datatypes.usuario;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ResumenUsuarioDto(
    UUID id,
    String ci,
    String nombre,
    String apellido,
    Instant fechaBaja,
    List<String> roles
) {}
