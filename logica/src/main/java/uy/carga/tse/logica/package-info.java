/**
 * Capa logica, organizada en un paquete por funcionalidad ({@code empresa},
 * {@code usuario}, {@code vehiculo}, {@code archivos}, {@code autenticacion}).
 * Cada uno tiene su session bean ({@code XServicio}) expuesto mediante una
 * interfaz {@code @Local} ({@code XServicioLocal}) que recibe y devuelve DTOs,
 * y sus repositorios de Jakarta Data ({@code XRepositorio}), mappers
 * ({@code XMapper}) y excepciones propias. Las entidades no salen de la capa.
 * <p>
 * {@code comun} tiene lo que comparten todas: utilidades y, en
 * {@code comun.excepciones}, las categorias de {@code ExcepcionNegocio} que la
 * capa de servicios traduce a codigos HTTP.
 */
package uy.carga.tse.logica;
