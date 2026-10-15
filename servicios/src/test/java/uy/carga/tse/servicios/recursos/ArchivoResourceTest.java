package uy.carga.tse.servicios.recursos;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Solo los headers: armar la Response necesita la implementacion de JAX-RS,
 * que la da el servidor.
 */
class ArchivoResourceTest {

    @Test
    void disposicionParaMostrarYParaDescargar() {
        assertEquals(
            "inline; filename=\"itv.pdf\"; filename*=UTF-8''itv.pdf",
            ArchivoResource.disposicion(true, "itv.pdf")
        );
        assertEquals(
            "attachment; filename=\"pagina.html\"; filename*=UTF-8''pagina.html",
            ArchivoResource.disposicion(false, "pagina.html")
        );
    }

    @Test
    void disposicionEscapaComillasYBarras() {
        assertEquals(
            "attachment; filename=\"a_b_c.txt\"; filename*=UTF-8''a%22b%5Cc.txt",
            ArchivoResource.disposicion(false, "a\"b\\c.txt")
        );
    }

    @Test
    void disposicionCodificaNombresNoAscii() {
        assertEquals(
            "inline; filename=\"gu_a de carga*.pdf\";" +
                " filename*=UTF-8''gu%C3%ADa%20de%20carga%2A.pdf",
            ArchivoResource.disposicion(true, "guía de carga*.pdf")
        );
    }
}
