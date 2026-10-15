package uy.carga.tse.entidades;

import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.OneToOne;
import lombok.Getter;
import lombok.Setter;

/**
 * Base de los roles de un {@link Usuario}: relacion 1 a 0..1 con Usuario,
 * la FK (unica) vive en la tabla del rol.
 */
@MappedSuperclass
@Getter
@Setter
public abstract class RolUsuario extends EntidadBase {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false, unique = true)
    private Usuario usuario;
}
