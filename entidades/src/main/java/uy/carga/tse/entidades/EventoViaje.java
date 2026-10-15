package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorColumn;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Jerarquia en una sola tabla: las columnas de las subclases (autogenerado,
 * peso, descripcion) son nullables.
 */
@Entity
@Table(name = "eventos_viaje")
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "tipo_evento")
@Getter
@Setter
@NoArgsConstructor
public abstract class EventoViaje extends EntidadBase {

    @Column(nullable = false)
    private Instant timestamp;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "guia_id", nullable = false)
    private Guia guia;
}
