package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import java.math.BigDecimal;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("CARGA")
@Getter
@Setter
@NoArgsConstructor
public class EventoCarga extends EventoViaje {

    // Peso cargado, en kg
    @Column(precision = 12, scale = 3)
    private BigDecimal peso;
}
