package api.geolocation.datamodels;

import api.geolocation.DataStore;
import lombok.Data;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;

import java.util.HashMap;
import java.util.Map;

@Data
public class Node implements IOSMDataModel {
    private long id;
    private double lat;
    private double lon;
    private Map<String, String> tags;

    public Node() {
        tags = new HashMap<>();
    }

    public Node(long id, double lat, double lon, Map<String, String> tags) {
        this.id = id;
        this.lat = lat;
        this.lon = lon;
        this.tags = tags;
    }

    public Geometry toGeometry() {
        Coordinate[] coordinates = new Coordinate[] { new Coordinate(lon, lat) };

        return new Point(CoordinateArraySequenceFactory.instance()
                .create(coordinates), DataStore.geometryFactory);
    }
}
