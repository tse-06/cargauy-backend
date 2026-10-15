# cargauy-backend

Componente central (Java, Maven, WildFly) de la plataforma **carga.uy** — Taller de Sistemas Empresariales, Grupo 06, 2026.

Contiene la lógica de negocio principal (gestión de empresas, vehículos, guías, infracciones y usuarios), el subsistema de detección automática de infracciones, el subsistema de notificaciones, y la ingesta/almacenamiento de los eventos de tracking recibidos vía Kafka desde el nodo periférico de Sistema de Tracking.

Repos relacionados: [`cargauy-mobile`](https://gitlab.fing.edu.uy/tse-06/cargauy-mobile) (app del chofer) y [`cargauy-frontend`](https://gitlab.fing.edu.uy/tse-06/cargauy-frontend) (frontoffice/backoffice web).

## Estructura

```
datatypes/         DTOs y codigos de error, por funcionalidad (empresa/, usuario/, ...)
entidades/         Entidades JPA y enums
logica/            EJBs con la logica de negocio
servicios/         API REST (JAX-RS): recursos/, errores/ (ExceptionMappers), seguridad/ (JWT)
ingesta-tracking/  WAR que consume los eventos de tracking de Kafka y los guarda en Mongo
ear/               Empaqueta logica + servicios en cargauy.ear
infra/             Migraciones de Postgres (Flyway), init de Mongo y Dockerfiles de WildFly
```

### Paquetes de `logica`

Un paquete por funcionalidad (`empresa`, `usuario`, `vehiculo`, `archivos`, `autenticacion`), y cada uno tiene todo lo suyo:

| Qué | Nombre |
|---|---|
| Interfaz `@Local` / session bean | `XServicioLocal` / `XServicio` |
| Repositorio de Jakarta Data | `XRepositorio` |
| Conversión entidad ↔ DTO | `XMapper` (MapStruct, se inyecta) |
| Excepciones propias | `XException`, extendiendo una categoría de `comun.excepciones` |

`comun` tiene lo compartido; en `comun.excepciones` están las categorías de `ExcepcionNegocio` que `servicios` traduce a códigos HTTP. Una funcionalidad nueva (guías, infracciones, ...) va en un paquete nuevo con la misma forma.

### Convenciones

- Identificadores en español. Los términos técnicos (`Jwt`, `Mapper`, `Resource`, ...) y el módulo de gub.uy (`autenticacion.gubuy`) quedan en inglés.
- DTOs: `CrearXDto`, `ModificarXDto`, `DetalleXDto`, `ResumenXDto`.
- Los tests van en el mismo paquete que la clase que prueban; los helpers compartidos, en `testsupport`.

### Mappers

- Son interfaces (o clases abstractas, si tienen métodos propios) de MapStruct con `@Mapper(config = ConfiguracionMappers.class)`. La implementación generada (`XMapperImpl`) es un bean CDI: los EJBs la reciben con `@Inject` y los tests usan `new XMapperImpl()`, sin mocks.
- Un atributo del destino sin mapear rompe el build. Las asociaciones que no vienen del DTO se ignoran explícitamente con `@Mapping(target = "...", ignore = true)`.
- Métodos: `aEntidad(CrearXDto)`, `actualizar(ModificarXDto, @MappingTarget X)`, `aResumen(X)`, `aDetalle(X, ...)`.
- Las entidades relacionadas las mapea el mapper a partir de la entidad (`@Mapping(target = "itv", source = "inspeccionTecnicaVehicular")`). No se le pasan DTOs ya armados.
- **Lo que lee el mapper lo trae la consulta.** Los repositorios de Jakarta Data usan una `StatelessSession`, así que no hay lazy loading: más allá del id, una asociación que no se trajo no se puede leer. `findById` (`session.get`) ya trae con join las asociaciones `EAGER` a uno, como los `@OneToOne` inversos. Las consultas (`@Query`, `@Find`, `findAll`) no lo hacen: cada una de esas asociaciones genera una consulta extra por fila si no se trae con `left join fetch` (ej. `VehiculoRepositorio.porEmpresa`).
- Lo que no sale de JPA va como fuente extra (ej. los roles de un usuario) o como `@Context`. Por ejemplo, los archivos, que se vinculan sin FK: se traen con `ArchivoServicioLocal.adjuntos(...)` en una sola consulta y se pasan como `@Context Adjuntos`.
- Las conversiones que validan y tiran excepciones de negocio (ej. `parsearRubro`) son métodos `@Named` escritos a mano. `expression = "java(...)"` se usa solo para llamadas de una línea al `@Context`.
- Un mapper no usa a otro (`uses`): en los tests el mapper inyectado quedaría en null. Lo de un mismo agregado se mapea en el mismo mapper (ej. `EmpresaMapper.aDetallePermiso`).
- La normalización de negocio (ej. pasar la matrícula a mayúsculas) va en el servicio.
