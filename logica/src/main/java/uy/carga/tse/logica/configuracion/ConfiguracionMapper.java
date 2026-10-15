package uy.carga.tse.logica.configuracion;

import java.time.Duration;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import uy.carga.tse.datatypes.configuracion.DetalleConfiguracionFiscalizacionDto;
import uy.carga.tse.datatypes.configuracion.ModificarConfiguracionFiscalizacionDto;
import uy.carga.tse.entidades.ConfiguracionSistema;
import uy.carga.tse.logica.comun.ConfiguracionMappers;

@Mapper(config = ConfiguracionMappers.class)
public interface ConfiguracionMapper {
    @Mapping(target = "toleranciaDesvioRutaMetros", source = "toleranciaDesvioRuta")
    @Mapping(
        target = "periodoDescargoHoras",
        source = "periodoDescargo",
        qualifiedByName = "aHoras"
    )
    @Mapping(
        target = "periodoMaximoViajeHoras",
        source = "periodoMaximoViaje",
        qualifiedByName = "aHoras"
    )
    @Mapping(
        target = "periodoInfraccionRecurrenteDias",
        source = "periodoInfraccionRecurrente",
        qualifiedByName = "aDias"
    )
    DetalleConfiguracionFiscalizacionDto aDetalle(ConfiguracionSistema c);

    /** Reemplaza todos los parametros de la configuracion. */
    @Mapping(target = "toleranciaDesvioRuta", source = "toleranciaDesvioRutaMetros")
    @Mapping(
        target = "periodoDescargo",
        source = "periodoDescargoHoras",
        qualifiedByName = "deHoras"
    )
    @Mapping(
        target = "periodoMaximoViaje",
        source = "periodoMaximoViajeHoras",
        qualifiedByName = "deHoras"
    )
    @Mapping(
        target = "periodoInfraccionRecurrente",
        source = "periodoInfraccionRecurrenteDias",
        qualifiedByName = "deDias"
    )
    void actualizar(
        ModificarConfiguracionFiscalizacionDto dto,
        @MappingTarget ConfiguracionSistema c
    );

    @Named("aHoras")
    default long aHoras(Duration duracion) {
        return duracion.toHours();
    }

    @Named("aDias")
    default long aDias(Duration duracion) {
        return duracion.toDays();
    }

    @Named("deHoras")
    default Duration deHoras(Long horas) {
        return Duration.ofHours(horas);
    }

    @Named("deDias")
    default Duration deDias(Long dias) {
        return Duration.ofDays(dias);
    }
}
