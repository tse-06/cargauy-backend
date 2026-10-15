package uy.carga.tse.logica.usuario;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static uy.carga.tse.logica.testsupport.Entidades.conId;

import jakarta.ejb.SessionContext;
import java.security.Principal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumSet;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.usuario.CrearUsuarioDto;
import uy.carga.tse.datatypes.usuario.DetalleUsuarioDto;
import uy.carga.tse.datatypes.usuario.ModificarUsuarioDto;
import uy.carga.tse.entidades.Administrador;
import uy.carga.tse.entidades.Usuario;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.autenticacion.HashContrasenia;
import uy.carga.tse.logica.autenticacion.RolesUsuario;
import uy.carga.tse.logica.comun.excepciones.ConflictoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;
import uy.carga.tse.logica.usuario.UsuarioServicioLocal.NuevoUsuario;

class UsuarioServicioTest {

    private static final LocalDate NACIMIENTO = LocalDate.of(1990, 5, 1);
    private static final NuevoUsuario CIUDADANO = new NuevoUsuario(
        "1.234.567-8",
        "Ana",
        "Perez",
        " ana@example.uy ",
        NACIMIENTO
    );
    private static final CrearUsuarioDto INTERNO = new CrearUsuarioDto(
        "87654321",
        "Juan",
        "Gomez",
        "juan@example.uy",
        NACIMIENTO,
        "contrasenia-segura",
        List.of("administrador")
    );

    private final UsuarioServicio servicio = new UsuarioServicio();

    @BeforeEach
    void preparar() {
        servicio.usuarioRepositorio = mock(UsuarioRepositorio.class);
        servicio.administradorRepositorio = mock(
            AdministradorRepositorio.class
        );
        servicio.autoridadRepositorio = mock(AutoridadRepositorio.class);
        servicio.hashContrasenia = mock(HashContrasenia.class);
        servicio.roles = mock(RolesUsuario.class);
        servicio.usuarioMapper = new UsuarioMapperImpl();
        servicio.contexto = mock(SessionContext.class);
        Principal admin = mock(Principal.class);
        when(admin.getName()).thenReturn(UUID.randomUUID().toString());
        when(servicio.contexto.getCallerPrincipal()).thenReturn(admin);

        when(servicio.usuarioRepositorio.porCi(anyString())).thenReturn(
            Optional.empty()
        );
        when(servicio.usuarioRepositorio.porEmail(anyString())).thenReturn(
            Optional.empty()
        );
        when(servicio.usuarioRepositorio.insert(any(Usuario.class))).thenAnswer(
            i -> conId(i.getArgument(0))
        );
        when(servicio.hashContrasenia.hashear(any())).thenReturn("hash");
    }

    @Test
    void registraAlCiudadanoSinContraseniaYConLosDatosNormalizados() {
        UUID id = servicio.registrarCiudadano(CIUDADANO);

        Usuario creado = insertado();
        assertEquals(id, creado.getId());
        assertEquals("12345678", creado.getCi());
        assertEquals("ana@example.uy", creado.getEmail());
        assertEquals("Ana", creado.getNombre());
        assertEquals("Perez", creado.getApellido());
        assertEquals(NACIMIENTO, creado.getFechaNacimiento());
        assertNull(creado.getContrasenia());
        verify(servicio.administradorRepositorio, never()).insert(any());
        verify(servicio.autoridadRepositorio, never()).insert(any());
    }

    @Test
    void rechazaUnaCedulaSinDigitos() {
        NuevoUsuario sinDigitos = new NuevoUsuario(
            "sin cedula",
            "Ana",
            "Perez",
            "ana@example.uy",
            NACIMIENTO
        );
        ValidacionNegocioException e = assertThrows(
            ValidacionNegocioException.class,
            () -> servicio.registrarCiudadano(sinDigitos)
        );
        assertEquals(CodigoError.CI_INVALIDA, e.codigo());
        verify(servicio.usuarioRepositorio, never()).insert(any(Usuario.class));
    }

    @Test
    void elUsuarioInternoTieneContraseniaYSuRol() {
        DetalleUsuarioDto detalle = servicio.crearUsuarioInterno(INTERNO);

        assertEquals(List.of("ADMINISTRADOR"), detalle.roles());

        assertEquals("hash", insertado().getContrasenia());
        verify(servicio.administradorRepositorio).insert(
            any(Administrador.class)
        );
    }

    @Test
    void noRegistraUnaCedulaRepetida() {
        when(servicio.usuarioRepositorio.porCi("12345678")).thenReturn(
            Optional.of(conId(new Usuario()))
        );

        ConflictoException e = assertThrows(ConflictoException.class, () ->
            servicio.registrarCiudadano(CIUDADANO)
        );
        assertEquals(CodigoError.CI_EN_USO, e.codigo());
        verify(servicio.usuarioRepositorio, never()).insert(any(Usuario.class));
    }

