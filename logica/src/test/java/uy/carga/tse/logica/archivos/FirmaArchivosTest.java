package uy.carga.tse.logica.archivos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.logica.comun.excepciones.AccesoDenegadoException;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;

class FirmaArchivosTest {

    static final String SECRETO = secreto(1);
    static final String URL_BASE = "http://localhost:8080/api";
    private static final Instant AHORA = Instant.parse("2026-10-11T12:00:00Z");

    @Test
    void firmaYVerificaElId() {
        FirmaArchivos firma = firma(entorno());
        UUID archivoId = UUID.randomUUID();

        assertEquals(archivoId, firma.verificar(firma.firmar(archivoId)));
    }

    @Test
    void rechazaUnaFirmaAlterada() {
        FirmaArchivos firma = firma(entorno());
        String idFirmado = firma.firmar(UUID.randomUUID());
        char ultimo = idFirmado.charAt(idFirmado.length() - 2);
        String alterado =
            idFirmado.substring(0, idFirmado.length() - 2) +
            (ultimo == 'A' ? 'B' : 'A') +
            idFirmado.charAt(idFirmado.length() - 1);

        assertInvalido(firma, alterado);
    }

    @Test
    void rechazaUnIdAjenoConFirmaValida() {
        FirmaArchivos firma = firma(entorno());
        String[] propio = firma.firmar(UUID.randomUUID()).split("\\.");
        String[] ajeno = firma.firmar(UUID.randomUUID()).split("\\.");

        assertInvalido(firma, ajeno[0] + "." + propio[1]);
    }

    @Test
    void rechazaIdsMalformados() {
        FirmaArchivos firma = firma(entorno());
        String valido = firma.firmar(UUID.randomUUID());
        String[] partes = valido.split("\\.");
        String payloadCorto = Base64.getUrlEncoder()
            .withoutPadding()
            .encodeToString(new byte[16]);

        for (String id : Arrays.asList(
            null,
            "",
            ".",
            "abc",
            valido + ".x",
            "%%%." + partes[1],
            partes[0] + ".%%%",
            payloadCorto + "." + partes[1]
        )) {
            assertInvalido(firma, id);
        }
    }

    @Test
    void rechazaFirmasDeOtroSecreto() {
        Map<String, String> otro = entorno();
        otro.put(FirmaArchivos.VARIABLE_SECRETO, secreto(2));
        String idFirmado = firma(otro).firmar(UUID.randomUUID());

        assertInvalido(firma(entorno()), idFirmado);
    }

    @Test
    void rechazaUnEnlaceVencido() {
        FirmaArchivos firma = firma(entorno());
        String idFirmado = firma.firmar(UUID.randomUUID());
        firma.reloj = reloj(AHORA.plus(FirmaArchivos.VIGENCIA_POR_DEFECTO));

        AccesoDenegadoException e = assertThrows(
            AccesoDenegadoException.class,
            () -> firma.verificar(idFirmado)
        );
        assertEquals(CodigoError.ENLACE_ARCHIVO_VENCIDO, e.codigo());
    }

    @Test
    void aceptaElEnlaceHastaQueVence() {
        FirmaArchivos firma = firma(entorno());
        UUID archivoId = UUID.randomUUID();
        String idFirmado = firma.firmar(archivoId);
        firma.reloj = reloj(
            AHORA.plus(FirmaArchivos.VIGENCIA_POR_DEFECTO).minusSeconds(1)
        );

        assertEquals(archivoId, firma.verificar(idFirmado));
    }

    @Test
    void usaLaVigenciaConfigurada() {
        Map<String, String> entorno = entorno();
        entorno.put(FirmaArchivos.VARIABLE_VIGENCIA, " 60 ");
        FirmaArchivos firma = firma(entorno);
        UUID archivoId = UUID.randomUUID();
        String idFirmado = firma.firmar(archivoId);

        firma.reloj = reloj(AHORA.plus(Duration.ofMinutes(59)));
        assertEquals(archivoId, firma.verificar(idFirmado));

        firma.reloj = reloj(AHORA.plus(Duration.ofMinutes(60)));
        assertThrows(AccesoDenegadoException.class, () ->
            firma.verificar(idFirmado)
        );
    }

    @Test
    void urlBaseSinBarraFinal() {
        Map<String, String> entorno = entorno();
        entorno.put(FirmaArchivos.VARIABLE_URL_BASE, " " + URL_BASE + "// ");

        assertEquals(URL_BASE, firma(entorno).urlBase());
    }

    @Test
    void iniciarFallaSinSecreto() {
        Map<String, String> entorno = entorno();
        entorno.remove(FirmaArchivos.VARIABLE_SECRETO);

        assertFallaAlIniciar(entorno);
    }

    @Test
    void iniciarFallaConSecretoInvalido() {
        Map<String, String> entorno = entorno();
        entorno.put(
            FirmaArchivos.VARIABLE_SECRETO,
            Base64.getEncoder().encodeToString(new byte[31])
        );
        assertFallaAlIniciar(entorno);

        entorno.put(FirmaArchivos.VARIABLE_SECRETO, "no es base64!");
        assertFallaAlIniciar(entorno);
    }

    @Test
    void iniciarFallaSinUrlBase() {
        Map<String, String> entorno = entorno();
        entorno.put(FirmaArchivos.VARIABLE_URL_BASE, " ");

        assertFallaAlIniciar(entorno);
    }

    @Test
    void iniciarFallaConVigenciaInvalida() {
        Map<String, String> entorno = entorno();
        for (String vigencia : new String[] { "0", "-5", "diez" }) {
            entorno.put(FirmaArchivos.VARIABLE_VIGENCIA, vigencia);
            assertFallaAlIniciar(entorno);
        }
    }

    /** Variables minimas para iniciar; mutable para que cada test la ajuste. */
    static Map<String, String> entorno() {
        Map<String, String> entorno = new HashMap<>();
        entorno.put(FirmaArchivos.VARIABLE_SECRETO, SECRETO);
        entorno.put(FirmaArchivos.VARIABLE_URL_BASE, URL_BASE);
        return entorno;
    }

    /** Firma iniciada con el entorno y el reloj fijo en {@link #AHORA}. */
    static FirmaArchivos firma(Map<String, String> entorno) {
        FirmaArchivos firma = new FirmaArchivos();
        firma.iniciar(entorno::get);
        firma.reloj = reloj(AHORA);
        return firma;
    }

    private static Clock reloj(Instant instante) {
        return Clock.fixed(instante, ZoneOffset.UTC);
    }

    private static String secreto(int relleno) {
        byte[] bytes = new byte[32];
        Arrays.fill(bytes, (byte) relleno);
        return Base64.getEncoder().encodeToString(bytes);
    }

    private static void assertInvalido(FirmaArchivos firma, String idFirmado) {
        RecursoNoEncontradoException e = assertThrows(
            RecursoNoEncontradoException.class,
            () -> firma.verificar(idFirmado),
            () -> "Deberia rechazar " + idFirmado
        );
        assertEquals(CodigoError.ARCHIVO_NO_ENCONTRADO, e.codigo());
    }

    private static void assertFallaAlIniciar(Map<String, String> entorno) {
        assertThrows(IllegalStateException.class, () ->
            new FirmaArchivos().iniciar(entorno::get)
        );
    }
}
