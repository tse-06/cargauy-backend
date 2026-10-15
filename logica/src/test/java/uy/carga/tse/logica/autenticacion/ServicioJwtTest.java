package uy.carga.tse.logica.autenticacion;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uy.carga.tse.entidades.enums.Rol;

class ServicioJwtTest {

    private static final String SECRETO =
        "secreto-de-prueba-0123456789abcdefghij";

    private final Map<String, String> entorno = new HashMap<>();
    private ServicioJwt servicio;

    @BeforeEach
    void preparar() {
        entorno.put("JWT_SECRET", SECRETO);
        servicio = nuevo();
    }

    private ServicioJwt nuevo() {
        ServicioJwt s = new ServicioJwt();
        s.iniciar(entorno::get);
        return s;
    }

    private static SecretKey clave() {
        return Keys.hmacShaKeyFor(SECRETO.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void emitirYVerificarDevuelveUsuarioYRol() {
        UUID id = UUID.randomUUID();
        Sesion sesion = servicio.emitir(id, Rol.CHOFER);

        assertEquals(
            new Identidad(id, Rol.CHOFER),
            servicio.verificar(sesion.token())
        );
        assertEquals(Rol.CHOFER, sesion.rol());
    }

    @Test
    void vencimientoDevuelveElExpDelToken() {
        Sesion sesion = servicio.emitir(UUID.randomUUID(), Rol.CHOFER);

        // el exp del JWT tiene precision de segundos
        assertEquals(
            sesion.expiraEn().truncatedTo(ChronoUnit.SECONDS),
            servicio.vencimiento(sesion.token())
        );
    }

    @Test
    void vencimientoRechazaUnTokenInvalido() {
        assertThrows(
            TokenInvalidoException.class,
            () -> servicio.vencimiento("basura")
        );
        assertThrows(
            TokenInvalidoException.class,
            () -> servicio.vencimiento(null)
        );
    }

    @Test
    void elTokenLlevaSoloIssSubRolIatYExp() {
        UUID id = UUID.randomUUID();
        String token = servicio.emitir(id, Rol.ADMINISTRADOR).token();

        var payload = Jwts.parser()
            .verifyWith(clave())
            .build()
            .parseSignedClaims(token)
            .getPayload();

        assertEquals(
            Set.of("iss", "sub", "rol", "iat", "exp"),
            payload.keySet()
        );
        assertEquals(id.toString(), payload.getSubject());
        assertEquals("ADMINISTRADOR", payload.get("rol"));
    }

    @Test
    void laVigenciaDependeDelRol() {
        Duration admin = vigencia(
            servicio.emitir(UUID.randomUUID(), Rol.ADMINISTRADOR)
        );
        Duration chofer = vigencia(
            servicio.emitir(UUID.randomUUID(), Rol.CHOFER)
        );

        assertEquals(Duration.ofMinutes(30), admin);
        assertEquals(Duration.ofDays(30), chofer);
        assertTrue(chofer.compareTo(admin) > 0);
    }

    @Test
    void laVigenciaSePuedeConfigurarPorRol() {
        entorno.put("JWT_EXPIRACION_MINUTOS_CHOFER", "90");
        ServicioJwt configurado = nuevo();

        assertEquals(
            Duration.ofMinutes(90),
            vigencia(configurado.emitir(UUID.randomUUID(), Rol.CHOFER))
        );
        assertEquals(
            Duration.ofMinutes(30),
            vigencia(configurado.emitir(UUID.randomUUID(), Rol.ADMINISTRADOR))
        );
    }

    @Test
    void unaVigenciaInvalidaImpideIniciar() {
        for (String invalida : new String[] { "abc", "0", "-5" }) {
            entorno.put("JWT_EXPIRACION_MINUTOS_AUTORIDAD", invalida);
            assertThrows(IllegalStateException.class, this::nuevo);
        }
    }

    @Test
    void unSecretoCortoImpideIniciar() {
        entorno.put("JWT_SECRET", "corto");
        assertThrows(IllegalStateException.class, this::nuevo);
        entorno.remove("JWT_SECRET");
        assertThrows(IllegalStateException.class, this::nuevo);
    }

    @Test
    void rechazaUnTokenAdulterado() {
        String token = servicio.emitir(UUID.randomUUID(), Rol.CHOFER).token();
        String[] partes = token.split("\\.");
        String otroPayload = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(
                (
                    "{\"sub\":\"" +
                    UUID.randomUUID() +
                    "\",\"rol\":\"ADMINISTRADOR\"}"
                ).getBytes(StandardCharsets.UTF_8)
            );
        String adulterado = partes[0] + "." + otroPayload + "." + partes[2];

        assertThrows(TokenInvalidoException.class, () ->
            servicio.verificar(adulterado)
        );
    }

    @Test
    void rechazaTokenFirmadoConOtraClave() {
        SecretKey otra = Keys.hmacShaKeyFor(
            "otra-clave-distinta-0123456789abcdefghij".getBytes(
                StandardCharsets.UTF_8
            )
        );
        String token = armar(
            "cargauy",
            UUID.randomUUID().toString(),
            "CHOFER",
            60_000,
            otra
        );

        assertThrows(TokenInvalidoException.class, () ->
            servicio.verificar(token)
        );
    }

    @Test
    void rechazaEmisorIncorrecto() {
        String token = armar(
            "otro",
            UUID.randomUUID().toString(),
            "CHOFER",
            60_000,
            clave()
        );

        assertThrows(TokenInvalidoException.class, () ->
            servicio.verificar(token)
        );
    }

    @Test
    void rechazaTokenVencido() {
        String token = armar(
            "cargauy",
            UUID.randomUUID().toString(),
            "CHOFER",
            -60_000,
            clave()
        );

        assertThrows(TokenInvalidoException.class, () ->
            servicio.verificar(token)
        );
    }

    @Test
    void rechazaRolDesconocidoOSubjectInvalido() {
        String rolDesconocido = armar(
            "cargauy",
            UUID.randomUUID().toString(),
            "FOO",
            60_000,
            clave()
        );
        String subjectInvalido = armar(
            "cargauy",
            "no-es-uuid",
            "CHOFER",
            60_000,
            clave()
        );

        assertThrows(TokenInvalidoException.class, () ->
            servicio.verificar(rolDesconocido)
        );
        assertThrows(TokenInvalidoException.class, () ->
            servicio.verificar(subjectInvalido)
        );
    }

    @Test
    void rechazaTextoQueNoEsUnJwt() {
        assertThrows(TokenInvalidoException.class, () ->
            servicio.verificar("basura")
        );
        assertThrows(TokenInvalidoException.class, () ->
            servicio.verificar("")
        );
    }

    private static Duration vigencia(Sesion sesion) {
        var payload = Jwts.parser()
            .verifyWith(clave())
            .build()
            .parseSignedClaims(sesion.token())
            .getPayload();
        return Duration.between(
            payload.getIssuedAt().toInstant(),
            payload.getExpiration().toInstant()
        );
    }

    private static String armar(
        String emisor,
        String subject,
        String rol,
        long vigenciaMs,
        SecretKey clave
    ) {
        long ahora = System.currentTimeMillis();
        return Jwts.builder()
            .issuer(emisor)
            .subject(subject)
            .claim("rol", rol)
            .issuedAt(new Date(ahora - 120_000))
            .expiration(new Date(ahora + vigenciaMs))
            .signWith(clave)
            .compact();
    }
}
