package uy.carga.tse.logica.empresa;

import jakarta.annotation.Resource;
import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import uy.carga.tse.datatypes.empresa.CrearEmpresaDto;
import uy.carga.tse.datatypes.empresa.DetalleEmpresaDto;
import uy.carga.tse.datatypes.empresa.ModificarEmpresaDto;
import uy.carga.tse.datatypes.empresa.ResumenEmpresaDto;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.permiso.DetallePermisoDto;
import uy.carga.tse.datatypes.permiso.ReemplazarPermisoDto;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.Permiso;
import uy.carga.tse.logica.archivos.Adjuntos;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoSubido;
import uy.carga.tse.logica.comun.Fechas;
import uy.carga.tse.logica.comun.Persistencia;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;
import uy.carga.tse.logica.usuario.EncargadoEmpresaRepositorio;

@Stateless
public class EmpresaServicio implements EmpresaServicioLocal {

    static final String TIPO_MIME_PERMISO = "application/pdf";

    @Inject
    EmpresaRepositorio empresaRepositorio;

    @Inject
    PermisoRepositorio permisoRepositorio;

    @Inject
    EncargadoEmpresaRepositorio encargadoRepositorio;

    @Inject
    ArchivoServicioLocal servicioArchivos;

    @Inject
    AccesoEmpresa accesoEmpresa;

    @Inject
    EmpresaMapper empresaMapper;

    // El caller es el id del usuario y su unico rol el activo (ver
    // IdentityStoreJwt)
    @Resource
    SessionContext contexto;

    @Override
    public DetalleEmpresaDto crearEmpresa(CrearEmpresaDto dto) {
        Empresa empresa = empresaMapper.aEntidad(dto);
        // Recien creada no puede tener permiso
        return empresaMapper.aDetalle(
            empresaRepositorio.insert(empresa),
            Adjuntos.vacio()
        );
    }

    @Override
    public DetalleEmpresaDto modificarEmpresa(
        UUID id,
        ModificarEmpresaDto dto
    ) {
        Empresa empresa = buscar(id);
        empresaMapper.actualizar(dto, empresa);
        return detalle(empresaRepositorio.update(empresa));
    }

    @Override
    public void eliminarEmpresa(UUID id) {
        Empresa empresa = buscar(id);
        // Los archivos se vinculan sin FK: hay que borrarlos a mano
        Permiso permiso = empresa.getPermiso();
        if (permiso != null) {
            servicioArchivos.eliminarTodos(permiso);
            permisoRepositorio.delete(permiso);
        }
        empresaRepositorio.delete(empresa);
    }

    @Override
    public DetalleEmpresaDto obtener(UUID id)
        throws RecursoNoEncontradoException {
        return detalle(buscar(id));
    }

    /** Listado paginado. La pagina empieza en 1. */
    @Override
    public Page<ResumenEmpresaDto> listar(
        PageRequest pageRequest,
        List<Sort<Object>> sorts
    ) {
        return Persistencia.mapear(
            empresaRepositorio.todas(pageRequest, Persistencia.orden(sorts)),
            empresaMapper::aResumen
        );
    }

    @Override
    public List<ResumenEmpresaDto> listarDelEncargado() {
        return encargadoRepositorio
            .empresasDe(
                UUID.fromString(contexto.getCallerPrincipal().getName())
            )
            .stream()
            .map(empresaMapper::aResumen)
            .toList();
    }

    @Override
    public DetallePermisoDto reemplazarPermiso(
        UUID empresaId,
        ReemplazarPermisoDto dto,
        ArchivoSubido subido
    ) {
        Empresa empresa = buscar(empresaId);
        exigirEncargado(empresa);

        Permiso permiso = empresa.getPermiso();
        if (permiso == null) {
            permiso = new Permiso();
            permiso.setEmpresa(empresa);
        }
        permiso.setCodigoValidacion(dto.getCodigoValidacion());
        permiso.setFechaVencimiento(Fechas.parsear(dto.getFechaVencimiento()));
        permiso =
            permiso.getId() == null
                ? permisoRepositorio.insert(permiso)
                : permisoRepositorio.update(permiso);

        // El tipo se detecta por el contenido guardado. Si no es un PDF, el
        // rollback borra el archivo nuevo y conserva el anterior
        Archivo archivo = servicioArchivos.reemplazar(
            permiso,
            Permiso.ADJUNTO_ARCHIVO,
            subido
        );
        if (!TIPO_MIME_PERMISO.equals(archivo.getTipoMime())) {
            throw new ValidacionNegocioException(
                CodigoError.PERMISO_NO_PDF,
                "El permiso tiene que ser un PDF"
            );
        }
        return empresaMapper.aDetallePermiso(
            permiso,
            Adjuntos.vacio().con(
                permiso,
                Permiso.ADJUNTO_ARCHIVO,
                archivo,
                servicioArchivos.url(archivo)
            )
        );
    }

    /** Detalle de la empresa con su permiso, si tiene. */
    private DetalleEmpresaDto detalle(Empresa empresa) {
        return empresaMapper.aDetalle(
            empresa,
            servicioArchivos.adjuntos(
                Permiso.ADJUNTO_ARCHIVO,
                empresa.getPermiso()
            )
        );
    }

    private Empresa buscar(UUID id) {
        return empresaRepositorio
            .findById(id)
            .orElseThrow(() -> noEncontrada(id));
    }

    /**
     * Solo los encargados de la empresa. Al resto se le responde como si la
     * empresa no existiera, para no revelar que existe.
     */
    private void exigirEncargado(Empresa empresa) {
        if (!accesoEmpresa.esEncargadoDe(empresa.getId())) {
            throw noEncontrada(empresa.getId());
        }
    }

    private static RecursoNoEncontradoException noEncontrada(UUID id) {
        return new RecursoNoEncontradoException(
            CodigoError.EMPRESA_NO_ENCONTRADA,
            "Empresa no encontrada: " + id
        );
    }
}
