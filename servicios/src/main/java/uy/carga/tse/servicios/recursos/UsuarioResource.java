package uy.carga.tse.servicios.recursos;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.Set;
import java.util.UUID;
import uy.carga.tse.datatypes.paginacion.PaginaDto;
import uy.carga.tse.datatypes.paginacion.PaginacionParams;
import uy.carga.tse.datatypes.usuario.CrearUsuarioDto;
import uy.carga.tse.datatypes.usuario.DetalleUsuarioDto;
import uy.carga.tse.datatypes.usuario.ModificarUsuarioDto;
import uy.carga.tse.datatypes.usuario.ResumenUsuarioDto;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.usuario.UsuarioServicioLocal;

@RolesAllowed(Roles.ADMINISTRADOR)
@Path("/usuarios")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class UsuarioResource {

    private static final Set<String> CAMPOS_ORDENABLES = Set.of(
        "ci",
        "nombre",
        "apellido",
        "email"
    );

    @Inject
    UsuarioServicioLocal usuarioServicio;

    @Context
    UriInfo uriInfo;

    @GET
    public PaginaDto<ResumenUsuarioDto> listar(
        @BeanParam @Valid PaginacionParams params
    ) {
        return PaginaDto.de(
            usuarioServicio.listar(
                params.toPageRequest(),
                params.toSorts(CAMPOS_ORDENABLES)
            )
        );
    }

    @GET
    @Path("/{id}")
    public DetalleUsuarioDto obtener(@PathParam("id") UUID id) {
        return usuarioServicio.obtener(id);
    }

    @POST
    public Response crear(@Valid CrearUsuarioDto dto) {
        DetalleUsuarioDto creado = usuarioServicio.crearUsuarioInterno(dto);
        return Response.created(
            uriInfo
                .getAbsolutePathBuilder()
                .path(creado.id().toString())
                .build()
        )
            .entity(creado)
            .build();
    }

    @PUT
    @Path("/{id}")
    public DetalleUsuarioDto modificar(
        @PathParam("id") UUID id,
        @Valid ModificarUsuarioDto dto
    ) {
        return usuarioServicio.modificarUsuario(id, dto);
    }

    @PUT
    @Path("/{id}/baja")
    public DetalleUsuarioDto darDeBaja(@PathParam("id") UUID id) {
        return usuarioServicio.darDeBaja(id);
    }

    @PUT
    @Path("/{id}/alta")
    public DetalleUsuarioDto darDeAlta(@PathParam("id") UUID id) {
        return usuarioServicio.darDeAlta(id);
    }
}
