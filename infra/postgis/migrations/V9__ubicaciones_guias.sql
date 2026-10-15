-- Coordenadas de origen y destino de las guias. geography para que ST_Distance
-- y ST_DWithin midan en metros (deteccion de desvios y llegada a destino).
alter table guias add column ubicacion_origen geography(Point, 4326), add column ubicacion_destino geography(Point, 4326);

-- Las guias existentes (solo datos de prueba) quedan en el centro de Montevideo
update guias set ubicacion_origen = 'SRID=4326;POINT(-56.1913 -34.9058)', ubicacion_destino = 'SRID=4326;POINT(-56.1913 -34.9058)' where ubicacion_origen is null;

alter table guias alter column ubicacion_origen set not null, alter column ubicacion_destino set not null;
