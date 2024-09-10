package api.geolocation.datamodels;

import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

@Data
public class AmenityModel {
    private long id;
    private Geometry geometry;
    private Map<String, String> tags;

    public AmenityModel(long id, Geometry geometry, Map<String, String> tags) {
        this.geometry = geometry;
        this.tags = tags;
        this.id = id;
    }
}
