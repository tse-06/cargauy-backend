package uy.carga.tse.datatypes.vehiculo;

import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.FormParam;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos de la inspeccion tecnica (ITV) de un vehiculo, recibidos como campos
 * de un multipart (con {@code @BeanParam}). La fecha va en formato ISO
 * (yyyy-MM-dd).
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ReemplazarItvDto {

    @FormParam("fechaVencimiento")
    @NotBlank
    private String fechaVencimiento;
}
