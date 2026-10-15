-- Los archivos dejan de referenciarse con FKs propias de cada entidad y pasan a
-- vincularse con una asociacion polimorfica (archivos_adjuntos). La tabla
-- archivos se recrea: hasta ahora no se guardaba ningun archivo.
alter table permisos drop column archivo_id;
alter table inspecciones_tecnicas_vehiculares drop column archivo_id;
alter table permisos_nacionales_circulacion drop column archivo_id;
drop table archivos;

create table archivos (
    id uuid not null,
    clave varchar(28) not null unique,
    nombre varchar(255) not null,
    tipo_mime varchar(255) not null,
    tamano bigint not null,
    primary key (id)
);

create table archivos_adjuntos (
    id uuid not null,
    tipo_entidad varchar(255) not null,
    entidad_id uuid not null,
    nombre varchar(255) not null,
    archivo_id uuid not null,
    creado timestamp(6) with time zone not null,
    primary key (id),
    unique (tipo_entidad, entidad_id, nombre, archivo_id)
);
create index archivos_adjuntos_entidad_idx on archivos_adjuntos (tipo_entidad, entidad_id, nombre);
create index archivos_adjuntos_archivo_idx on archivos_adjuntos (archivo_id);
alter table archivos_adjuntos add constraint archivos_adjuntos_archivo_fk foreign key (archivo_id) references archivos;
