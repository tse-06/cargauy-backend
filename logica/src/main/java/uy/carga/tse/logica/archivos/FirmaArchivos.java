package uy.carga.tse.logica.archivos;

import jakarta.annotation.PostConstruct;
import jakarta.ejb.Lock;
import jakarta.ejb.LockType;
import jakarta.ejb.Singleton;
import jakarta.ejb.Startup;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import java.util.function.Function;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;

/**
 * Firma con HMAC-SHA256 los ids de archivo que viajan en las URLs de
 * descarga, para poder usarlas sin JWT. El id firmado es
 * {@code base64url(payload).base64url(hmac)}, donde el payload son los 16
 * bytes del UUID y los 8 del vencimiento (segundos epoch).
 */
@Singleton
@Startup
@Lock(LockType.READ)
public class FirmaArchivos {

    static final String VARIABLE_SECRETO = "ARCHIVOS_URL_SECRETO";
    static final String VARIABLE_VIGENCIA = "ARCHIVOS_URL_VIGENCIA_MINUTOS";
    static final String VARIABLE_URL_BASE = "ARCHIVOS_URL_BASE";
    static final int LARGO_MINIMO_SECRETO = 32;
    static final Duration VIGENCIA_POR_DEFECTO = Duration.ofMinutes(5);

    private static final String ALGORITMO = "HmacSHA256";
    // Separa la firma de otros usos que pueda tener la misma clave
    private static final byte[] PROPOSITO = "archivo:".getBytes(
        StandardCharsets.US_ASCII
    );
    // UUID (2 longs) + vencimiento (1 long)
    private static final int LARGO_PAYLOAD = 3 * Long.BYTES;
    private static final Base64.Encoder CODIFICADOR =
        Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();

    Clock reloj = Clock.systemUTC();

    private SecretKeySpec clave;
    private Duration vigencia;
    private String urlBase;

    @PostConstruct
    void iniciar() {
        iniciar(System::getenv);
    }

    void iniciar(Function<String, String> entorno) {
        clave = new SecretKeySpec(secreto(entorno), ALGORITMO);
        vigencia = vigencia(entorno);
        urlBase = urlBase(entorno);
    }

    /** URL de la API sin barra final (ej. {@code http://localhost:8080/api}). */
    public String urlBase() {
        return urlBase;
    }

    /** Id firmado que vence dentro de la vigencia configurada. */
    public String firmar(UUID archivoId) {
        Instant vence = reloj.instant().plus(vigencia);
        byte[] payload = ByteBuffer.allocate(LARGO_PAYLOAD)
            .putLong(archivoId.getMostSignificantBits())
            .putLong(archivoId.getLeastSignificantBits())
            .putLong(vence.getEpochSecond())
            .array();
        return (
            CODIFICADOR.encodeToString(payload) +
            "." +
            CODIFICADOR.encodeToString(hmac(payload))
        );
    }

    /**
     * Devuelve el id del archivo si la firma es valida y no vencio. Un id
     * malformado o con firma invalida se trata como inexistente, sin decir
     * por que.
     */
    public UUID verificar(String idFirmado) {
        byte[] payload = payloadFirmado(idFirmado);
        ByteBuffer datos = ByteBuffer.wrap(payload);
        UUID archivoId = new UUID(datos.getLong(), datos.getLong());
        Instant vence = Instant.ofEpochSecond(datos.getLong());
        if (!reloj.instant().isBefore(vence)) {
            throw new AccesoDenegadoException(
                CodigoError.ENLACE_ARCHIVO_VENCIDO,
                "El enlace del archivo " + archivoId + " vencio"
            );
        }
        return archivoId;
    }

    private byte[] payloadFirmado(String idFirmado) {
        if (idFirmado == null) {
            throw invalido();
        }
        String[] partes = idFirmado.split("\\.", -1);
        if (partes.length != 2) {
            throw invalido();
        }
        byte[] payload;
        byte[] firma;
        try {
            payload = DECODIFICADOR.decode(partes[0]);
            firma = DECODIFICADOR.decode(partes[1]);
        } catch (IllegalArgumentException e) {
            throw invalido();
        }
        if (
            payload.length != LARGO_PAYLOAD ||
            !MessageDigest.isEqual(hmac(payload), firma)
        ) {
            throw invalido();
        }
        return payload;
    }

    private byte[] hmac(byte[] payload) {
        try {
            // Mac no es thread-safe: una instancia por llamada
            Mac mac = Mac.getInstance(ALGORITMO);
            mac.init(clave);
            mac.update(PROPOSITO);
            return mac.doFinal(payload);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo calcular el HMAC", e);
        }
    }

    private static RecursoNoEncontradoException invalido() {
        return new RecursoNoEncontradoException(
            CodigoError.ARCHIVO_NO_ENCONTRADO,
            "Enlace de archivo invalido"
        );
    }

    private static byte[] secreto(Function<String, String> entorno) {
        String configurado = entorno.apply(VARIABLE_SECRETO);
        if (configurado == null || configurado.isBlank()) {
            throw new IllegalStateException(
                VARIABLE_SECRETO + " no esta definida"
            );
        }
        byte[] secreto;
        try {
            secreto = Base64.getDecoder().decode(configurado.trim());
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                VARIABLE_SECRETO + " tiene que estar en base64"
            );
        }
        if (secreto.length < LARGO_MINIMO_SECRETO) {
            throw new IllegalStateException(
                VARIABLE_SECRETO +
                    " tiene que tener al menos " +
                    LARGO_MINIMO_SECRETO +
                    " bytes"
            );
        }
        return secreto;
    }

    private static Duration vigencia(Function<String, String> entorno) {
        String configurada = entorno.apply(VARIABLE_VIGENCIA);
        if (configurada == null || configurada.isBlank()) {
            return VIGENCIA_POR_DEFECTO;
        }
        long minutos;
        try {
            minutos = Long.parseLong(configurada.trim());
        } catch (NumberFormatException e) {
            throw new IllegalStateException(
                VARIABLE_VIGENCIA + " debe ser un entero positivo"
            );
        }
        if (minutos <= 0) {
            throw new IllegalStateException(
                VARIABLE_VIGENCIA + " debe ser un entero positivo"
            );
        }
        return Duration.ofMinutes(minutos);
    }

    private static String urlBase(Function<String, String> entorno) {
        String configurada = entorno.apply(VARIABLE_URL_BASE);
        if (configurada == null || configurada.isBlank()) {
            throw new IllegalStateException(
                VARIABLE_URL_BASE + " no esta definida"
            );
        }
        String url = configurada.trim();
        while (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        return url;
    }
}
