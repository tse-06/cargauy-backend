-- Baja logica de usuarios: null = activo; con valor = momento de la baja.
alter table usuarios add column fecha_baja timestamp(6) with time zone;
