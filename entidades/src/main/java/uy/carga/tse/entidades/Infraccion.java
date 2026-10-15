package uy.carga.tse.entidades;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import uy.carga.tse.entidades.enums.EstadoInfraccion;
import uy.carga.tse.entidades.enums.TipoInfraccion;

@Entity
@Table(name = "infracciones")
@Getter
@Setter
@NoArgsConstructor
public class Infraccion extends EntidadBase {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private EstadoInfraccion estado;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TipoInfraccion tipo;

    // Documento JSON libre, la estructura depende del tipo de infraccion
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private Map<String, Object> evidencia;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false)
    private Empresa empresa;

    // Nullable: hay infracciones (ej. ViajeSinGuia) que pueden no resolver un
    // vehiculo
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vehiculo_id")
    private Vehiculo vehiculo;

    @OneToOne(mappedBy = "infraccion", cascade = CascadeType.ALL)
    private Descargo descargo;

    @OneToOne(mappedBy = "infraccion", cascade = CascadeType.ALL)
    private Resolucion resolucion;

    @OneToMany(mappedBy = "infraccion")
    private List<AuditoriaInfraccion> auditorias = new ArrayList<>();

    @OneToMany(mappedBy = "infraccion")
    private List<Notificacion> notificaciones = new ArrayList<>();
}
