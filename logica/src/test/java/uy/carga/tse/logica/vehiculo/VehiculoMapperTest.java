package uy.carga.tse.logica.vehiculo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static uy.carga.tse.logica.testsupport.Entidades.conId;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import uy.carga.tse.datatypes.vehiculo.DetalleVehiculoDto;
import uy.carga.tse.datatypes.vehiculo.ModificarVehiculoDto;
import uy.carga.tse.entidades.Archivo;
import uy.carga.tse.entidades.Empresa;
import uy.carga.tse.entidades.InspeccionTecnicaVehicular;
import uy.carga.tse.entidades.PermisoNacionalDeCirculacion;
import uy.carga.tse.entidades.Vehiculo;
import uy.carga.tse.logica.archivos.Adjuntos;

class VehiculoMapperTest {

    private final VehiculoMapper mapper = new VehiculoMapperImpl();
    private final Empresa empresa = conId(new Empresa());
    private final Vehiculo vehiculo = conId(new Vehiculo());

    VehiculoMapperTest() {
        vehiculo.setEmpresa(empresa);
        vehiculo.setMatricula("ABC1234");
    }

    @Test
    void mapeaItvYPncDelVehiculo() {
        InspeccionTecnicaVehicular itv = conId(
            new InspeccionTecnicaVehicular()
        );
        itv.setFechaVencimiento(LocalDate.of(2027, 12, 31));
        vehiculo.setInspeccionTecnicaVehicular(itv);
        PermisoNacionalDeCirculacion pnc = conId(
            new PermisoNacionalDeCirculacion()
        );
        pnc.setNumero("PNC-123");
        vehiculo.setPermisoNacionalDeCirculacion(pnc);
        Archivo archivo = conId(new Archivo());
        archivo.setNombre("itv.pdf");
        Adjuntos adjuntos = Adjuntos.vacio().con(
            itv,
            InspeccionTecnicaVehicular.ADJUNTO_ARCHIVO,
            archivo,
            "http://url/itv"
        );

        DetalleVehiculoDto detalle = mapper.aDetalle(vehiculo, adjuntos);

        assertEquals(empresa.getId(), detalle.empresaId());
        assertEquals(itv.getId(), detalle.itv().id());
        assertEquals(
            LocalDate.of(2027, 12, 31),
            detalle.itv().fechaVencimiento()
        );
        assertEquals("itv.pdf", detalle.itv().nombreArchivo());
        assertEquals("http://url/itv", detalle.itv().urlArchivo());
        assertEquals(pnc.getId(), detalle.pnc().id());
        assertEquals("PNC-123", detalle.pnc().numero());
        // El archivo es de la ITV, no del PNC
        assertNull(detalle.pnc().nombreArchivo());
        assertNull(detalle.pnc().urlArchivo());
    }

    @Test
    void dejaItvYPncEnNullSiNoTiene() {
        DetalleVehiculoDto detalle = mapper.aDetalle(
            vehiculo,
            Adjuntos.vacio()
        );

        assertEquals("ABC1234", detalle.matricula());
        assertNull(detalle.itv());
        assertNull(detalle.pnc());
    }

    @Test
    void actualizarNoTocaEmpresaNiDocumentos() {
        InspeccionTecnicaVehicular itv = conId(
            new InspeccionTecnicaVehicular()
        );
        vehiculo.setInspeccionTecnicaVehicular(itv);

        mapper.actualizar(
            new ModificarVehiculoDto(
                "XYZ9876",
                "Volvo",
                "FH",
                new BigDecimal("8000"),
                new BigDecimal("20000")
            ),
            vehiculo
        );

        assertEquals("XYZ9876", vehiculo.getMatricula());
        assertEquals("Volvo", vehiculo.getMarca());
        assertSame(empresa, vehiculo.getEmpresa());
        assertSame(itv, vehiculo.getInspeccionTecnicaVehicular());
    }
}
