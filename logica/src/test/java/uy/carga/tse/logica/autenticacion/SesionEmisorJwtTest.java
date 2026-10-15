package uy.carga.tse.logica.autenticacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import jakarta.ejb.EJBException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.enums.Rol;
import uy.carga.tse.logica.autenticacion.AutenticacionServicioLocal.Cuenta;
import uy.carga.tse.logica.autenticacion.gubuy.GubUyIdentity;
import uy.carga.tse.logica.autenticacion.gubuy.PersonasPdi;
import uy.carga.tse.logica.autenticacion.gubuy.PersonasPdi.DatosPersona;
import uy.carga.tse.logica.autenticacion.gubuy.SesionEmisorJwt;
import uy.carga.tse.logica.comun.excepciones.ConflictoException;
import uy.carga.tse.logica.comun.excepciones.ExcepcionNegocio;
import uy.carga.tse.logica.comun.excepciones.ServicioNoDisponibleException;
import uy.carga.tse.logica.usuario.UsuarioServicioLocal;
import uy.carga.tse.logica.usuario.UsuarioServicioLocal.NuevoUsuario;

/**
 * Esta en el paquete de ServicioJwt porque su inicializacion es de paquete.
 * Usa el ServicioJwt real y verifica los tokens que emite.
 */
class SesionEmisorJwtTest {

    private static final Clock RELOJ = Clock.fixed(
        Instant.parse("2026-10-09T12:00:00Z"),
        ZoneOffset.UTC
    );
    private static final LocalDate HOY = LocalDate.of(2026, 10, 9);
    private static final GubUyIdentity IDENTIDAD = new GubUyIdentity(
        "uid-1",
        "12345678",
        "persona@example.uy",
        "2"
    );

    private ServicioJwt jwt;
    private UsuarioServicioLocal usuarios;
    /** Usuarios registrados por cedula, como los ve AutenticacionServicio. */
    private Map<String, Cuenta> cuentas;
    private List<NuevoUsuario> registrados;
    private Optional<DatosPersona> persona;
    private boolean pdiCaida;
    private int consultasPdi;
    private SesionEmisorJwt emisor;

    @BeforeEach
    void preparar() {
        jwt = new ServicioJwt();
        jwt.iniciar(
            Map.of("JWT_SECRET", "secreto-de-prueba-0123456789abcdefghij")::get
        );

        cuentas = new HashMap<>();
        AutenticacionServicioLocal autenticacion = mock(
            AutenticacionServicioLocal.class
        );
        when(autenticacion.cuentaPorCi(anyString())).thenAnswer(i ->
            Optional.ofNullable(cuentas.get(i.<String>getArgument(0)))
        );

        registrados = new ArrayList<>();
        usuarios = mock(UsuarioServicioLocal.class);
        when(usuarios.registrarCiudadano(any())).thenAnswer(i -> {
            NuevoUsuario nuevo = i.getArgument(0);
            registrados.add(nuevo);
            UUID id = UUID.randomUUID();
            cuentas.put(nuevo.ci(), new Cuenta(id, Set.of()));
            return id;
        });

        persona = Optional.of(
            new DatosPersona("Ana", "Perez", HOY.minusYears(30))
        );
        pdiCaida = false;
        consultasPdi = 0;
        PersonasPdi pdi = ci -> {
            consultasPdi++;
            if (pdiCaida) {
                throw new ServicioNoDisponibleException(
                    CodigoError.PDI_NO_DISPONIBLE,
                    "PDI caida"
                );
            }
            return persona;
        };
        emisor = new SesionEmisorJwt(jwt, autenticacion, usuarios, pdi, RELOJ);
    }

    private UUID alta(Rol... roles) {
        UUID id = UUID.randomUUID();
        cuentas.put(IDENTIDAD.documento(), new Cuenta(id, Set.of(roles)));
        return id;
    }

