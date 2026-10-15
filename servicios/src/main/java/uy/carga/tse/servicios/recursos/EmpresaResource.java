package uy.carga.tse.servicios.recursos;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.EntityPart;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import uy.carga.tse.datatypes.empresa.CrearEmpresaDto;
import uy.carga.tse.datatypes.empresa.DetalleEmpresaDto;
import uy.carga.tse.datatypes.empresa.ModificarEmpresaDto;
import uy.carga.tse.datatypes.empresa.ResumenEmpresaDto;
import uy.carga.tse.datatypes.paginacion.PaginaDto;
import uy.carga.tse.datatypes.paginacion.PaginacionParams;
import uy.carga.tse.datatypes.permiso.DetallePermisoDto;
import uy.carga.tse.datatypes.permiso.ReemplazarPermisoDto;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoSubido;
import uy.carga.tse.logica.empresa.EmpresaServicioLocal;

@RolesAllowed({ Roles.ADMINISTRADOR, Roles.AUTORIDAD })
@Path("/empresas")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class EmpresaResource {

    private static final Set<String> CAMPOS_ORDENABLES = Set.of(
        "nombre",
        "razonSocial",
        "numero",
        "rubro"
    );

    @Inject
    EmpresaServicioLocal empresaServicio;

    @Context
    UriInfo uriInfo;

    @GET
    public PaginaDto<ResumenEmpresaDto> listar(
        @BeanParam @Valid PaginacionParams params
    ) {
        return PaginaDto.de(
            empresaServicio.listar(
                params.toPageRequest(),
                params.toSorts(CAMPOS_ORDENABLES)
            )
        );
    }

    /** Empresas del encargado, para elegir con cual trabajar. */
    @GET
    @Path("/mias")
    @RolesAllowed(Roles.ENCARGADO_EMPRESA)
    public List<ResumenEmpresaDto> listarMias() {
        return empresaServicio.listarDelEncargado();
    }

    @GET
    @Path("/{id}")
    public DetalleEmpresaDto obtener(@PathParam("id") UUID id) {
        return empresaServicio.obtener(id);
    }

    @POST
    public Response crear(@Valid CrearEmpresaDto dto) {
        DetalleEmpresaDto creada = empresaServicio.crearEmpresa(dto);
        return Response.created(
            uriInfo
                .getAbsolutePathBuilder()
                .path(creada.id().toString())
                .build()
        )
            .entity(creada)
            .build();
    }

    @PUT
    @Path("/{id}")
    public DetalleEmpresaDto modificar(
        @PathParam("id") UUID id,
        @Valid ModificarEmpresaDto dto
    ) {
        return empresaServicio.modificarEmpresa(id, dto);
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") UUID id) {
        empresaServicio.eliminarEmpresa(id);
        return Response.noContent().build();
    }

    /**
     * Crea o reemplaza el permiso de la empresa (multipart: codigoValidacion,
     * fechaVencimiento en formato yyyy-MM-dd y archivo PDF). Solo para los
     * encargados de la empresa.
     */
    @PUT
    @Path("/{id}/permiso")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed(Roles.ENCARGADO_EMPRESA)
    public DetallePermisoDto reemplazarPermiso(
        @PathParam("id") UUID id,
        @BeanParam @Valid ReemplazarPermisoDto dto,
        @FormParam("archivo") @NotNull EntityPart archivo
    ) {
        return empresaServicio.reemplazarPermiso(
            id,
            dto,
            new ArchivoSubido(
                archivo.getFileName().orElse(null),
                archivo.getContent()
            )
        );
    }
}
