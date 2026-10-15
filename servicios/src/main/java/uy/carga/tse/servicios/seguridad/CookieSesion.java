package uy.carga.tse.servicios.seguridad;

import jakarta.ws.rs.core.NewCookie;
import java.time.Duration;
import java.time.Instant;
import java.util.function.Function;

/**
 * Cookie con el JWT de la sesion web. La emiten el login (POST /auth/login) y
 * el callback de gub.uy cuando vuelve a un front web, y la borra el logout.
 * <p>
 * Es httpOnly: el JavaScript del front nunca ve el token, solo el navegador
 * lo manda en cada llamada ({@link MecanismoJwt} lo lee). SameSite=Lax y el
 * encabezado exigido por {@link FiltroCsrf} defienden de CSRF. El Path es el
 * context-root de la API, asi no viaja a los servidores de los fronts. Max-Age
 * coincide con el vencimiento del token. La app movil no la usa: sigue con
 * {@code Authorization: Bearer}.
 * <p>
 * Secure se controla con {@code GUBUY_COOKIE_SECURE} (por defecto true), la
 * misma variable de la cookie de flujo de gub.uy: solo se desactiva en
 * desarrollo local sin HTTPS. Se lee aca y no de GubUyConfig para que el login
 * interno no dependa de que gub.uy este configurado.
 */
public final class CookieSesion {

    public static final String NOMBRE = "cargauy_token";

    static final String RUTA = "/api";

    private static final boolean SECURE = secure(System::getenv);

    private CookieSesion() {}

    public static NewCookie emitir(String token, Instant expiraEn) {
        return construir(token, segundosHasta(expiraEn, Instant.now()));
    }

    /** Cookie vencida con el mismo nombre y Path, que el navegador descarta. */
    public static NewCookie borrar() {
        return construir("", 0);
    }

    static boolean secure(Function<String, String> entorno) {
        String valor = entorno.apply("GUBUY_COOKIE_SECURE");
        return Boolean.parseBoolean(
            valor == null || valor.isBlank() ? "true" : valor
        );
    }

    /** Segundos de vida que le quedan al token; 0 si ya vencio. */
    static int segundosHasta(Instant expiraEn, Instant ahora) {
        long segundos = Duration.between(ahora, expiraEn).getSeconds();
        return (int) Math.max(0, Math.min(segundos, Integer.MAX_VALUE));
    }

    private static NewCookie construir(String valor, int maxAgeSegundos) {
        return new NewCookie.Builder(NOMBRE)
            .value(valor)
            .path(RUTA)
            .maxAge(maxAgeSegundos)
            .httpOnly(true)
            .secure(SECURE)
            .sameSite(NewCookie.SameSite.LAX)
            .build();
    }
}
