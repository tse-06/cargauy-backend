package uy.carga.tse.datatypes.paginacion;

import jakarta.data.Sort;
import jakarta.data.page.PageRequest;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.QueryParam;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

/**
 * Query params de paginacion comunes a todos los listados
 */
@Getter
@Setter
public class PaginacionParams {

    public static final int TAMANIO_MAXIMO = 100;

    @QueryParam("page")
    @DefaultValue("1")
    @Min(1)
    private int page = 1;

    @QueryParam("size")
    @DefaultValue("20")
    @Min(1)
    @Max(TAMANIO_MAXIMO)
    private int size = 20;

    @QueryParam("sort")
    private List<String> sort = new ArrayList<>();

    public PageRequest toPageRequest() {
        return PageRequest.ofPage(page, size, true);
    }

    public List<Sort<Object>> toSorts(Set<String> camposPermitidos) {
        List<Sort<Object>> sorts = new ArrayList<>();
        for (String criterio : sort) {
            String[] partes = criterio.split(",", 2);
            String campo = partes[0].trim();
            String direccion =
                partes.length > 1 ? partes[1].trim().toLowerCase() : "asc";
            if (!camposPermitidos.contains(campo)) {
                throw new BadRequestException(
                    "Campo de ordenamiento no permitido: " + campo
                );
            }
            switch (direccion) {
                case "asc" -> sorts.add(Sort.asc(campo));
                case "desc" -> sorts.add(Sort.desc(campo));
                default -> throw new BadRequestException(
                    "Direccion de ordenamiento invalida: " + direccion
                );
            }
        }
        return sorts;
    }
}
