package uy.carga.tse.logica.vehiculo;

import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import uy.carga.tse.datatypes.vehiculo.CrearVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.DetalleItvDto;
import uy.carga.tse.datatypes.vehiculo.DetallePncDto;
import uy.carga.tse.datatypes.vehiculo.DetalleVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ModificarVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ResumenVehiculoDto;
import uy.carga.tse.entidades.InspeccionTecnicaVehicular;
import uy.carga.tse.entidades.PermisoNacionalDeCirculacion;
import uy.carga.tse.entidades.Vehiculo;
import uy.carga.tse.logica.archivos.Adjuntos;
import uy.carga.tse.logica.comun.ConfiguracionMappers;

@Mapper(config = ConfiguracionMappers.class)
public interface VehiculoMapper {
    /** Sin empresa: la asigna el servicio. */
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "inspeccionTecnicaVehicular", ignore = true)
    @Mapping(target = "permisoNacionalDeCirculacion", ignore = true)
    Vehiculo aEntidad(CrearVehiculoDto dto);

    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "inspeccionTecnicaVehicular", ignore = true)
    @Mapping(target = "permisoNacionalDeCirculacion", ignore = true)
    void actualizar(ModificarVehiculoDto dto, @MappingTarget Vehiculo vehiculo);

    @Mapping(target = "empresaId", source = "empresa.id")
    ResumenVehiculoDto aResumen(Vehiculo vehiculo);

    /** ITV y PNC vienen con el vehiculo (@OneToOne inversos, siempre cargados). */
    @Mapping(target = "empresaId", source = "empresa.id")
    @Mapping(target = "itv", source = "inspeccionTecnicaVehicular")
    @Mapping(target = "pnc", source = "permisoNacionalDeCirculacion")
    DetalleVehiculoDto aDetalle(Vehiculo vehiculo, @Context Adjuntos adjuntos);

    @Mapping(
        target = "nombreArchivo",
        expression = "java(adjuntos.nombreArchivo(itv, InspeccionTecnicaVehicular.ADJUNTO_ARCHIVO))"
    )
    @Mapping(
        target = "urlArchivo",
        expression = "java(adjuntos.url(itv, InspeccionTecnicaVehicular.ADJUNTO_ARCHIVO))"
    )
    DetalleItvDto aDetalleItv(
        InspeccionTecnicaVehicular itv,
        @Context Adjuntos adjuntos
    );

    @Mapping(
        target = "nombreArchivo",
        expression = "java(adjuntos.nombreArchivo(pnc, PermisoNacionalDeCirculacion.ADJUNTO_ARCHIVO))"
    )
    @Mapping(
        target = "urlArchivo",
        expression = "java(adjuntos.url(pnc, PermisoNacionalDeCirculacion.ADJUNTO_ARCHIVO))"
    )
    DetallePncDto aDetallePnc(
        PermisoNacionalDeCirculacion pnc,
        @Context Adjuntos adjuntos
    );
}
