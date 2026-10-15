-- Datos de desarrollo (NO es una migracion de Flyway: no se aplica solo).
-- Todos los usuarios tienen contrasenia "password" (PBKDF2WithHmacSHA256, 210000 iteraciones,
-- mismo formato que HashContrasenia). Idempotente: se puede correr varias veces.
--
--   docker compose exec -T postgis psql -U cargauy_app -d cargauy < infra/postgis/seed/seed.sql

begin;

-- Usuarios (uno por rol; el admin es 11111111 / password, y tambien es autoridad)
insert into usuarios (id, ci, nombre, apellido, email, contrasenia, fecha_nacimiento) values
  ('00000000-0000-0000-0000-000000000001', '11111111', 'Admin',   'Sistema', 'admin@carga.uy',    'PBKDF2WithHmacSHA256:210000:3/kkql1aVrCWOQqTZuB0vCWfQjAEta/QWkOV/vtDtvI=:WQFQptpUgT/La6YBB9AYAqvFFepbwlGvgEd3lC+7GlI=', '1980-01-01'),
  ('00000000-0000-0000-0000-000000000002', '22222222', 'Ana',     'Autoridad', 'autoridad@carga.uy', 'PBKDF2WithHmacSHA256:210000:3/kkql1aVrCWOQqTZuB0vCWfQjAEta/QWkOV/vtDtvI=:WQFQptpUgT/La6YBB9AYAqvFFepbwlGvgEd3lC+7GlI=', '1985-03-12'),
  ('00000000-0000-0000-0000-000000000003', '33333333', 'Fabian',  'Funcionario', 'funcionario@carga.uy', 'PBKDF2WithHmacSHA256:210000:3/kkql1aVrCWOQqTZuB0vCWfQjAEta/QWkOV/vtDtvI=:WQFQptpUgT/La6YBB9AYAqvFFepbwlGvgEd3lC+7GlI=', '1990-07-23'),
  ('00000000-0000-0000-0000-000000000004', '44444444', 'Elena',   'Encargada', 'encargado@carga.uy', 'PBKDF2WithHmacSHA256:210000:3/kkql1aVrCWOQqTZuB0vCWfQjAEta/QWkOV/vtDtvI=:WQFQptpUgT/La6YBB9AYAqvFFepbwlGvgEd3lC+7GlI=', '1988-11-05'),
  ('00000000-0000-0000-0000-000000000005', '55555555', 'Carlos',  'Chofer', 'chofer@carga.uy',     'PBKDF2WithHmacSHA256:210000:3/kkql1aVrCWOQqTZuB0vCWfQjAEta/QWkOV/vtDtvI=:WQFQptpUgT/La6YBB9AYAqvFFepbwlGvgEd3lC+7GlI=', '1992-05-30')
on conflict do nothing;

insert into administradores (id, usuario_id) values ('10000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-000000000001') on conflict do nothing;
insert into autoridades     (id, usuario_id) values ('10000000-0000-0000-0000-000000000002', '00000000-0000-0000-0000-000000000002') on conflict do nothing;
insert into autoridades     (id, usuario_id) values ('10000000-0000-0000-0000-000000000006', '00000000-0000-0000-0000-000000000001') on conflict do nothing;
insert into funcionarios    (id, usuario_id) values ('10000000-0000-0000-0000-000000000003', '00000000-0000-0000-0000-000000000003') on conflict do nothing;
insert into encargados_empresa (id, usuario_id) values ('10000000-0000-0000-0000-000000000004', '00000000-0000-0000-0000-000000000004') on conflict do nothing;
insert into choferes        (id, usuario_id) values ('10000000-0000-0000-0000-000000000005', '00000000-0000-0000-0000-000000000005') on conflict do nothing;

-- Empresas
insert into empresas (id, numero, nombre, razon_social, direccion, rubro) values
  ('20000000-0000-0000-0000-000000000001', '210000000001', 'Transportes del Sur', 'Transportes del Sur S.A.', 'Av. Italia 1234, Montevideo', 'ALIMENTOS'),
  ('20000000-0000-0000-0000-000000000002', '210000000002', 'Forestal Rivera',     'Forestal Rivera S.R.L.',   'Ruta 5 km 300, Rivera',        'FORESTAL')
on conflict do nothing;

insert into encargados_empresa_empresas (empresa_id, encargado_empresa_id) values
  ('20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000004'),
  ('20000000-0000-0000-0000-000000000002', '10000000-0000-0000-0000-000000000004')
on conflict do nothing;

insert into choferes_empresas (chofer_id, empresa_id) values
  ('10000000-0000-0000-0000-000000000005', '20000000-0000-0000-0000-000000000001')
on conflict do nothing;

-- Vehiculos
insert into vehiculos (id, matricula, marca, modelo, capacidad, peso, empresa_id) values
  ('30000000-0000-0000-0000-000000000001', 'SAB1234', 'Volvo',    'FH 460',  28000.000, 9000.000,  '20000000-0000-0000-0000-000000000001'),
  ('30000000-0000-0000-0000-000000000002', 'SBC5678', 'Scania',   'R 450',   30000.000, 9500.000,  '20000000-0000-0000-0000-000000000001'),
  ('30000000-0000-0000-0000-000000000003', 'SRV9012', 'Mercedes', 'Actros',  32000.000, 10000.000, '20000000-0000-0000-0000-000000000002')
on conflict do nothing;

-- Guias
insert into guias (id, fecha, origen, destino, ubicacion_origen, ubicacion_destino, tipo_carga, rubro_cliente, peso_carga, estado, empresa_id, chofer_id, vehiculo_id) values
  ('40000000-0000-0000-0000-000000000001', current_date, 'Montevideo', 'Salto',    'SRID=4326;POINT(-56.1913 -34.9058)', 'SRID=4326;POINT(-57.9667 -31.3833)', 'GENERAL',     'ALIMENTOS', 15000.000, 'PENDIENTE',  '20000000-0000-0000-0000-000000000001', '10000000-0000-0000-0000-000000000005', '30000000-0000-0000-0000-000000000001'),
  ('40000000-0000-0000-0000-000000000002', current_date, 'Rivera',     'Fray Bentos', 'SRID=4326;POINT(-55.5508 -30.9053)', 'SRID=4326;POINT(-58.2956 -33.1325)', 'GRANEL',   'FORESTAL',  25000.000, 'INCOMPLETA', '20000000-0000-0000-0000-000000000002', null,                                   '30000000-0000-0000-0000-000000000003')
on conflict do nothing;

-- Configuracion del sistema (los Duration se guardan en nanosegundos: descargo 5 dias, viaje maximo 48 h, recurrencia 365 dias)
insert into configuraciones_sistema (id, periodo_descargo, periodo_maximo_viaje, tolerancia_desvio_ruta, tolerancia_diferencia_peso_balanzas, tolerancia_exceso_carga, cantidad_infracciones_recurrentes, periodo_infraccion_recurrente)
select '50000000-0000-0000-0000-000000000001', 432000000000000, 172800000000000, 5.00, 2.000, 5.000, 5, 31536000000000000
where not exists (select 1 from configuraciones_sistema);

commit;
