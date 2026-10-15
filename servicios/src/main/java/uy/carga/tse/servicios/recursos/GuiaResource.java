package uy.carga.tse.servicios.recursos;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import uy.carga.tse.datatypes.guia.CrearGuiaDto;
import uy.carga.tse.datatypes.guia.DetalleGuiaDto;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.guia.GuiaServicioLocal;

@RolesAllowed(Roles.ENCARGADO_EMPRESA)
@Path("/guias")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class GuiaResource {

    @Inject
    GuiaServicioLocal guiaServicio;

    @Context
    UriInfo uriInfo;

    /** Solo para los encargados de la empresa de la guia. */
    @POST
    public Response crear(@Valid CrearGuiaDto dto) {
        DetalleGuiaDto creada = guiaServicio.crearGuia(dto);
        return Response.created(
            uriInfo
                .getAbsolutePathBuilder()
                .path(creada.id().toString())
                .build()
        )
            .entity(creada)
            .build();
    }
}
