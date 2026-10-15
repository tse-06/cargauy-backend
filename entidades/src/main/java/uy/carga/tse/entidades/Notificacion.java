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
@Table(name = "notificaciones")
@Getter
@Setter
@NoArgsConstructor
public class Notificacion extends EntidadBase {

    @Column(nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private boolean leida;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "encargado_empresa_id", nullable = false)
    private EncargadoEmpresa encargado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "infraccion_id", nullable = false)
    private Infraccion infraccion;
}
