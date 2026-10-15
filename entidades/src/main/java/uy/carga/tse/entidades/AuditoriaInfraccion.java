package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "auditorias_infraccion")
@Getter
@Setter
@NoArgsConstructor
public class AuditoriaInfraccion extends EntidadBase {

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String accion;

    @Column(columnDefinition = "text")
    private String fundamento;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "infraccion_id", nullable = false)
    private Infraccion infraccion;

    // Nullable: las acciones automaticas del sistema no tienen usuario
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id")
    private Usuario usuario;
}
