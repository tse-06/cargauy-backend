package uy.carga.tse.datatypes.vehiculo;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.ws.rs.FormParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos del permiso nacional de circulacion (PNC) de un vehiculo, recibidos
 * como campos de un multipart (con {@code @BeanParam}). La fecha va en formato
 * ISO (yyyy-MM-dd).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReemplazarPncDto {

    @FormParam("numero")
    @NotBlank
    @Size(max = 255)
    private String numero;

    @FormParam("fechaVencimiento")
    @NotBlank
    private String fechaVencimiento;
}
