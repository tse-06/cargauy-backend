package uy.carga.tse.logica.empresa;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.UUID;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.usuario.EncargadoEmpresaRepositorio;

/**
 * Chequeos de acceso a los datos de una empresa segun el caller. A quien no
 * tiene acceso se le responde como si el recurso no existiera, para no revelar
 * que existe.
 */
@Stateless
public class AccesoEmpresa {

    @Inject
    EmpresaRepositorio empresaRepositorio;

    @Inject
    EncargadoEmpresaRepositorio encargadoRepositorio;

    // El caller es el id del usuario y su unico rol el activo (ver
    // IdentityStoreJwt)
    @Resource
    SessionContext contexto;

    /**
     * Si el caller es encargado de la empresa. Cada servicio decide que
     * responder si no lo es (empresa o vehiculo no encontrado, ...).
     */
    public boolean esEncargadoDe(UUID empresaId) {
        return (
            contexto.isCallerInRole(Roles.ENCARGADO_EMPRESA) &&
            encargadoRepositorio.gestiona(
                UUID.fromString(contexto.getCallerPrincipal().getName()),
                empresaId
            )
        );
    }

    /**
     * Para leer los datos de la empresa: administradores y autoridades ven
     * cualquiera, los encargados solo las suyas. Si no existe o no tiene
     * acceso, la empresa no se encuentra.
     */
    public void exigirAcceso(UUID empresaId) {
        boolean puedeVer =
            contexto.isCallerInRole(Roles.ADMINISTRADOR) ||
            contexto.isCallerInRole(Roles.AUTORIDAD)
                ? empresaRepositorio.findById(empresaId).isPresent()
                : esEncargadoDe(empresaId);
        if (!puedeVer) {
            throw new RecursoNoEncontradoException(
                CodigoError.EMPRESA_NO_ENCONTRADA,
                "Empresa no encontrada: " + empresaId
            );
        }
    }
}
