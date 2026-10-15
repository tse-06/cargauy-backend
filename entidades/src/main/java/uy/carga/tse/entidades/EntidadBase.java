package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import java.time.Instant;
import java.util.UUID;
import lombok.Getter;
import org.hibernate.Hibernate;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/**
 * Superclase de todas las entidades: id UUID generado por Hibernate (columna
 * uuid en PostgreSQL), marcas de alta y ultima modificacion, y equals/hashCode
 * compatibles con proxies lazy de Hibernate (igualdad por clase real + id).
 */
@MappedSuperclass
@Getter
public abstract class EntidadBase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Momento del alta. Se toma del reloj de la JVM y no de la base:
    // current_timestamp es el inicio de la transaccion y empataria las
    // entidades creadas en una misma transaccion (ej. el orden de los adjuntos)
    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private Instant creado;

    // Momento de la ultima modificacion; Hibernate lo actualiza en cada update
    @UpdateTimestamp
    @Column(nullable = false)
    private Instant actualizado;

    @Override
    public final boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof EntidadBase otra)) {
            return false;
        }
        return (
            Hibernate.getClass(this) == Hibernate.getClass(otra) &&
            getId() != null &&
            getId().equals(otra.getId())
        );
    }

    @Override
    public final int hashCode() {
        // Constante por clase: el id puede ser null hasta el persist
        return Hibernate.getClass(this).hashCode();
    }

    @Override
    public String toString() {
        return Hibernate.getClass(this).getSimpleName() + "#" + getId();
    }
}
