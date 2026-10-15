package uy.carga.tse.logica.autenticacion;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.identitystore.Pbkdf2PasswordHash;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class HashContrasenia {

    @Inject
    Pbkdf2PasswordHash pbkdf2;

    // Este hash se utiliza cuando un usuario ingresa una cedula incorrecta, para
    // evitar leakear información
    // con el timing de las requests
    private String hashFicticio;

    @PostConstruct
    void iniciar() {
        pbkdf2.initialize(
            Map.of(
                "Pbkdf2PasswordHash.Algorithm",
                "PBKDF2WithHmacSHA256",
                "Pbkdf2PasswordHash.Iterations",
                "210000",
                "Pbkdf2PasswordHash.SaltSizeBytes",
                "32",
                "Pbkdf2PasswordHash.KeySizeBytes",
                "32"
            )
        );
        hashFicticio = pbkdf2.generate(
            UUID.randomUUID().toString().toCharArray()
        );
    }

    public String hashear(char[] contrasenia) {
        return pbkdf2.generate(contrasenia);
    }

    public boolean verificar(char[] contrasenia, String hashAlmacenado) {
        if (hashAlmacenado == null) {
            pbkdf2.verify(contrasenia, hashFicticio);
            return false;
        }
        return pbkdf2.verify(contrasenia, hashAlmacenado);
    }
}
