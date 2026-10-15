package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("DESCARGA")
@Getter
@Setter
@NoArgsConstructor
public class EventoDescarga extends EventoViaje {

    // Peso descargado, en kg
    @Column(precision = 12, scale = 3)
    private BigDecimal peso;
}