    /** El registro falla y, como si otro login lo hubiera creado, aparece la cuenta. */
    private UUID registroConcurrente(RuntimeException falla) {
        UUID id = UUID.randomUUID();
        doAnswer(i -> {
            cuentas.put(
                IDENTIDAD.documento(),
                new Cuenta(id, Set.of(Rol.CHOFER))
            );
            throw falla;
        })
            .when(usuarios)
            .registrarCiudadano(any());
        return id;
    }

    private Identidad identidadDe(String token) {
        return jwt.verificar(token);
    }

    private ExcepcionNegocio rechazo(GubUyIdentity identidad, Rol rol) {
        return assertThrows(ExcepcionNegocio.class, () ->
            emisor.emitirToken(identidad, rol)
        );
    }

    @Test
    void usuarioConUnSoloRolEntraSinIndicarlo() {
        UUID id = alta(Rol.CHOFER);
        Identidad identidad = identidadDe(emisor.emitirToken(IDENTIDAD, null));
        assertEquals(new Identidad(id, Rol.CHOFER), identidad);
        assertEquals(0, consultasPdi, "ya registrado: no se consulta la PDI");
    }

    @Test
    void emitirSesionDevuelveElTokenConSuVencimientoYRol() {
        UUID id = alta(Rol.CHOFER);
        Sesion sesion = emisor.emitirSesion(IDENTIDAD, null);

        assertEquals(Rol.CHOFER, sesion.rol());
        assertEquals(
            new Identidad(id, Rol.CHOFER),
            identidadDe(sesion.token())
        );
        assertEquals(
            sesion.expiraEn().truncatedTo(ChronoUnit.SECONDS),
            jwt.vencimiento(sesion.token())
        );
    }

    @Test
    void conVariosRolesUsaElSolicitado() {
        UUID id = alta(Rol.CHOFER, Rol.ENCARGADO_EMPRESA);
        Identidad identidad = identidadDe(
            emisor.emitirToken(IDENTIDAD, Rol.ENCARGADO_EMPRESA)
        );
        assertEquals(new Identidad(id, Rol.ENCARGADO_EMPRESA), identidad);
    }

    @Test
    void conVariosRolesYSinIndicarCualEsAmbiguo() {
        alta(Rol.CHOFER, Rol.ENCARGADO_EMPRESA);
        assertEquals(
            CodigoError.ROL_REQUERIDO,
            rechazo(IDENTIDAD, null).codigo()
        );
    }

    @Test
    void rolSolicitadoQueNoTieneSeRechaza() {
        alta(Rol.CHOFER);
        assertEquals(
            CodigoError.ROL_NO_ASIGNADO,
            rechazo(IDENTIDAD, Rol.FUNCIONARIO).codigo()
        );
    }

    @Test
    void administradorYAutoridadNuncaEntranPorGubUyAunqueLosTenga() {
        alta(Rol.ADMINISTRADOR, Rol.AUTORIDAD);
        for (Rol rol : new Rol[] { Rol.ADMINISTRADOR, Rol.AUTORIDAD }) {
            assertEquals(
                CodigoError.ROL_NO_PERMITIDO_GUBUY,
                rechazo(IDENTIDAD, rol).codigo()
            );
        }
        // sin pedir rol: sus roles internos no cuentan
        assertEquals(
            CodigoError.USUARIO_SIN_ROLES,
            rechazo(IDENTIDAD, null).codigo()
        );
    }

    @Test
    void administradorQueTambienEsChoferEntraSoloComoChofer() {
        UUID id = alta(Rol.ADMINISTRADOR, Rol.CHOFER);
        assertEquals(
            new Identidad(id, Rol.CHOFER),
            identidadDe(emisor.emitirToken(IDENTIDAD, null))
        );
    }

    @Test
    void primeraAutenticacionRegistraConLosDatosDeLaPdiPeroSinRolNoEntra() {
        assertEquals(
            CodigoError.USUARIO_SIN_ROLES,
            rechazo(IDENTIDAD, Rol.CHOFER).codigo()
        );

        assertEquals(1, registrados.size());
        NuevoUsuario nuevo = registrados.get(0);
        assertEquals("12345678", nuevo.ci());
        assertEquals("Ana", nuevo.nombre());
        assertEquals("Perez", nuevo.apellido());
        assertEquals("persona@example.uy", nuevo.email());
        assertEquals(HOY.minusYears(30), nuevo.fechaNacimiento());

        // la segunda vez ya existe: no se vuelve a registrar ni a consultar la PDI
        rechazo(IDENTIDAD, Rol.CHOFER);
        assertEquals(1, registrados.size());
        assertEquals(1, consultasPdi);
    }

