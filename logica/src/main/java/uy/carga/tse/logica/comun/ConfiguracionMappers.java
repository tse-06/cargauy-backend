package uy.carga.tse.logica.comun;

import org.mapstruct.InjectionStrategy;
import org.mapstruct.MapperConfig;
import org.mapstruct.MappingConstants.ComponentModel;
import org.mapstruct.ReportingPolicy;

/**
 * Configuracion comun de los mappers de MapStruct: {@code @Mapper(config =
 * ConfiguracionMappers.class)}.
 *
 * <ul>
 *   <li>La implementacion generada es un bean CDI; los EJBs la inyectan con
 *       {@code @Inject} y los tests la crean con {@code new XMapperImpl()}.
 *   <li>Un atributo del destino sin mapear rompe el build: las asociaciones
 *       que no vienen del DTO se ignoran explicitamente.
 * </ul>
 */
@MapperConfig(
    componentModel = ComponentModel.JAKARTA_CDI,
    injectionStrategy = InjectionStrategy.FIELD,
    unmappedTargetPolicy = ReportingPolicy.ERROR
)
public interface ConfiguracionMappers {}
