package uy.carga.tse.servicios;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/** Activa JAX-RS. La URL base queda en el context-root de jboss-web.xml (/api). */
@ApplicationPath("/")
public class ApiApplication extends Application {}
