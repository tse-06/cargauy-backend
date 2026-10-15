package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("FIN")
@Getter
@Setter
@NoArgsConstructor
public class EventoFin extends EventoViaje {

    // true si el sistema genero el fin del viaje (no lo reporto el chofer)
    @Column
    private Boolean autogenerado;
}
