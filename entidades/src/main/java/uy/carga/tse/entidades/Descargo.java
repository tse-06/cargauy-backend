package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "descargos")
@Getter
@Setter
@NoArgsConstructor
public class Descargo extends EntidadBase {

    @Column(nullable = false, columnDefinition = "text")
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "infraccion_id", nullable = false, unique = true)
    private Infraccion infraccion;
}
