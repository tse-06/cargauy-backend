package uy.carga.tse.entidades;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "administradores")
@NoArgsConstructor
public class Administrador extends RolUsuario {}
