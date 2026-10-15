package uy.carga.tse.entidades;

import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import lombok.NoArgsConstructor;

@Entity
@DiscriminatorValue("INICIO")
@NoArgsConstructor
public class EventoInicio extends EventoViaje {}
