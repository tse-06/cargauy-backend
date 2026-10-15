package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.locationtech.jts.geom.Point;
import uy.carga.tse.entidades.enums.EstadoGuia;
import uy.carga.tse.entidades.enums.Rubro;
import uy.carga.tse.entidades.enums.TipoCarga;

@Entity
@Table(name = "guias")
@Getter
@Setter
@NoArgsConstructor
public class Guia extends EntidadBase {

    @Enumerated(EnumType.STRING)
    @Column(name = "rubro_cliente", nullable = false)
    private Rubro rubroCliente;

    // Peso declarado de la carga, en kg
    @Column(name = "peso_carga", nullable = false, precision = 12, scale = 3)
    private BigDecimal pesoCarga;

    @Column(nullable = false)
    private String origen;

    @Column(nullable = false)
    private String destino;

    // Coordenadas de origen y destino (SRID 4326: x = longitud, y = latitud)
    @Column(
        name = "ubicacion_origen",
        nullable = false,
        columnDefinition = "geography(Point,4326)"
    )
    private Point ubicacionOrigen;

    @Column(
        name = "ubicacion_destino",
        nullable = false,
        columnDefinition = "geography(Point,4326)"
    )
    private Point ubicacionDestino;

    @Column(nullable = false)
    private LocalDate fecha;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_carga", nullable = false)
    private TipoCarga tipoCarga;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoGuia estado;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chofer_id")
    private Chofer chofer;

    @OneToMany(mappedBy = "guia")
    private List<EventoViaje> eventos = new ArrayList<>();
}
