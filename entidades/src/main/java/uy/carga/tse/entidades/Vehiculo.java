package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "vehiculos")
@Getter
@Setter
@NoArgsConstructor
public class Vehiculo extends EntidadBase {

    @Column(nullable = false, unique = true)
    private String matricula;

    @Column(nullable = false)
    private String marca;

    @Column(nullable = false)
    private String modelo;

    // Tara del vehiculo, en kg
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal peso;

    // Capacidad maxima de carga, en kg
    @Column(nullable = false, precision = 12, scale = 3)
    private BigDecimal capacidad;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @OneToOne(mappedBy = "vehiculo")
    private PermisoNacionalDeCirculacion permisoNacionalDeCirculacion;

    @OneToOne(mappedBy = "vehiculo")
    private InspeccionTecnicaVehicular inspeccionTecnicaVehicular;
}
