package uy.carga.tse.logica.guia;

import java.util.Arrays;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.guia.CoordenadaDto;
import uy.carga.tse.datatypes.guia.CrearGuiaDto;
import uy.carga.tse.datatypes.guia.DetalleGuiaDto;
import uy.carga.tse.entidades.Guia;
import uy.carga.tse.entidades.enums.Rubro;
import uy.carga.tse.entidades.enums.TipoCarga;
import uy.carga.tse.logica.comun.ConfiguracionMappers;
import uy.carga.tse.logica.comun.Coordenadas;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;
import uy.carga.tse.logica.empresa.EmpresaMapper;

@Mapper(config = ConfiguracionMappers.class)
public interface GuiaMapper {
    /** Sin empresa, vehiculo, chofer ni estado: los asigna el servicio. */
    @Mapping(target = "rubroCliente", qualifiedByName = "rubro")
    @Mapping(target = "tipoCarga", qualifiedByName = "tipoCarga")
    @Mapping(target = "origen", qualifiedByName = "recortar")
    @Mapping(target = "destino", qualifiedByName = "recortar")
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "empresa", ignore = true)
    @Mapping(target = "vehiculo", ignore = true)
    @Mapping(target = "chofer", ignore = true)
    @Mapping(target = "eventos", ignore = true)
    Guia aEntidad(CrearGuiaDto dto);

    // Solo ids de las asociaciones: no inicializan los proxies lazy
    @Mapping(target = "empresaId", source = "empresa.id")
    @Mapping(target = "vehiculoId", source = "vehiculo.id")
    @Mapping(target = "choferId", source = "chofer.usuario.id")
    DetalleGuiaDto aDetalle(Guia guia);

    default Point aPunto(CoordenadaDto dto) {
        return Coordenadas.aPunto(dto);
    }

    default CoordenadaDto aCoordenada(Point punto) {
        return Coordenadas.aDto(punto);
    }

    @Named("recortar")
    default String recortar(String valor) {
        return valor.trim();
    }

    @Named("rubro")
    default Rubro rubro(String valor) {
        return EmpresaMapper.parsearRubro(valor);
    }

    @Named("tipoCarga")
    default TipoCarga tipoCarga(String valor) {
        try {
            return TipoCarga.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidacionNegocioException(
                CodigoError.TIPO_CARGA_INVALIDO,
                "Tipo de carga invalido: " +
                    valor +
                    ". Valores posibles: " +
                    Arrays.toString(TipoCarga.values())
            );
        }
    }
}
