package uy.carga.tse.logica.vehiculo;

import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.ejb.Local;
import java.util.List;
import java.util.UUID;
import uy.carga.tse.datatypes.vehiculo.CrearVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.DetalleItvDto;
import uy.carga.tse.datatypes.vehiculo.DetallePncDto;
import uy.carga.tse.datatypes.vehiculo.DetalleVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ModificarVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ReemplazarItvDto;
import uy.carga.tse.datatypes.vehiculo.ReemplazarPncDto;
import uy.carga.tse.datatypes.vehiculo.ResumenVehiculoDto;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoSubido;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;

@Local
public interface VehiculoServicioLocal {
    DetalleVehiculoDto crearVehiculo(CrearVehiculoDto dto);

    DetalleVehiculoDto modificarVehiculo(UUID id, ModificarVehiculoDto dto);

    void eliminarVehiculo(UUID id);

    DetalleVehiculoDto obtener(UUID id) throws RecursoNoEncontradoException;

    /** Listado paginado. La pagina empieza en 1. */
    Page<ResumenVehiculoDto> listar(
        PageRequest pageRequest,
        List<Sort<Object>> sorts
    );

    /**
     * Listado paginado de los vehiculos de la empresa. Los encargados solo
     * pueden ver los de sus empresas.
     */
    Page<ResumenVehiculoDto> listarPorEmpresa(
        UUID empresaId,
        PageRequest pageRequest,
        List<Sort<Object>> sorts
    );

    /**
     * Crea o reemplaza la ITV del vehiculo. Solo para los encargados de la
     * empresa del vehiculo. El archivo tiene que ser un PDF.
     */
    DetalleItvDto reemplazarItv(
        UUID vehiculoId,
        ReemplazarItvDto dto,
        ArchivoSubido archivo
    );

    /**
     * Crea o reemplaza el PNC del vehiculo. Solo para los encargados de la
     * empresa del vehiculo. El archivo tiene que ser un PDF.
     */
    DetallePncDto reemplazarPnc(
        UUID vehiculoId,
        ReemplazarPncDto dto,
        ArchivoSubido archivo
    );
}
