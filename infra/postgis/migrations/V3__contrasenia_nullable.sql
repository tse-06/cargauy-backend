-- Los usuarios que se autentican con gub.uy no tienen contrasenia en carga.uy
alter table usuarios alter column contrasenia drop not null;
