package uy.carga.tse.logica.autenticacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.entidades.Chofer;
import uy.carga.tse.entidades.Funcionario;
import uy.carga.tse.entidades.Usuario;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.usuario.AdministradorRepositorio;
import uy.carga.tse.logica.usuario.AutoridadRepositorio;
import uy.carga.tse.logica.usuario.ChoferRepositorio;
import uy.carga.tse.logica.usuario.EncargadoEmpresaRepositorio;
import uy.carga.tse.logica.usuario.FuncionarioRepositorio;

class RolesUsuarioTest {

    private final RolesUsuario roles = new RolesUsuario();
    private final Usuario usuario = new Usuario();

    @BeforeEach
    void preparar() {
        roles.administradorRepositorio = mock(AdministradorRepositorio.class);
        roles.autoridadRepositorio = mock(AutoridadRepositorio.class);
        roles.choferRepositorio = mock(ChoferRepositorio.class);
        roles.encargadoEmpresaRepositorio = mock(
            EncargadoEmpresaRepositorio.class
        );
        roles.funcionarioRepositorio = mock(FuncionarioRepositorio.class);
        when(roles.administradorRepositorio.porUsuario(any())).thenReturn(
            Optional.empty()
        );
        when(roles.autoridadRepositorio.porUsuario(any())).thenReturn(
            Optional.empty()
        );
        when(roles.choferRepositorio.porUsuario(any())).thenReturn(
            Optional.empty()
        );
        when(roles.encargadoEmpresaRepositorio.porUsuario(any())).thenReturn(
            Optional.empty()
        );
        when(roles.funcionarioRepositorio.porUsuario(any())).thenReturn(
            Optional.empty()
        );
    }

    @Test
    void reuneLosRolesDeCadaRepositorio() {
        when(roles.choferRepositorio.porUsuario(usuario)).thenReturn(
            Optional.of(new Chofer())
        );
        when(roles.funcionarioRepositorio.porUsuario(usuario)).thenReturn(
            Optional.of(new Funcionario())
        );

        assertEquals(
            EnumSet.of(Rol.CHOFER, Rol.FUNCIONARIO),
            roles.de(usuario)
        );
        assertTrue(roles.tiene(usuario, Rol.CHOFER));
        assertFalse(roles.tiene(usuario, Rol.ADMINISTRADOR));
    }

    @Test
    void sinRolesNoPuedeElegir() {
        assertThrows(UsuarioSinRolesException.class, () ->
            RolesUsuario.elegir(Set.of(), null)
        );
        assertThrows(UsuarioSinRolesException.class, () ->
            RolesUsuario.elegir(Set.of(), Rol.CHOFER)
        );
    }

    @Test
    void conUnSoloRolNoHaceFaltaPedirlo() {
        assertEquals(Rol.CHOFER, RolesUsuario.elegir(Set.of(Rol.CHOFER), null));
    }

    @Test
    void conVariosRolesHayQuePedirUno() {
        Set<Rol> habilitados = EnumSet.of(Rol.CHOFER, Rol.FUNCIONARIO);
        assertThrows(RolRequeridoException.class, () ->
            RolesUsuario.elegir(habilitados, null)
        );
        assertEquals(
            Rol.FUNCIONARIO,
            RolesUsuario.elegir(habilitados, Rol.FUNCIONARIO)
        );
    }

    @Test
    void noSePuedePedirUnRolQueNoTiene() {
        assertThrows(RolNoAsignadoException.class, () ->
            RolesUsuario.elegir(Set.of(Rol.CHOFER), Rol.ADMINISTRADOR)
        );
    }

    @Test
    void parseaElRolSinImportarMayusculasNiEspacios() {
        assertEquals(
            Rol.ENCARGADO_EMPRESA,
            RolesUsuario.parsear(" encargado_empresa ")
        );
        assertNull(RolesUsuario.parsear(null));
        assertNull(RolesUsuario.parsear("  "));
        assertThrows(RolInvalidoException.class, () ->
            RolesUsuario.parsear("JEFE")
        );
    }
}
