package uy.carga.tse.logica.vehiculo;

import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.PageRequest;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.util.List;
import java.util.UUID;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.vehiculo.CrearVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.DetalleItvDto;
import uy.carga.tse.datatypes.vehiculo.DetallePncDto;
import uy.carga.tse.datatypes.vehiculo.DetalleVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ModificarVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ReemplazarItvDto;
import uy.carga.tse.datatypes.vehiculo.ReemplazarPncDto;
import uy.carga.tse.datatypes.vehiculo.ResumenVehiculoDto;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.EntidadBase;
import uy.carga.tse.entidades.InspeccionTecnicaVehicular;
import uy.carga.tse.entidades.PermisoNacionalDeCirculacion;
import uy.carga.tse.entidades.Vehiculo;
import uy.carga.tse.logica.archivos.Adjuntos;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal;
import uy.carga.tse.logica.archivos.ArchivoServicioLocal.ArchivoSubido;
import uy.carga.tse.logica.comun.Fechas;
import uy.carga.tse.logica.comun.Persistencia;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;
import uy.carga.tse.logica.empresa.AccesoEmpresa;
import uy.carga.tse.logica.empresa.EmpresaRepositorio;

@Stateless
public class VehiculoServicio implements VehiculoServicioLocal {

    static final String TIPO_MIME_DOCUMENTO = "application/pdf";

    @Inject
    VehiculoRepositorio vehiculoRepositorio;

    @Inject
    EmpresaRepositorio empresaRepositorio;

    @Inject
    ItvRepositorio itvRepositorio;

    @Inject
    PncRepositorio pncRepositorio;

    @Inject
    ArchivoServicioLocal servicioArchivos;

    @Inject
    VehiculoMapper vehiculoMapper;

    @Inject
    AccesoEmpresa accesoEmpresa;

    @Override
    public DetalleVehiculoDto crearVehiculo(CrearVehiculoDto dto) {
        Empresa empresa = empresaRepositorio
            .findById(dto.empresaId())
            .orElseThrow(() ->
                new RecursoNoEncontradoException(
                    CodigoError.EMPRESA_NO_ENCONTRADA,
                    "Empresa no encontrada: " + dto.empresaId()
                )
            );
        Vehiculo vehiculo = vehiculoMapper.aEntidad(dto);
        vehiculo.setMatricula(normalizar(vehiculo.getMatricula()));
        vehiculo.setEmpresa(empresa);
        // Recien creado no puede tener ITV ni PNC
        return vehiculoMapper.aDetalle(
            vehiculoRepositorio.insert(vehiculo),
            Adjuntos.vacio()
        );
    }

    @Override
    public DetalleVehiculoDto modificarVehiculo(
        UUID id,
        ModificarVehiculoDto dto
    ) {
        Vehiculo vehiculo = buscar(id);
        vehiculoMapper.actualizar(dto, vehiculo);
        vehiculo.setMatricula(normalizar(vehiculo.getMatricula()));
        return detalle(vehiculoRepositorio.update(vehiculo));
    }

    @Override
    public void eliminarVehiculo(UUID id) {
        Vehiculo vehiculo = buscar(id);
        // Los archivos se vinculan sin FK: hay que borrarlos a mano
        InspeccionTecnicaVehicular itv =
            vehiculo.getInspeccionTecnicaVehicular();
        if (itv != null) {
            servicioArchivos.eliminarTodos(itv);
            itvRepositorio.delete(itv);
        }
        PermisoNacionalDeCirculacion pnc =
            vehiculo.getPermisoNacionalDeCirculacion();
        if (pnc != null) {
            servicioArchivos.eliminarTodos(pnc);
            pncRepositorio.delete(pnc);
        }
        vehiculoRepositorio.delete(vehiculo);
    }

    @Override
    public DetalleVehiculoDto obtener(UUID id)
        throws RecursoNoEncontradoException {
        return detalle(buscar(id));
    }

    /** Listado paginado. La pagina empieza en 1. */
    @Override
    public Page<ResumenVehiculoDto> listar(
        PageRequest pageRequest,
        List<Sort<Object>> sorts
    ) {
        return Persistencia.mapear(
            vehiculoRepositorio.todos(pageRequest, Persistencia.orden(sorts)),
            vehiculoMapper::aResumen
        );
    }

    @Override
    public Page<ResumenVehiculoDto> listarPorEmpresa(
        UUID empresaId,
        PageRequest pageRequest,
        List<Sort<Object>> sorts
    ) {
        accesoEmpresa.exigirAcceso(empresaId);
        return Persistencia.mapear(
            vehiculoRepositorio.porEmpresa(
                empresaId,
                pageRequest,
                Persistencia.orden(sorts)
            ),
            vehiculoMapper::aResumen
        );
    }

