package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import uy.carga.tse.entidades.enums.Rubro;

@Entity
@Table(name = "empresas")
@Getter
@Setter
@NoArgsConstructor
public class Empresa extends EntidadBase {

    @Column(nullable = false)
    private String nombre;

    @Column(name = "razon_social", nullable = false)
    private String razonSocial;

    @Column(nullable = false, unique = true)
    private String numero;

    @Column(nullable = false)
    private String direccion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Rubro rubro;

    // Inverso: se carga siempre (no puede ser lazy). Las consultas que
    // devuelven empresas lo traen con join para no hacer una consulta extra
    // por empresa
    @OneToOne(mappedBy = "empresa")
    private Permiso permiso;

    @ManyToMany(mappedBy = "empresas")
    private Set<Chofer> choferes = new HashSet<>();

    @ManyToMany(mappedBy = "empresas")
    private Set<EncargadoEmpresa> encargados = new HashSet<>();

    @OneToMany(mappedBy = "empresa")
    private List<Vehiculo> vehiculos = new ArrayList<>();

    @OneToMany(mappedBy = "empresa")
    private List<Guia> guias = new ArrayList<>();

    @OneToMany(mappedBy = "empresa")
    private List<Infraccion> infracciones = new ArrayList<>();
}
