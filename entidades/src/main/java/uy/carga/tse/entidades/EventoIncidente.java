package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@DiscriminatorValue("INCIDENTE")
@Getter
@Setter
@NoArgsConstructor
public class EventoIncidente extends EventoViaje {

    /** Nombre de la relacion con sus archivos en ArchivoServicioLocal (varios). */
    public static final String ADJUNTO_ARCHIVOS = "archivos";

    @Column(columnDefinition = "text")
    private String descripcion;
}