    @Override
    public DetalleItvDto reemplazarItv(
        UUID vehiculoId,
        ReemplazarItvDto dto,
        ArchivoSubido subido
    ) {
        Vehiculo vehiculo = buscar(vehiculoId);
        exigirEncargado(vehiculo);

        InspeccionTecnicaVehicular itv =
            vehiculo.getInspeccionTecnicaVehicular();
        if (itv == null) {
            itv = new InspeccionTecnicaVehicular();
            itv.setVehiculo(vehiculo);
        }
        itv.setFechaVencimiento(Fechas.parsear(dto.getFechaVencimiento()));
        itv =
            itv.getId() == null
                ? itvRepositorio.insert(itv)
                : itvRepositorio.update(itv);

        Archivo archivo = reemplazarDocumento(
            itv,
            InspeccionTecnicaVehicular.ADJUNTO_ARCHIVO,
            subido
        );
        return vehiculoMapper.aDetalleItv(
            itv,
            adjunto(itv, InspeccionTecnicaVehicular.ADJUNTO_ARCHIVO, archivo)
        );
    }

    @Override
    public DetallePncDto reemplazarPnc(
        UUID vehiculoId,
        ReemplazarPncDto dto,
        ArchivoSubido subido
    ) {
        Vehiculo vehiculo = buscar(vehiculoId);
        exigirEncargado(vehiculo);

        PermisoNacionalDeCirculacion pnc =
            vehiculo.getPermisoNacionalDeCirculacion();
        if (pnc == null) {
            pnc = new PermisoNacionalDeCirculacion();
            pnc.setVehiculo(vehiculo);
        }
        pnc.setNumero(dto.getNumero().trim());
        pnc.setFechaVencimiento(Fechas.parsear(dto.getFechaVencimiento()));
        pnc =
            pnc.getId() == null
                ? pncRepositorio.insert(pnc)
                : pncRepositorio.update(pnc);

        Archivo archivo = reemplazarDocumento(
            pnc,
            PermisoNacionalDeCirculacion.ADJUNTO_ARCHIVO,
            subido
        );
        return vehiculoMapper.aDetallePnc(
            pnc,
            adjunto(pnc, PermisoNacionalDeCirculacion.ADJUNTO_ARCHIVO, archivo)
        );
    }

    /**
     * Reemplaza el archivo del documento. El tipo se detecta por el contenido
     * guardado. Si no es un PDF, el rollback borra el archivo nuevo y conserva
     * el anterior.
     */
    private Archivo reemplazarDocumento(
        EntidadBase documento,
        String adjunto,
        ArchivoSubido subido
    ) {
        Archivo archivo = servicioArchivos.reemplazar(
            documento,
            adjunto,
            subido
        );
        if (!TIPO_MIME_DOCUMENTO.equals(archivo.getTipoMime())) {
            throw new ValidacionNegocioException(
                CodigoError.ARCHIVO_NO_PDF,
                "El documento tiene que ser un PDF"
            );
        }
        return archivo;
    }

    /** Detalle del vehiculo con su ITV y PNC, si tiene. */
    private DetalleVehiculoDto detalle(Vehiculo vehiculo) {
        // ITV y PNC usan el mismo nombre de relacion: una sola consulta
        Adjuntos adjuntos = servicioArchivos.adjuntos(
            InspeccionTecnicaVehicular.ADJUNTO_ARCHIVO,
            vehiculo.getInspeccionTecnicaVehicular(),
            vehiculo.getPermisoNacionalDeCirculacion()
        );
        return vehiculoMapper.aDetalle(vehiculo, adjuntos);
    }

    /** El archivo recien subido, con su URL firmada. */
    private Adjuntos adjunto(
        EntidadBase documento,
        String adjunto,
        Archivo archivo
    ) {
        return Adjuntos.vacio().con(
            documento,
            adjunto,
            archivo,
            servicioArchivos.url(archivo)
        );
    }

    private Vehiculo buscar(UUID id) {
        return vehiculoRepositorio
            .findById(id)
            .orElseThrow(() -> noEncontrado(id));
    }

    /**
     * Solo los encargados de la empresa del vehiculo. Al resto se le responde
     * como si el vehiculo no existiera, para no revelar que existe ni de que
     * empresa es.
     */
    private void exigirEncargado(Vehiculo vehiculo) {
        if (!accesoEmpresa.esEncargadoDe(vehiculo.getEmpresa().getId())) {
            throw noEncontrado(vehiculo.getId());
        }
    }

    private static RecursoNoEncontradoException noEncontrado(UUID id) {
        return new RecursoNoEncontradoException(
            CodigoError.VEHICULO_NO_ENCONTRADO,
            "Vehiculo no encontrado: " + id
        );
    }

    private static String normalizar(String matricula) {
        return matricula.trim().toUpperCase();
    }
}
