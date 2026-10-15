package uy.carga.tse.servicios.recursos;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.BeanParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import java.util.UUID;
import uy.carga.tse.datatypes.paginacion.PaginaDto;
import uy.carga.tse.datatypes.paginacion.PaginacionParams;
import uy.carga.tse.datatypes.vehiculo.ResumenVehiculoDto;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.vehiculo.VehiculoServicioLocal;

/**
 * Vehiculos de una empresa. Los encargados solo pueden ver los de sus
 * empresas (se chequea en la logica).
 */
@RolesAllowed({ Roles.ADMINISTRADOR, Roles.AUTORIDAD, Roles.ENCARGADO_EMPRESA })
@Path("/empresas/{empresaId}/vehiculos")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
public class VehiculoEmpresaResource {

    @Inject
    VehiculoServicioLocal vehiculoServicio;

    @GET
    public PaginaDto<ResumenVehiculoDto> listar(
        @PathParam("empresaId") UUID empresaId,
        @BeanParam @Valid PaginacionParams params
    ) {
        return PaginaDto.de(
            vehiculoServicio.listarPorEmpresa(
                empresaId,
                params.toPageRequest(),
                params.toSorts(VehiculoResource.CAMPOS_ORDENABLES)
            )
        );
    }
}
