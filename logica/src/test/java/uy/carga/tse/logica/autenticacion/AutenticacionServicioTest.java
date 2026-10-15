package uy.carga.tse.logica.autenticacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uy.carga.tse.logica.testsupport.Entidades.conId;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.usuario.DetalleSesionDto;
import uy.carga.tse.entidades.Usuario;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.usuario.UsuarioMapperImpl;
import uy.carga.tse.logica.usuario.UsuarioRepositorio;

class AutenticacionServicioTest {

    private static final Instant VENCE = Instant.parse("2026-10-13T12:30:00Z");

    private final AutenticacionServicio servicio = new AutenticacionServicio();

    @BeforeEach
    void preparar() {
        servicio.usuarioRepositorio = mock(UsuarioRepositorio.class);
        servicio.usuarioMapper = new UsuarioMapperImpl();
    }

    @Test
    void detalleSesionDevuelveLosDatosDelUsuarioSinToken() {
        Usuario usuario = conId(new Usuario());
        usuario.setCi("12345678");
        usuario.setNombre("Ana");
        usuario.setApellido("Perez");
        when(servicio.usuarioRepositorio.findById(usuario.getId())).thenReturn(
            Optional.of(usuario)
        );

        DetalleSesionDto detalle = servicio.detalleSesion(
            usuario.getId(),
            Rol.ADMINISTRADOR,
            VENCE
        );

        assertNull(detalle.token());
        assertEquals("ADMINISTRADOR", detalle.rol());
        assertEquals(VENCE, detalle.expiraEn());
        assertEquals("12345678", detalle.ci());
        assertEquals("Ana", detalle.nombre());
        assertEquals("Perez", detalle.apellido());
    }

    @Test
    void detalleSesionDeUnUsuarioInexistenteEsRolRevocado() {
        UUID id = UUID.randomUUID();
        when(servicio.usuarioRepositorio.findById(id)).thenReturn(
            Optional.empty()
        );

        assertThrows(RolRevocadoException.class, () ->
            servicio.detalleSesion(id, Rol.ADMINISTRADOR, VENCE)
        );
    }
}
