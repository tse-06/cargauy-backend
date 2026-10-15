package uy.carga.tse.logica.comun;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import uy.carga.tse.datatypes.guia.CoordenadaDto;

/**
 * Conversion entre {@link CoordenadaDto} y {@link Point} de JTS (SRID 4326).
 * JTS usa x = longitud, y = latitud: crear los puntos siempre por aca para no
 * invertir los ejes.
 */
public final class Coordenadas {

    public static final int SRID_WGS84 = 4326;

    private static final GeometryFactory FABRICA = new GeometryFactory(
        new PrecisionModel(),
        SRID_WGS84
    );

    private Coordenadas() {}

    public static Point aPunto(CoordenadaDto dto) {
        return FABRICA.createPoint(
            new Coordinate(dto.longitud(), dto.latitud())
        );
    }

    public static CoordenadaDto aDto(Point punto) {
        return new CoordenadaDto(punto.getY(), punto.getX());
    }
}
