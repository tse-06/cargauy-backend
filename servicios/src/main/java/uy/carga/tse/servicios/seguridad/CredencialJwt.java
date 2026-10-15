package uy.carga.tse.servicios.seguridad;

import jakarta.security.enterprise.credential.Credential;

/** Credencial que lleva el JWT recibido en el header Authorization. */
public record CredencialJwt(String token) implements Credential {
    @Override
    public String toString() {
        return "CredencialJwt[]";
    }
}
