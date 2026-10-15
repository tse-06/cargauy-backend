package uy.carga.tse.servicios.recursos;

import jakarta.annotation.security.RolesAllowed;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import uy.carga.tse.datatypes.configuracion.DetalleConfiguracionFiscalizacionDto;
import uy.carga.tse.datatypes.configuracion.ModificarConfiguracionFiscalizacionDto;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.configuracion.ConfiguracionServicioLocal;

@RolesAllowed(Roles.ADMINISTRADOR)
@Path("/configuracion-fiscalizacion")
@RequestScoped
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ConfiguracionResource {

    @Inject
    ConfiguracionServicioLocal configuracionServicio;

    @GET
    public DetalleConfiguracionFiscalizacionDto obtener() {
        return configuracionServicio.obtenerFiscalizacion();
    }

    @PUT
    public DetalleConfiguracionFiscalizacionDto modificar(
        @Valid ModificarConfiguracionFiscalizacionDto dto
    ) {
        return configuracionServicio.modificarFiscalizacion(dto);
    }
}
