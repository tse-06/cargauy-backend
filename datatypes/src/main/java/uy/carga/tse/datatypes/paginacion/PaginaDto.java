package uy.carga.tse.datatypes.paginacion;

import jakarta.data.page.Page;
import java.util.List;
import java.util.function.Function;

/**
 * Respuesta paginada generica para todos los endpoints de listado
 */
public record PaginaDto<T>(
    List<T> contenido,
    long page,
    int size,
    long totalElementos,
    long totalPaginas
) {
    public static <E, T> PaginaDto<T> de(Page<E> pagina, Function<E, T> mapeo) {
        return new PaginaDto<>(
            pagina.content().stream().map(mapeo).toList(),
            pagina.pageRequest().page(),
            pagina.pageRequest().size(),
            pagina.totalElements(),
            pagina.totalPages()
        );
    }

    public static <T> PaginaDto<T> de(Page<T> pagina) {
        return de(pagina, Function.identity());
    }
}