    @Test
    void menorDeEdadNoSeRegistra() {
        persona = Optional.of(
            new DatosPersona("Ana", "Perez", HOY.minusYears(18).plusDays(1))
        );
        assertEquals(
            CodigoError.MENOR_DE_EDAD,
            rechazo(IDENTIDAD, null).codigo()
        );
        assertEquals(0, registrados.size());
    }

    @Test
    void conExactamenteDieciochoAniosSeRegistra() {
        persona = Optional.of(
            new DatosPersona("Ana", "Perez", HOY.minusYears(18))
        );
        // sin rol, pero quedo registrado
        assertEquals(
            CodigoError.USUARIO_SIN_ROLES,
            rechazo(IDENTIDAD, null).codigo()
        );
        assertEquals(1, registrados.size());
    }

    @Test
    void siLaPdiNoConoceLaCedulaNoSeRegistra() {
        persona = Optional.empty();
        assertEquals(
            CodigoError.PERSONA_NO_ENCONTRADA,
            rechazo(IDENTIDAD, null).codigo()
        );
        assertEquals(0, registrados.size());
    }

    @Test
    void sinCorreoNoSeRegistraNiSeConsultaLaPdi() {
        GubUyIdentity sinCorreo = new GubUyIdentity(
            "uid-1",
            "12345678",
            null,
            null
        );
        assertEquals(
            CodigoError.EMAIL_REQUERIDO,
            rechazo(sinCorreo, null).codigo()
        );
        assertEquals(0, consultasPdi);
    }

    @Test
    void siLaPdiNoResponde_elNuevoFallaConNoDisponibleYElExistenteEntra() {
        pdiCaida = true;
        ServicioNoDisponibleException e = assertThrows(
            ServicioNoDisponibleException.class,
            () -> emisor.emitirToken(IDENTIDAD, null)
        );
        assertEquals(CodigoError.PDI_NO_DISPONIBLE, e.codigo());
        assertEquals(0, registrados.size());

        UUID id = alta(Rol.FUNCIONARIO);
        assertEquals(
            new Identidad(id, Rol.FUNCIONARIO),
            identidadDe(emisor.emitirToken(IDENTIDAD, null))
        );
    }

    @Test
    void siOtroLoginLoRegistroAntesDeLaVerificacionUsaEseUsuario() {
        UUID id = registroConcurrente(
            new ConflictoException(CodigoError.CI_EN_USO, "ci repetida")
        );
        assertEquals(
            new Identidad(id, Rol.CHOFER),
            identidadDe(emisor.emitirToken(IDENTIDAD, null))
        );
    }

    @Test
    void siLaConstraintUniqueRechazaElInsertUsaElUsuarioQueCreoElOtro() {
        UUID id = registroConcurrente(new EJBException("unique violada"));
        assertEquals(
            new Identidad(id, Rol.CHOFER),
            identidadDe(emisor.emitirToken(IDENTIDAD, null))
        );
    }

    @Test
    void unErrorDelContenedorSinUsuarioCreadoSePropaga() {
        EJBException falla = new EJBException("base caida");
        doThrow(falla).when(usuarios).registrarCiudadano(any());
        assertSame(
            falla,
            assertThrows(EJBException.class, () ->
                emisor.emitirToken(IDENTIDAD, null)
            )
        );
    }

    @Test
    void siElCorreoLoUsaOtroUsuarioSeInforma() {
        doThrow(
            new ConflictoException(CodigoError.EMAIL_EN_USO, "email repetido")
        )
            .when(usuarios)
            .registrarCiudadano(any());
        assertEquals(
            CodigoError.EMAIL_EN_USO,
            rechazo(IDENTIDAD, null).codigo()
        );
    }
}
