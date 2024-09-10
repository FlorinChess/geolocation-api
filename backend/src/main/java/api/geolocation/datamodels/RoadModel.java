package api.geolocation.datamodels;

import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.util.List;
import java.util.Map;

@Data
public class RoadModel {
    private long id;
    private Geometry geometry;
    private Map<String, String> tags;
    private List<Long> nodeRefs;

    public RoadModel(long id, Geometry geometry, Map<String, String> tags, List<Long> nodeRefs) {
        this.id = id;
        this.geometry = geometry;
        this.tags = tags;
        this.nodeRefs = nodeRefs;
    }
}
