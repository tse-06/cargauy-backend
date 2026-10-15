package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.util.UUID;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Vinculo polimorfico entre una entidad cualquiera y un {@link Archivo}. La
 * entidad se identifica por el nombre simple de su clase y su id (no hay FK),
 * y {@code nombre} indica la relacion que representa (ej. "archivo" en un
 * Permiso, "archivos" en un EventoIncidente).
 */
@Entity
@Table(
    name = "archivos_adjuntos",
    uniqueConstraints = @UniqueConstraint(
        columnNames = { "tipo_entidad", "entidad_id", "nombre", "archivo_id" }
    ),
    indexes = {
        @Index(
            name = "archivos_adjuntos_entidad_idx",
            columnList = "tipo_entidad, entidad_id, nombre"
        ),
        @Index(
            name = "archivos_adjuntos_archivo_idx",
            columnList = "archivo_id"
        ),
    }
)
@Getter
@Setter
@NoArgsConstructor
public class ArchivoAdjunto extends EntidadBase {

    @Column(name = "tipo_entidad", nullable = false)
    private String tipoEntidad;

    @Column(name = "entidad_id", nullable = false)
    private UUID entidadId;

    @Column(nullable = false)
    private String nombre;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "archivo_id", nullable = false)
    private Archivo archivo;
}
