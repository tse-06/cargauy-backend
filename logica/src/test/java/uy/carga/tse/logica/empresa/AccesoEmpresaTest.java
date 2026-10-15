package uy.carga.tse.logica.empresa;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static uy.carga.tse.logica.testsupport.Entidades.conId;

import jakarta.ejb.SessionContext;
import java.security.Principal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.usuario.EncargadoEmpresaRepositorio;

class AccesoEmpresaTest {

    private static final UUID USUARIO_ID = UUID.randomUUID();

    private final AccesoEmpresa acceso = new AccesoEmpresa();
    private final Empresa empresa = conId(new Empresa());

    @BeforeEach
    void preparar() {
        acceso.empresaRepositorio = mock(EmpresaRepositorio.class);
        acceso.encargadoRepositorio = mock(EncargadoEmpresaRepositorio.class);
        acceso.contexto = mock(SessionContext.class);

        Principal caller = () -> USUARIO_ID.toString();
        when(acceso.contexto.getCallerPrincipal()).thenReturn(caller);
        when(acceso.contexto.isCallerInRole(anyString())).thenReturn(false);
        when(acceso.empresaRepositorio.findById(empresa.getId())).thenReturn(
            Optional.of(empresa)
        );
    }

    @Test
    void esEncargadoDeLaEmpresaQueGestiona() {
        comoEncargado(true);

        assertTrue(acceso.esEncargadoDe(empresa.getId()));
    }

    @Test
    void noEsEncargadoDeOtraEmpresa() {
        comoEncargado(false);

        assertFalse(acceso.esEncargadoDe(empresa.getId()));
    }

    @Test
    void elAdministradorNoEsEncargado() {
        conRol(Roles.ADMINISTRADOR);

        assertFalse(acceso.esEncargadoDe(empresa.getId()));
        verifyNoInteractions(acceso.encargadoRepositorio);
    }

    @Test
    void exigirAccesoAceptaAlAdministradorYAutoridad() {
        conRol(Roles.ADMINISTRADOR);
        assertDoesNotThrow(() -> acceso.exigirAcceso(empresa.getId()));

        conRol(Roles.AUTORIDAD);
        assertDoesNotThrow(() -> acceso.exigirAcceso(empresa.getId()));

        verifyNoInteractions(acceso.encargadoRepositorio);
    }

    @Test
    void exigirAccesoRespondeNoEncontradaSiNoExiste() {
        conRol(Roles.ADMINISTRADOR);
        UUID otra = UUID.randomUUID();
        when(acceso.empresaRepositorio.findById(otra)).thenReturn(
            Optional.empty()
        );

        RecursoNoEncontradoException e = assertThrows(
            RecursoNoEncontradoException.class,
            () -> acceso.exigirAcceso(otra)
        );
        assertEquals(CodigoError.EMPRESA_NO_ENCONTRADA, e.codigo());
    }

    @Test
    void exigirAccesoAceptaAlEncargadoDeLaEmpresa() {
        comoEncargado(true);

        assertDoesNotThrow(() -> acceso.exigirAcceso(empresa.getId()));
    }

    @Test
    void exigirAccesoRespondeNoEncontradaAlEncargadoDeOtraEmpresa() {
        comoEncargado(false);

        RecursoNoEncontradoException e = assertThrows(
            RecursoNoEncontradoException.class,
            () -> acceso.exigirAcceso(empresa.getId())
        );
        assertEquals(CodigoError.EMPRESA_NO_ENCONTRADA, e.codigo());
    }

    @Test
    void exigirAccesoRespondeNoEncontradaAOtrosRoles() {
        conRol(Roles.CHOFER);

        assertThrows(RecursoNoEncontradoException.class, () ->
            acceso.exigirAcceso(empresa.getId())
        );
        verifyNoInteractions(acceso.encargadoRepositorio);
    }

    /** El caller tiene solo ese rol. */
    private void conRol(String rol) {
        when(acceso.contexto.isCallerInRole(anyString())).thenAnswer(i ->
            rol.equals(i.getArgument(0))
        );
    }

    private void comoEncargado(boolean gestiona) {
        conRol(Roles.ENCARGADO_EMPRESA);
        when(
            acceso.encargadoRepositorio.gestiona(USUARIO_ID, empresa.getId())
        ).thenReturn(gestiona);
    }
}
