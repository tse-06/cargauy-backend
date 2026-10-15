/**
 * DTOs de la API REST. Convencion de nombres (una clase por DTO, en un
 * subpaquete por entidad, ej. {@code datatypes.usuario}):
 *
 * <ul>
 * <li>Entrada (lo que reciben los resources): {@code <Accion><Entidad>Dto} o
 * {@code <Funcion>Dto}. Ej: {@code CrearUsuarioDto}, {@code IniciarSesionDto}.</li>
 * <li>Salida (lo que devuelven): {@code Resumen<Entidad>Dto} (para listados) o
 * {@code Detalle<Entidad>Dto}. Ej: {@code ResumenUsuarioDto},
 * {@code DetalleUsuarioDto}.</li>
 * </ul>
 *
 * Los session beans (modulo logica) reciben y devuelven DTOs: las entidades
 * no salen de la capa logica (las manejan los repositorios y los beans).
 */
package uy.carga.tse.datatypes;
