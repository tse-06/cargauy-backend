package uy.carga.tse.logica.configuracion;

import jakarta.ejb.Local;
import uy.carga.tse.datatypes.configuracion.DetalleConfiguracionFiscalizacionDto;
import uy.carga.tse.datatypes.configuracion.ModificarConfiguracionFiscalizacionDto;

@Local
public interface ConfiguracionServicioLocal {
    DetalleConfiguracionFiscalizacionDto obtenerFiscalizacion();

    DetalleConfiguracionFiscalizacionDto modificarFiscalizacion(
        ModificarConfiguracionFiscalizacionDto dto
    );
}
