package uy.carga.tse.servicios.seguridad;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.security.enterprise.credential.Credential;
import jakarta.security.enterprise.identitystore.CredentialValidationResult;
import jakarta.security.enterprise.identitystore.IdentityStore;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uy.carga.tse.logica.autenticacion.AutenticacionServicioLocal;
import uy.carga.tse.logica.autenticacion.Identidad;
import uy.carga.tse.logica.autenticacion.ServicioJwt;
import uy.carga.tse.logica.comun.excepciones.NoAutenticadoException;

/**
 * Valida un {@link CredencialJwt}: el token tiene que estar bien firmado y
 * vigente, y el usuario tiene que seguir teniendo el rol que lleva. El
 * caller es el id del usuario y su unico grupo es el rol activo.
 */
@ApplicationScoped
public class IdentityStoreJwt implements IdentityStore {

    private static final Logger log = LoggerFactory.getLogger(
        IdentityStoreJwt.class
    );

    @Inject
    ServicioJwt servicioJwt;

    @Inject
    AutenticacionServicioLocal autenticacionServicio;

    @Override
    public Set<ValidationType> validationTypes() {
        // PROVIDE_GROUPS: sin esto el handler descarta los grupos (el rol)
        // que devuelve validate()
        return Set.of(ValidationType.VALIDATE, ValidationType.PROVIDE_GROUPS);
    }

    @Override
    public CredentialValidationResult validate(Credential credencial) {
        if (!(credencial instanceof CredencialJwt jwt)) {
            return CredentialValidationResult.NOT_VALIDATED_RESULT;
        }
        try {
            Identidad identidad = servicioJwt.verificar(jwt.token());
            autenticacionServicio.exigirRol(
                identidad.usuarioId(),
                identidad.rol()
            );
            return new CredentialValidationResult(
                identidad.usuarioId().toString(),
                Set.of(identidad.rol().name())
            );
        } catch (NoAutenticadoException e) {
            log.debug("Autenticacion rechazada: {}", e.getMessage());
            return CredentialValidationResult.INVALID_RESULT;
        }
    }
}
