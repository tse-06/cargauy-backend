package uy.carga.tse.datatypes.vehiculo;

import java.time.LocalDate;
import java.util.UUID;

public record DetalleItvDto(
    UUID id,
    LocalDate fechaVencimiento,
    String nombreArchivo,
    String urlArchivo
) {}
