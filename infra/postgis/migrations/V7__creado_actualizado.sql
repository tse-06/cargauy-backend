-- Todas las entidades tienen creado y actualizado (EntidadBase). Los asigna
-- Hibernate (@CreationTimestamp / @UpdateTimestamp); el default cubre las filas
-- existentes y los inserts que no pasan por Hibernate (ej. seed.sql).
-- archivos y archivos_adjuntos ya tenian creado (V4, V6).
alter table administradores add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table auditorias_infraccion add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table autoridades add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table choferes add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table configuraciones_sistema add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table descargos add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table empresas add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table encargados_empresa add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table eventos_viaje add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table funcionarios add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table guias add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table infracciones add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table inspecciones_tecnicas_vehiculares add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table notificaciones add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table permisos add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table permisos_nacionales_circulacion add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table resoluciones add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table usuarios add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table vehiculos add column creado timestamp(6) with time zone not null default now(), add column actualizado timestamp(6) with time zone not null default now();
alter table archivos add column actualizado timestamp(6) with time zone not null default now();
alter table archivos_adjuntos add column actualizado timestamp(6) with time zone not null default now();
