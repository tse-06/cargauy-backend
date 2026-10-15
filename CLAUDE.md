# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

Module layout, `logica` package shape and naming conventions: @README.md

Stack: Java 25, Jakarta EE 11 on WildFly 41, Hibernate 7 + Jakarta Data repositories, Lombok. Multi-module Maven (`datatypes`, `entidades`, `logica` ejb, `servicios` war, `ingesta-tracking` war, `ear`).

## Build and run

- Dev: `cp .env.example .env`, fill it in, `docker compose up`, then `mvn package` on the host. `./ear/target` is bind-mounted as WildFly's `standalone/deployments`, so `mvn package` hot-deploys. API at http://localhost:8080/api, debug port 8787 (ingesta: 8081 / 8788).
- `mvn clean` wipes the bind-mounted deployments dir and undeploys the app. Don't run it in the normal loop, but DO run it after changing dependencies: jar names in the EAR carry no version and the ear plugin only copies newer files, so stale jars survive in `ear/ear-work` (this broke Tika once).
- `JWT_SECRET` (>= 32 chars), `ARCHIVOS_URL_SECRETO` (base64, >= 32 bytes) and `ARCHIVOS_URL_BASE` are required or the EAR deployment fails. `FRONTOFFICE_URL` / `BACKOFFICE_URL` feed the CORS allow-list; if missing, no origin is allowed.
- New annotation processors must be added to `annotationProcessorPaths` in the root `pom.xml` (JDK 23+ ignores processors on the classpath).

## Tests

- JUnit 5 + Mockito, no container. Single test: `mvn -pl logica -am test -Dtest=EmpresaServicioTest#creaElPermisoSiNoTiene -Dsurefire.failIfNoSpecifiedTests=false`
- EJBs are tested by `new`-ing them and assigning mocks to their `@Inject` / `@Resource` fields directly, so keep injected fields package-private (not `private`).
- Singletons that read env vars expose `iniciar(Function<String,String> entorno)` (called with `System::getenv` from `@PostConstruct`) so tests can inject values. Follow this for new env-dependent beans.
- Use real mappers in tests (`servicio.xMapper = new XMapperImpl()`), never mocks.
- Test method names are Spanish camelCase sentences (`creaElPermisoSiNoTiene`).

## Conventions

- Code, comments and javadoc in Spanish, comments without accents. Format with prettier-java (4-space indent, width 80): `mvn spotless:apply` (needs Node/npm; not bound to any build phase).
- Schema is owned by Flyway (`hbm2ddl.auto=validate`): any entity change needs a new `infra/postgis/migrations/V<n>__<desc>.sql`.
- `CodigoError` (`datatypes/.../error/CodigoError.java`) is a client contract: only add values, never rename or remove.
- From `logica`, throw only subclasses of the `comun.excepciones` categories (`ExcepcionNegocio`, rollback=true); the category decides the HTTP status in `ExcepcionNegocioMapper`. Never throw raw exceptions or `WebApplicationException` from `logica`.
- Pagination is 1-based; use `Persistencia.orden()` / `Persistencia.mapear()`.
- Mappers are MapStruct with `@Mapper(config = ConfiguracionMappers.class)`, injected as CDI beans; see README › Mappers. Jakarta Data repos run on a `StatelessSession` (no lazy loading), so whatever a mapper reads beyond an association's id must be loaded up front. `findById` joins EAGER to-one associations; queries don't (N+1) unless they `join fetch`. Archivos go to mappers as `@Context Adjuntos` (`servicioArchivos.adjuntos(...)`, one query).
- Archivos are attached polymorphically without FKs: when deleting an owner, call `servicioArchivos.eliminarTodos(...)` explicitly (see `EmpresaServicio.eliminarEmpresa`).
- `servicios` depends on `logica` with `provided` scope; keep runtime-only libs (e.g. jjwt-impl) in `logica`'s pom so they don't land in the WAR.

## Auth

- JWT via a Jakarta Security `HttpAuthenticationMechanism` (`servicios/.../seguridad/`). Missing/invalid tokens continue as anonymous and `@RolesAllowed` rejects them.
- The token comes from `Authorization: Bearer` (mobile app, scripts) or, for the web fronts, from the httpOnly cookie `cargauy_token` (`CookieSesion`); if the header is present the cookie is ignored. `POST /auth/login` sets the cookie (the body carries no token); the gub.uy callback sets it when `returnTo` is http/https and puts `#token=` in the fragment only for the mobile deep link; `POST /auth/logout` clears it; `GET /auth/me` returns the current session. A non-GET request authenticated only by the cookie must send `X-Requested-With: cargauy` (`FiltroCsrf`, 403 `CSRF_INVALIDO` otherwise). `CorsFilter` allows credentials for `FRONTOFFICE_URL` / `BACKOFFICE_URL`.
- In `@RolesAllowed` use the constants in `entidades/.../enums/Roles.java`, not the `Rol` enum.
- Do not put `@RolesAllowed` on EJBs. Role checks go on JAX-RS resources; inside EJBs read the caller from `@Resource SessionContext contexto`: `getCallerPrincipal().getName()` is the user UUID, `isCallerInRole(Roles.X)` checks the role. Ownership checks live in the EJB.

## Git

- GitLab (`gitlab.fing.edu.uy`), merged into `main` via MRs.
- Branches: `feature/`, `fix/`, `refactor/` + Spanish kebab-case (`feature/sistema-de-archivos`).
- Conventional Commits in Spanish, third-person present, lowercase, module as scope: `feat(logica): agrega ...`, `fix(ear): limpia ...`. Body as `- ` bullets explaining why.
