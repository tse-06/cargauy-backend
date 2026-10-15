package uy.carga.tse.logica.comun;

import jakarta.data.Order;
import jakarta.data.Sort;
import jakarta.data.page.Page;
import jakarta.data.page.impl.PageRecord;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Utilidades que comparten los session beans para hablar con los repositorios.
 */
public final class Persistencia {

    private Persistencia() {}

    /** Orden pedido mas el id como desempate, para que la paginacion sea estable. */
    public static <T> Order<T> orden(List<Sort<Object>> sorts) {
        List<Sort<? super T>> criterios = new ArrayList<>(sorts);
        criterios.add(Sort.asc("id"));
        return Order.by(criterios);
    }

    public static <E, D> Page<D> mapear(Page<E> pagina, Function<E, D> mapeo) {
        return new PageRecord<>(
            pagina.pageRequest(),
            pagina.content().stream().map(mapeo).toList(),
            pagina.totalElements()
        );
    }
}
