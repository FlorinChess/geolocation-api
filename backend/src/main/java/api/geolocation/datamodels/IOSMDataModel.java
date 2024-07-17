package api.geolocation.datamodels;

import org.locationtech.jts.geom.Geometry;

import java.util.Map;

public interface IOSMDataModel {
    long getId();
    Map<String, String> getTags();
    Geometry toGeometry();
}
