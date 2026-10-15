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
@Table(name = "choferes")
@Getter
@Setter
@NoArgsConstructor
public class Chofer extends RolUsuario {

    @ManyToMany
    @JoinTable(
        name = "choferes_empresas",
        joinColumns = @JoinColumn(name = "chofer_id"),
        inverseJoinColumns = @JoinColumn(name = "empresa_id")
    )
    private Set<Empresa> empresas = new HashSet<>();

    @OneToMany(mappedBy = "chofer")
    private List<Guia> guias = new ArrayList<>();
}
