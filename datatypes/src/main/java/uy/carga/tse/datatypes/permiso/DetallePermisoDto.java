package uy.carga.tse.datatypes.permiso;

import java.time.LocalDate;
import java.util.UUID;

public record DetallePermisoDto(
    UUID id,
    String codigoValidacion,
    LocalDate fechaVencimiento,
    String nombreArchivo,
    String urlArchivo
) {}
