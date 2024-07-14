package api.geolocation;

import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class AmenityModel {
    long id;
    Geometry geometry;
    Map<String, String> tags;

    public AmenityModel(long id, Geometry geometry, Map<String, String> tags) {
        this.geometry = geometry;
        this.tags = tags;
        this.id = id;
    }

}
