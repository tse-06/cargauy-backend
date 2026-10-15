-- Parametros de las infracciones recurrentes (DA012/DA014): cantidad minima de
-- infracciones confirmadas del mismo tipo (5) y periodo de analisis (365 dias,
-- en nanosegundos como el resto de los Duration).
alter table configuraciones_sistema add column cantidad_infracciones_recurrentes integer not null default 5, add column periodo_infraccion_recurrente numeric(21,0) not null default 31536000000000000;

alter table configuraciones_sistema alter column cantidad_infracciones_recurrentes drop default, alter column periodo_infraccion_recurrente drop default;

-- La configuracion es un singleton: debe existir siempre una fila (mismos
-- valores por defecto que el seed de desarrollo)
insert into configuraciones_sistema (id, periodo_descargo, periodo_maximo_viaje, tolerancia_desvio_ruta, tolerancia_diferencia_peso_balanzas, tolerancia_exceso_carga, cantidad_infracciones_recurrentes, periodo_infraccion_recurrente)
select gen_random_uuid(), 432000000000000, 172800000000000, 5.00, 2.000, 5.000, 5, 31536000000000000
where not exists (select 1 from configuraciones_sistema);
