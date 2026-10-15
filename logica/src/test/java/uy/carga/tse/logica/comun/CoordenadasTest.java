package uy.carga.tse.logica.comun;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Point;
import uy.carga.tse.datatypes.guia.CoordenadaDto;

class CoordenadasTest {

    @Test
    void aPuntoUsaLongitudComoXYLatitudComoY() {
        Point punto = Coordenadas.aPunto(new CoordenadaDto(-34.9, -56.19));

        assertEquals(-56.19, punto.getX());
        assertEquals(-34.9, punto.getY());
        assertEquals(Coordenadas.SRID_WGS84, punto.getSRID());
    }

    @Test
    void aDtoDevuelveLaMismaCoordenada() {
        CoordenadaDto dto = new CoordenadaDto(-31.38, -57.97);

        assertEquals(dto, Coordenadas.aDto(Coordenadas.aPunto(dto)));
    }
}
