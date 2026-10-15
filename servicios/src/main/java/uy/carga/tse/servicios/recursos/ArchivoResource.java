package uy.carga.tse.servicios.recursos;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.Response;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoDescarga;

/**
 * Descarga de archivos por id firmado (ver ArchivoServicioLocal#url). No
 * pide JWT: la firma es la autorizacion, asi la URL sirve en un
 * {@code <a href>} o un {@code <img src>}.
 */
@Path("/archivos")
@RequestScoped
public class ArchivoResource {

    @Inject
    ArchivoServicioLocal servicioArchivos;

    @GET
    @Path("/{idFirmado}")
    public Response descargar(@PathParam("idFirmado") String idFirmado) {
        ArchivoDescarga archivo = servicioArchivos.abrirFirmado(idFirmado);
        // RESTEasy cierra el InputStream despues de escribirlo
        return Response.ok(archivo.contenido(), archivo.tipoMime())
            .header(HttpHeaders.CONTENT_LENGTH, archivo.tamano())
            .header(
                "Content-Disposition",
                disposicion(archivo.seMuestraEnNavegador(), archivo.nombre())
            )
            .header("X-Content-Type-Options", "nosniff")
            .header(HttpHeaders.CACHE_CONTROL, "private")
            .build();
    }

    /**
     * Content-Disposition con el nombre en ASCII (para clientes viejos) y en
     * UTF-8 con {@code filename*} (RFC 6266 / RFC 5987).
     */
    static String disposicion(boolean seMuestraEnNavegador, String nombre) {
        return (
            (seMuestraEnNavegador ? "inline" : "attachment") +
            "; filename=\"" +
            nombreAscii(nombre) +
            "\"; filename*=UTF-8''" +
            URLEncoder.encode(nombre, StandardCharsets.UTF_8)
                .replace("+", "%20")
                .replace("*", "%2A")
        );
    }

    private static String nombreAscii(String nombre) {
        StringBuilder ascii = new StringBuilder(nombre.length());
        for (char c : nombre.toCharArray()) {
            boolean imprimible = c >= 0x20 && c < 0x7f;
            ascii.append(imprimible && c != '"' && c != '\\' ? c : '_');
        }
        return ascii.toString();
    }
}
