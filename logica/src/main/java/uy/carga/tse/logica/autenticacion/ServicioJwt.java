package uy.carga.tse.logica.autenticacion;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.EnumMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.SecretKey;
import uy.carga.tse.entidades.enums.Rol;

/**
 * Emite y verifica los JWT de sesion. El token lleva solo el id del usuario
 * ({@code sub}) y el rol activo ({@code rol}); la vigencia depende del rol.
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class ServicioJwt {

    static final int LARGO_MINIMO_SECRETO = 32;
    static final String EMISOR = "cargauy";
    static final String CLAIM_ROL = "rol";
    static final String PREFIJO_VIGENCIA = "JWT_EXPIRACION_MINUTOS_";

    private static final Map<Rol, Duration> VIGENCIA_POR_DEFECTO = Map.of(
        Rol.ADMINISTRADOR,
        Duration.ofMinutes(30),
        Rol.AUTORIDAD,
        Duration.ofHours(2),
        Rol.FUNCIONARIO,
        Duration.ofHours(8),
        Rol.ENCARGADO_EMPRESA,
        Duration.ofHours(8),
        Rol.CHOFER,
        Duration.ofDays(30)
    );

    private SecretKey clave;
    private final Map<Rol, Duration> vigencias = new EnumMap<>(Rol.class);

    @PostConstruct
    void iniciar() {
        iniciar(System::getenv);
    }

    void iniciar(Function<String, String> entorno) {
        String secreto = entorno.apply("JWT_SECRET");
        if (secreto == null || secreto.length() < LARGO_MINIMO_SECRETO) {
            throw new IllegalStateException(
                "JWT_SECRET no esta definida o tiene menos de " +
                    LARGO_MINIMO_SECRETO +
                    " caracteres"
            );
        }
        clave = Keys.hmacShaKeyFor(secreto.getBytes(StandardCharsets.UTF_8));

        for (Rol rol : Rol.values()) {
            vigencias.put(rol, vigencia(rol, entorno));
        }
    }

    private static Duration vigencia(
        Rol rol,
        Function<String, String> entorno
    ) {
        String variable = PREFIJO_VIGENCIA + rol.name();
        String configurada = entorno.apply(variable);
        if (configurada == null || configurada.isBlank()) {
            return VIGENCIA_POR_DEFECTO.get(rol);
        }
        long minutos;
        try {
            minutos = Long.parseLong(configurada.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                variable + " debe ser un entero positivo"
            );
        }
        if (minutos <= 0) {
            throw new IllegalStateException(
                variable + " debe ser un entero positivo"
            );
        }
        return Duration.ofMinutes(minutos);
    }

    public Sesion emitir(UUID usuarioId, Rol rol) {
        Instant ahora = Instant.now();
        Instant expira = ahora.plus(vigencias.get(rol));
        String token = Jwts.builder()
            .issuer(EMISOR)
            .subject(usuarioId.toString())
            .claim(CLAIM_ROL, rol.name())
            .issuedAt(Date.from(ahora))
            .expiration(Date.from(expira))
            .signWith(clave)
            .compact();
        return new Sesion(token, expira, rol);
    }

    public Identidad verificar(String token) {
        Claims claims = leer(token);
        try {
            return new Identidad(
                UUID.fromString(claims.getSubject()),
                Rol.valueOf(claims.get(CLAIM_ROL, String.class))
            );
        } catch (
            JwtException
            | IllegalArgumentException
            | NullPointerException e
        ) {
            throw new TokenInvalidoException(e);
        }
    }

    /**
     * Instante en que vence el token. Verifica la firma y el emisor igual que
     * {@link #verificar}.
     */
    public Instant vencimiento(String token) {
        Date expiracion = leer(token).getExpiration();
        if (expiracion == null) {
            throw new TokenInvalidoException("El token no tiene vencimiento");
        }
        return expiracion.toInstant();
    }

    private Claims leer(String token) {
        try {
            return Jwts.parser()
                .verifyWith(clave)
                .requireIssuer(EMISOR)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new TokenInvalidoException(e);
        }
    }
}
