-- Datos del permiso de la empresa, que pasa a ser uno solo por empresa. Hasta
-- ahora no se podia crear ningun permiso (ni guardar su archivo, ver V4).
delete from permisos;
alter table permisos add column codigo_validacion varchar(255) not null;
alter table permisos add column fecha_vencimiento date not null;
alter table permisos add constraint permisos_empresa_id_key unique (empresa_id);
