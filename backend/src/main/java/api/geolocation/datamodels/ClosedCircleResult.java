package api.geolocation.datamodels;

import org.locationtech.jts.geom.Polygon;

public class ClosedCircleResult {
    Polygon polygon;
    String lastRole;
    long lastMemberIndex;

    public ClosedCircleResult(Polygon polygon, String lastRole, long lastMemberIndex) {
        this.polygon = polygon;
        this.lastRole = lastRole;
        this.lastMemberIndex = lastMemberIndex;
    }
}
