package uy.carga.tse.logica.empresa;

import java.util.Arrays;
import org.mapstruct.Context;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;
import uy.carga.tse.datatypes.empresa.CrearEmpresaDto;
import uy.carga.tse.datatypes.empresa.DetalleEmpresaDto;
import uy.carga.tse.datatypes.empresa.ModificarEmpresaDto;
import uy.carga.tse.datatypes.empresa.ResumenEmpresaDto;
import uy.carga.tse.datatypes.error.CodigoError;
import uy.carga.tse.datatypes.permiso.DetallePermisoDto;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.Permiso;
import uy.carga.tse.entidades.enums.Rubro;
import uy.carga.tse.logica.archivos.Adjuntos;
import uy.carga.tse.logica.comun.ConfiguracionMappers;
import uy.carga.tse.logica.comun.excepciones.ValidacionNegocioException;

@Mapper(config = ConfiguracionMappers.class)
public abstract class EmpresaMapper {

    @Mapping(target = "rubro", qualifiedByName = "rubro")
    @Mapping(target = "choferes", ignore = true)
    @Mapping(target = "encargados", ignore = true)
    @Mapping(target = "vehiculos", ignore = true)
    @Mapping(target = "guias", ignore = true)
    @Mapping(target = "infracciones", ignore = true)
    @Mapping(target = "permiso", ignore = true)
    public abstract Empresa aEntidad(CrearEmpresaDto dto);

    @Mapping(target = "rubro", qualifiedByName = "rubro")
    @Mapping(target = "choferes", ignore = true)
    @Mapping(target = "encargados", ignore = true)
    @Mapping(target = "vehiculos", ignore = true)
    @Mapping(target = "guias", ignore = true)
    @Mapping(target = "infracciones", ignore = true)
    @Mapping(target = "permiso", ignore = true)
    public abstract void actualizar(
        ModificarEmpresaDto dto,
        @MappingTarget Empresa empresa
    );

    public abstract ResumenEmpresaDto aResumen(Empresa empresa);

    /** El permiso viene con la empresa (@OneToOne inverso, siempre cargado). */
    public abstract DetalleEmpresaDto aDetalle(
        Empresa empresa,
        @Context Adjuntos adjuntos
    );

    @Mapping(
        target = "nombreArchivo",
        expression = "java(adjuntos.nombreArchivo(permiso, Permiso.ADJUNTO_ARCHIVO))"
    )
    @Mapping(
        target = "urlArchivo",
        expression = "java(adjuntos.url(permiso, Permiso.ADJUNTO_ARCHIVO))"
    )
    public abstract DetallePermisoDto aDetallePermiso(
        Permiso permiso,
        @Context Adjuntos adjuntos
    );

    @Named("rubro")
    protected Rubro rubro(String valor) {
        return parsearRubro(valor);
    }

    public static Rubro parsearRubro(String valor) {
        try {
            return Rubro.valueOf(valor.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new ValidacionNegocioException(
                CodigoError.RUBRO_INVALIDO,
                "Rubro invalido: " +
                    valor +
                    ". Valores posibles: " +
                    Arrays.toString(Rubro.values())
            );
        }
    }
}
