-- Estado de las guias. Las existentes quedan PENDIENTE si tienen vehiculo y
-- chofer, INCOMPLETA si les falta alguno.
alter table guias add column estado varchar(255) check ((estado in ('INCOMPLETA','PENDIENTE','NO_REALIZADA','EN_CURSO','FINALIZADA')));

update guias set estado = case when vehiculo_id is not null and chofer_id is not null then 'PENDIENTE' else 'INCOMPLETA' end;

alter table guias alter column estado set not null;
