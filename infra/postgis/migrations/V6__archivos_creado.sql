-- Momento del alta de cada archivo, para poder limpiar los que quedan sin
-- adjuntar. Lo asigna Hibernate (@CreationTimestamp); el default cubre las
-- filas existentes y los inserts que no pasan por Hibernate.
alter table archivos add column creado timestamp(6) with time zone not null default now();
