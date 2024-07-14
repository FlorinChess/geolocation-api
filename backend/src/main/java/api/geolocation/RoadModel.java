package api.geolocation;

import org.locationtech.jts.geom.Geometry;

import java.util.List;
import java.util.Map;

public class RoadModel {
    long id;
    Geometry geometry;
    Map<String, String> tags;
    List<Long> nodeRefs;

    public RoadModel(long id, Geometry geometry, Map<String, String> tags, List<Long> nodeRefs) {
        this.id = id;
        this.geometry = geometry;
        this.tags = tags;
        this.nodeRefs = nodeRefs;
    }
}
