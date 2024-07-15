package api.geolocation.datamodels;

import api.geolocation.IOSMDataModel;
import lombok.Data;
import org.locationtech.jts.geom.Geometry;

import java.util.Map;

@Data
public class AmenityModel {
    private long id;
    private Geometry geometry;
    Map<String, String> tags;

    public AmenityModel(long id, Geometry geometry, Map<String, String> tags) {
        this.geometry = geometry;
        this.tags = tags;
        this.id = id;
    }

    public AmenityModel(IOSMDataModel dataModel) {
        this.id = dataModel.getId();
        this.geometry = dataModel.toGeometry();
        this.tags = dataModel.getTags();
    }

}
