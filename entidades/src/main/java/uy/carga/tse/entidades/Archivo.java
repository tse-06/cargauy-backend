package uy.carga.tse.entidades;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Metadatos de un archivo. Los bytes se guardan en disco, en un archivo
 * nombrado por la {@code clave}. Se vincula a las entidades mediante
 * {@link ArchivoAdjunto}.
 */
@Entity
@Table(name = "archivos")
@Getter
@Setter
@NoArgsConstructor
public class Archivo extends EntidadBase {

    // Nombre del archivo en disco (base36, ver AlmacenamientoDisco)
    @Column(nullable = false, unique = true, length = 28)
    private String clave;

    // Nombre original con el que se subio el archivo
    @Column(nullable = false)
    private String nombre;

    @Column(name = "tipo_mime", nullable = false)
    private String tipoMime;

    // Tamanio en bytes
    @Column(nullable = false)
    private Long tamano;
}
