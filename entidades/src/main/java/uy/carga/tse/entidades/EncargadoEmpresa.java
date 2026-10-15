package uy.carga.tse.entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "encargados_empresa")
@Getter
@Setter
@NoArgsConstructor
public class EncargadoEmpresa extends RolUsuario {

    @ManyToMany
    @JoinTable(
        name = "encargados_empresa_empresas",
        joinColumns = @JoinColumn(name = "encargado_empresa_id"),
        inverseJoinColumns = @JoinColumn(name = "empresa_id")
    )
    private Set<Empresa> empresas = new HashSet<>();

    @OneToMany(mappedBy = "encargado")
    private List<Notificacion> notificaciones = new ArrayList<>();
}
