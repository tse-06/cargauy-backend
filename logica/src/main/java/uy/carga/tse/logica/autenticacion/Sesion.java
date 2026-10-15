package uy.carga.tse.logica.autenticacion;

import java.time.Instant;
import uy.carga.tse.entidades.enums.Rol;

public record Sesion(String token, Instant expiraEn, Rol rol) {
    @Override
    public String toString() {
        return "Sesion[expiraEn=" + expiraEn + ", rol=" + rol + "]";
    }
}
