package uy.carga.tse.datatypes.usuario;

import java.time.Instant;

public record DetalleSesionDto(
    String token,
    String tipo,
    Instant expiraEn,
    String rol,
    String ci,
    String nombre,
    String apellido
) {}
