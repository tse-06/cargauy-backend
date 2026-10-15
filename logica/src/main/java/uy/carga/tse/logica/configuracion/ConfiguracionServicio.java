package uy.carga.tse.logica.configuracion;

import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import uy.carga.tse.datatypes.configuracion.DetalleConfiguracionFiscalizacionDto;
import uy.carga.tse.datatypes.configuracion.ModificarConfiguracionFiscalizacionDto;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.entidades.ConfiguracionSistema;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;

@Stateless
public class ConfiguracionServicio implements ConfiguracionServicioLocal {

    @Inject
    ConfiguracionRepositorio configuracionRepositorio;

    @Inject
    ConfiguracionMapper configuracionMapper;

    @Override
    public DetalleConfiguracionFiscalizacionDto obtenerFiscalizacion() {
        return configuracionMapper.aDetalle(buscar());
    }

    @Override
    public DetalleConfiguracionFiscalizacionDto modificarFiscalizacion(
        ModificarConfiguracionFiscalizacionDto dto
    ) {
        ConfiguracionSistema configuracion = buscar();
        configuracionMapper.actualizar(dto, configuracion);
        return configuracionMapper.aDetalle(
            configuracionRepositorio.update(configuracion)
        );
    }

    /** La configuracion es un singleton: la unica fila de la tabla. */
    private ConfiguracionSistema buscar() {
        return configuracionRepositorio
            .findAll()
            .findFirst()
            .orElseThrow(() ->
                new RecursoNoEncontradoException(
                    CodigoError.CONFIGURACION_NO_ENCONTRADA,
                    "La configuracion del sistema no esta cargada"
                )
            );
    }
}
