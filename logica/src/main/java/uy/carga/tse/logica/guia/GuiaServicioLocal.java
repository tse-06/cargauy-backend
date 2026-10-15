package uy.carga.tse.logica.guia;

import jakarta.ejb.Local;
import uy.carga.tse.datatypes.guia.CrearGuiaDto;
import uy.carga.tse.datatypes.guia.DetalleGuiaDto;

@Local
public interface GuiaServicioLocal {
    /**
     * Crea una guia de la empresa. Solo para los encargados de la empresa. El
     * vehiculo y el chofer son opcionales; si se pasan tienen que ser de la
     * empresa.
     */
    DetalleGuiaDto crearGuia(CrearGuiaDto dto);
}
