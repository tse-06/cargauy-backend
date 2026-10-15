package uy.carga.tse.datatypes.vehiculo;

import java.time.LocalDate;
import java.util.UUID;

public record DetallePncDto(
    UUID id,
    String numero,
    LocalDate fechaVencimiento,
    String nombreArchivo,
    String urlArchivo
) {}
