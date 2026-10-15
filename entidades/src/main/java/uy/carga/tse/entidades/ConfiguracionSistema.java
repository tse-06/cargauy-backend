package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Duration;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Singleton: se espera una unica fila en la tabla (se garantiza en la capa de
 * logica).
 */
@Entity
@Table(name = "configuraciones_sistema")
@Getter
@Setter
@NoArgsConstructor
public class ConfiguracionSistema extends EntidadBase {

    // Porcentaje de tolerancia sobre la capacidad del vehiculo
    @Column(
        name = "tolerancia_exceso_carga",
        nullable = false,
        precision = 7,
        scale = 3
    )
    private BigDecimal toleranciaExcesoCarga;

    // Distancia maxima permitida respecto a la ruta, en metros
    @Column(
        name = "tolerancia_desvio_ruta",
        nullable = false,
        precision = 10,
        scale = 2
    )
    private BigDecimal toleranciaDesvioRuta;

    @Column(name = "periodo_descargo", nullable = false)
    private Duration periodoDescargo;

    @Column(name = "periodo_maximo_viaje", nullable = false)
    private Duration periodoMaximoViaje;

    // Porcentaje de diferencia aceptado entre los pesos de las balanzas
    @Column(
        name = "tolerancia_diferencia_peso_balanzas",
        nullable = false,
        precision = 7,
        scale = 3
    )
    private BigDecimal toleranciaDiferenciaPesoBalanzas;

    // Cantidad minima de infracciones confirmadas del mismo tipo para
    // considerar una infraccion recurrente
    @Column(name = "cantidad_infracciones_recurrentes", nullable = false)
    private Integer cantidadInfraccionesRecurrentes;

    // Ventana, hacia atras desde hoy, en la que se cuentan esas infracciones
    @Column(name = "periodo_infraccion_recurrente", nullable = false)
    private Duration periodoInfraccionRecurrente;
}
