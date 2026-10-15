package uy.carga.tse.logica.guia;

import jakarta.annotation.Resource;
import jakarta.ejb.SessionContext;
import jakarta.ejb.Stateless;
import jakarta.inject.Inject;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.guia.CrearGuiaDto;
import uy.carga.tse.datatypes.guia.DetalleGuiaDto;
import uy.carga.tse.entidades.Chofer;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.Guia;
import uy.carga.tse.entidades.InspeccionTecnicaVehicular;
import uy.carga.tse.entidades.PermisoNacionalDeCirculacion;
import uy.carga.tse.entidades.Vehiculo;
import uy.carga.tse.entidades.enums.EstadoGuia;
import uy.carga.tse.entidades.enums.Roles;
import uy.carga.tse.logica.comun.excepciones.RecursoNoEncontradoException;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;
import uy.carga.tse.logica.empresa.EmpresaRepositorio;
import uy.carga.tse.logica.usuario.ChoferRepositorio;
import uy.carga.tse.logica.usuario.EncargadoEmpresaRepositorio;
import uy.carga.tse.logica.vehiculo.VehiculoRepositorio;

@Stateless
public class GuiaServicio implements GuiaServicioLocal {

    // La fecha de la guia es local a Uruguay, no UTC
    static final ZoneId ZONA = ZoneId.of("America/Montevideo");

    @Inject
    GuiaRepositorio guiaRepositorio;

    @Inject
    EmpresaRepositorio empresaRepositorio;

    @Inject
    VehiculoRepositorio vehiculoRepositorio;

    @Inject
    ChoferRepositorio choferRepositorio;

    @Inject
    EncargadoEmpresaRepositorio encargadoRepositorio;

    @Inject
    GuiaMapper guiaMapper;

    // El caller es el id del usuario y su unico rol el activo (ver
    // IdentityStoreJwt)
    @Resource
    SessionContext contexto;

    Clock reloj = Clock.systemUTC();

    @Override
    public DetalleGuiaDto crearGuia(CrearGuiaDto dto) {
        Empresa empresa = empresaDelEncargado(dto.empresaId());

        if (dto.fecha().isBefore(LocalDate.ofInstant(reloj.instant(), ZONA))) {
            throw new ValidacionNegocioException(
                CodigoError.FECHA_PASADA,
                "La fecha de la guia no puede ser anterior a hoy"
            );
        }
        if (dto.ubicacionOrigen().equals(dto.ubicacionDestino())) {
            throw new ValidacionNegocioException(
                CodigoError.ORIGEN_IGUAL_DESTINO,
                "El origen y el destino no pueden coincidir"
            );
        }

        Guia guia = guiaMapper.aEntidad(dto);
        guia.setEmpresa(empresa);
        if (dto.vehiculoId() != null) {
            guia.setVehiculo(vehiculoHabilitado(dto.vehiculoId(), guia));
        }
        if (dto.choferId() != null) {
            guia.setChofer(chofer(dto.choferId(), empresa));
        }
        guia.setEstado(
            guia.getVehiculo() != null && guia.getChofer() != null
                ? EstadoGuia.PENDIENTE
                : EstadoGuia.INCOMPLETA
        );
        return guiaMapper.aDetalle(guiaRepositorio.insert(guia));
    }

    /**
     * Empresa del caller. Al que no es encargado de la empresa se le responde
     * como si no existiera, para no revelar que existe.
     */
    private Empresa empresaDelEncargado(UUID empresaId) {
        if (
            !contexto.isCallerInRole(Roles.ENCARGADO_EMPRESA) ||
            !encargadoRepositorio.gestiona(
                UUID.fromString(contexto.getCallerPrincipal().getName()),
                empresaId
            )
        ) {
            throw empresaNoEncontrada(empresaId);
        }
        return empresaRepositorio
            .findById(empresaId)
            .orElseThrow(() -> empresaNoEncontrada(empresaId));
    }

    /**
     * Vehiculo de la empresa de la guia, con capacidad para la carga e ITV y
     * PNC vigentes en la fecha de la guia (inclusive el dia del vencimiento).
     * Si es de otra empresa se responde como si no existiera.
     */
    private Vehiculo vehiculoHabilitado(UUID vehiculoId, Guia guia) {
        Vehiculo vehiculo = vehiculoRepositorio
            .findById(vehiculoId)
            .filter(v ->
                v.getEmpresa().getId().equals(guia.getEmpresa().getId())
            )
            .orElseThrow(() ->
                new RecursoNoEncontradoException(
                    CodigoError.VEHICULO_NO_ENCONTRADO,
                    "Vehiculo no encontrado: " + vehiculoId
                )
            );
        if (guia.getPesoCarga().compareTo(vehiculo.getCapacidad()) > 0) {
            throw new ValidacionNegocioException(
                CodigoError.CAPACIDAD_EXCEDIDA,
                "La carga excede la capacidad del vehiculo"
            );
        }
        LocalDate fecha = guia.getFecha();
        InspeccionTecnicaVehicular itv =
            vehiculo.getInspeccionTecnicaVehicular();
        boolean itvVigente =
            itv != null && !itv.getFechaVencimiento().isBefore(fecha);
        if (!itvVigente) {
            throw new ValidacionNegocioException(
                CodigoError.ITV_NO_VIGENTE,
                "El vehiculo no tiene ITV vigente para el " + fecha
            );
        }
        PermisoNacionalDeCirculacion pnc =
            vehiculo.getPermisoNacionalDeCirculacion();
        boolean pncVigente =
            pnc != null && !pnc.getFechaVencimiento().isBefore(fecha);
        if (!pncVigente) {
            throw new ValidacionNegocioException(
                CodigoError.PNC_NO_VIGENTE,
                "El vehiculo no tiene PNC vigente para el " + fecha
            );
        }
        return vehiculo;
    }

    /** Si no trabaja para la empresa se responde como si no existiera. */
    private Chofer chofer(UUID usuarioId, Empresa empresa) {
        return choferRepositorio
            .deEmpresa(usuarioId, empresa.getId())
            .orElseThrow(() ->
                new RecursoNoEncontradoException(
                    CodigoError.CHOFER_NO_ENCONTRADO,
                    "Chofer no encontrado: " + usuarioId
                )
            );
    }

    private static RecursoNoEncontradoException empresaNoEncontrada(UUID id) {
        return new RecursoNoEncontradoException(
            CodigoError.EMPRESA_NO_ENCONTRADA,
            "Empresa no encontrada: " + id
        );
    }
}