    @Test
    void noRegistraUnCorreoRepetido() {
        when(
            servicio.usuarioRepositorio.porEmail("juan@example.uy")
        ).thenReturn(Optional.of(conId(new Usuario())));

        ConflictoException e = assertThrows(ConflictoException.class, () ->
            servicio.crearUsuarioInterno(INTERNO)
        );
        assertEquals(CodigoError.EMAIL_EN_USO, e.codigo());
        verify(servicio.administradorRepositorio, never()).insert(any());
    }

    @Test
    void alModificarPuedeConservarSuPropiaCedulaYCorreo() {
        Usuario existente = conId(new Usuario());
        when(
            servicio.usuarioRepositorio.findById(existente.getId())
        ).thenReturn(Optional.of(existente));
        when(servicio.usuarioRepositorio.porCi("87654321")).thenReturn(
            Optional.of(existente)
        );
        when(
            servicio.usuarioRepositorio.porEmail("juan@example.uy")
        ).thenReturn(Optional.of(existente));
        when(servicio.usuarioRepositorio.update(existente)).thenReturn(
            existente
        );

        servicio.modificarUsuario(existente.getId(), modificacion());

        verify(servicio.usuarioRepositorio).update(existente);
    }

    @Test
    void alModificarNoPuedeTomarElCorreoDeOtro() {
        Usuario existente = conId(new Usuario());
        when(
            servicio.usuarioRepositorio.findById(existente.getId())
        ).thenReturn(Optional.of(existente));
        when(
            servicio.usuarioRepositorio.porEmail("juan@example.uy")
        ).thenReturn(Optional.of(conId(new Usuario())));

        ConflictoException e = assertThrows(ConflictoException.class, () ->
            servicio.modificarUsuario(existente.getId(), modificacion())
        );
        assertEquals(CodigoError.EMAIL_EN_USO, e.codigo());
        verify(servicio.usuarioRepositorio, never()).update(any(Usuario.class));
    }

    @Test
    void daDeBajaAlUsuarioConLaFechaActual() {
        Usuario existente = conId(new Usuario());
        when(
            servicio.usuarioRepositorio.findById(existente.getId())
        ).thenReturn(Optional.of(existente));
        when(servicio.usuarioRepositorio.update(existente)).thenReturn(
            existente
        );

        servicio.darDeBaja(existente.getId());

        assertNotNull(existente.getFechaBaja());
        verify(servicio.usuarioRepositorio).update(existente);
    }

    @Test
    void darDeBajaAUnUsuarioYaDadoDeBajaConservaLaFechaOriginal() {
        Usuario existente = conId(new Usuario());
        Instant original = Instant.parse("2026-01-01T00:00:00Z");
        existente.setFechaBaja(original);
        when(
            servicio.usuarioRepositorio.findById(existente.getId())
        ).thenReturn(Optional.of(existente));

        servicio.darDeBaja(existente.getId());

        assertEquals(original, existente.getFechaBaja());
        verify(servicio.usuarioRepositorio, never()).update(any(Usuario.class));
    }

    @Test
    void unAdministradorNoPuedeDarseDeBajaASiMismo() {
        Usuario existente = conId(new Usuario());
        Principal yo = mock(Principal.class);
        when(yo.getName()).thenReturn(existente.getId().toString());
        when(servicio.contexto.getCallerPrincipal()).thenReturn(yo);

        ValidacionNegocioException e = assertThrows(
            ValidacionNegocioException.class,
            () -> servicio.darDeBaja(existente.getId())
        );
        assertEquals(CodigoError.AUTO_BAJA_NO_PERMITIDA, e.codigo());
        verify(servicio.usuarioRepositorio, never()).update(any(Usuario.class));
    }

    @Test
    void elDetalleListaTodosLosRolesDelUsuario() {
        Usuario existente = conId(new Usuario());
        when(
            servicio.usuarioRepositorio.findById(existente.getId())
        ).thenReturn(Optional.of(existente));
        when(servicio.roles.de(existente)).thenReturn(
            EnumSet.of(Rol.FUNCIONARIO, Rol.CHOFER)
        );

        assertEquals(
            List.of("CHOFER", "FUNCIONARIO"),
            servicio.obtener(existente.getId()).roles()
        );
    }

    @Test
    void daDeAltaAlUsuarioDadoDeBaja() {
        Usuario existente = conId(new Usuario());
        existente.setFechaBaja(Instant.parse("2026-01-01T00:00:00Z"));
        when(
            servicio.usuarioRepositorio.findById(existente.getId())
        ).thenReturn(Optional.of(existente));
        when(servicio.usuarioRepositorio.update(existente)).thenReturn(
            existente
        );

        servicio.darDeAlta(existente.getId());

        assertNull(existente.getFechaBaja());
        verify(servicio.usuarioRepositorio).update(existente);
    }

    private static ModificarUsuarioDto modificacion() {
        return new ModificarUsuarioDto(
            "87654321",
            "Juan",
            "Gomez",
            "juan@example.uy",
            NACIMIENTO
        );
    }

    private Usuario insertado() {
        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(servicio.usuarioRepositorio).insert(captor.capture());
        return captor.getValue();
    }
}
