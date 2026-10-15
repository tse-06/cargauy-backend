package uy.carga.tse.logica.autenticacion;

import java.util.UUID;
import uy.carga.tse.entidades.enums.Rol;

/** Usuario y rol activo que lleva un token ya verificado. */
public record Identidad(UUID usuarioId, Rol rol) {}
