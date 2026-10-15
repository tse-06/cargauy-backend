package uy.carga.tse.servicios;

import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.json.bind.JsonbConfig;
import jakarta.ws.rs.ext.ContextResolver;
import jakarta.ws.rs.ext.Provider;

/**
 * Configura JSON-B para que los campos null se serialicen como null. Por
 * defecto se omiten y el cliente los ve como undefined.
 */
@Provider
public class ConfiguracionJson implements ContextResolver<Jsonb> {

    private final Jsonb jsonb = JsonbBuilder.create(
        new JsonbConfig().withNullValues(true)
    );

    @Override
    public Jsonb getContext(Class<?> tipo) {
        return jsonb;
    }
}
