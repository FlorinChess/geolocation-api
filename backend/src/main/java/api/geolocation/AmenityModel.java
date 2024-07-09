package api.geolocation;

import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public class AmenityModel {
    Long id;
    Geometry geometry;
    Map<String, String> tags;

    public AmenityModel(Geometry geometry, Map<String, String> tags, long id) {
        this.geometry = geometry;
        this.tags = tags;
        this.id = id;
    }

}
