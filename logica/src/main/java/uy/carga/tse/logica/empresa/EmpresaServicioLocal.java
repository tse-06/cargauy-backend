package uy.carga.tse.logica.empresa;

import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.ejb.Local;
import java.util.List;
import java.util.UUID;
import uy.carga.tse.datatypes.empresa.CrearEmpresaDto;
import uy.carga.tse.datatypes.empresa.DetalleEmpresaDto;
import uy.carga.tse.datatypes.empresa.ModificarEmpresaDto;
import uy.carga.tse.datatypes.empresa.ResumenEmpresaDto;
import uy.carga.tse.datatypes.permiso.DetallePermisoDto;
import uy.carga.tse.datatypes.permiso.ReemplazarPermisoDto;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoSubido;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;

@Local
public interface EmpresaServicioLocal {
    DetalleEmpresaDto crearEmpresa(CrearEmpresaDto dto);

    DetalleEmpresaDto modificarEmpresa(UUID id, ModificarEmpresaDto dto);

    void eliminarEmpresa(UUID id);

    DetalleEmpresaDto obtener(UUID id) throws RecursoNoEncontradoException;

    /** Listado paginado. La pagina empieza en 1. */
    Page<ResumenEmpresaDto> listar(
        PageRequest pageRequest,
        List<Sort<Object>> sorts
    );

    /** Empresas que gestiona el encargado que llama, por nombre. */
    List<ResumenEmpresaDto> listarDelEncargado();

    /**
     * Crea o reemplaza el permiso de la empresa. Solo para sus encargados. El
     * archivo tiene que ser un PDF.
     */
    DetallePermisoDto reemplazarPermiso(
        UUID empresaId,
        ReemplazarPermisoDto dto,
        ArchivoSubido archivo
    );
}
