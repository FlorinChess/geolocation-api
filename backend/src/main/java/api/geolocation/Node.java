package api.geolocation;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;

import java.util.HashMap;
import java.util.Map;

public class Node {
    long id;
    double lat;
    double lon;
    Map<String, String> tags;

    public Node() {
        tags = new HashMap<>();
    }

    public Geometry toPoint(){
        Coordinate[] coordinates = new Coordinate[] { new Coordinate(lon, lat) };

        return new Point(CoordinateArraySequenceFactory.instance()
                .create(coordinates), new GeometryFactory());
    }
}
