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
import java.util.Set;
import java.util.UUID;
import uy.carga.tse.datatypes.paginacion.PaginaDto;
import uy.carga.tse.datatypes.paginacion.PaginacionParams;
import uy.carga.tse.datatypes.vehiculo.CrearVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.DetalleItvDto;
import uy.carga.tse.datatypes.vehiculo.DetallePncDto;
import uy.carga.tse.datatypes.vehiculo.DetalleVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ModificarVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ReemplazarItvDto;
import uy.carga.tse.datatypes.vehiculo.ReemplazarPncDto;
import uy.carga.tse.datatypes.vehiculo.ResumenVehiculoDto;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoSubido;
import uy.carga.tse.logica.vehiculo.VehiculoServicioLocal;

@RolesAllowed({ Roles.ADMINISTRADOR, Roles.AUTORIDAD })
@Path("/vehiculos")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class VehiculoResource {

    static final Set<String> CAMPOS_ORDENABLES = Set.of(
        "matricula",
        "marca",
        "modelo",
        "peso",
        "capacidad"
    );

    @Inject
    VehiculoServicioLocal vehiculoServicio;

    @Context
    UriInfo uriInfo;

    /**
     * Todos los vehiculos. Los de una empresa se listan en
     * {@link VehiculoEmpresaResource}.
     */
    @GET
    public PaginaDto<ResumenVehiculoDto> listar(
        @BeanParam @Valid PaginacionParams params
    ) {
        return PaginaDto.de(
            vehiculoServicio.listar(
                params.toPageRequest(),
                params.toSorts(CAMPOS_ORDENABLES)
            )
        );
    }

    @GET
    @Path("/{id}")
    public DetalleVehiculoDto obtener(@PathParam("id") UUID id) {
        return vehiculoServicio.obtener(id);
    }

    @POST
    public Response crear(@Valid CrearVehiculoDto dto) {
        DetalleVehiculoDto creado = vehiculoServicio.crearVehiculo(dto);
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
    public DetalleVehiculoDto modificar(
        @PathParam("id") UUID id,
        @Valid ModificarVehiculoDto dto
    ) {
        return vehiculoServicio.modificarVehiculo(id, dto);
    }

    @DELETE
    @Path("/{id}")
    public Response eliminar(@PathParam("id") UUID id) {
        vehiculoServicio.eliminarVehiculo(id);
        return Response.noContent().build();
    }

    /**
     * Crea o reemplaza la ITV del vehiculo (multipart: fechaVencimiento en
     * formato yyyy-MM-dd y archivo PDF). Solo para los encargados de la
     * empresa del vehiculo.
     */
    @PUT
    @Path("/{id}/itv")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed(Roles.ENCARGADO_EMPRESA)
    public DetalleItvDto reemplazarItv(
        @PathParam("id") UUID id,
        @BeanParam @Valid ReemplazarItvDto dto,
        @FormParam("archivo") @NotNull EntityPart archivo
    ) {
        return vehiculoServicio.reemplazarItv(id, dto, subido(archivo));
    }

    /**
     * Crea o reemplaza el PNC del vehiculo (multipart: numero,
     * fechaVencimiento en formato yyyy-MM-dd y archivo PDF). Solo para los
     * encargados de la empresa del vehiculo.
     */
    @PUT
    @Path("/{id}/pnc")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @RolesAllowed(Roles.ENCARGADO_EMPRESA)
    public DetallePncDto reemplazarPnc(
        @PathParam("id") UUID id,
        @BeanParam @Valid ReemplazarPncDto dto,
        @FormParam("archivo") @NotNull EntityPart archivo
    ) {
        return vehiculoServicio.reemplazarPnc(id, dto, subido(archivo));
    }

    private static ArchivoSubido subido(EntityPart archivo) {
        return new ArchivoSubido(
            archivo.getFileName().orElse(null),
            archivo.getContent()
        );
    }
}
