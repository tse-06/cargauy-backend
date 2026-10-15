package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "permisos")
@Getter
@Setter
@NoArgsConstructor
public class Permiso extends EntidadBase {

    /** Nombre de la relacion con su archivo en ArchivoServicioLocal (uno solo). */
    public static final String ADJUNTO_ARCHIVO = "archivo";

    @Column(name = "codigo_validacion", nullable = false)
    private String codigoValidacion;

    @Column(name = "fecha_vencimiento", nullable = false)
    private LocalDate fechaVencimiento;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "empresa_id", nullable = false, unique = true)
    private Empresa empresa;
}
