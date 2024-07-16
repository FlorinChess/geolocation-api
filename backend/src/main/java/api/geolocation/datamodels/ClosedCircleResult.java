package api.geolocation.datamodels;

import lombok.Data;
import org.locationtech.jts.geom.Polygon;

@Data
public class ClosedCircleResult {
    private Polygon polygon;
    private String lastRole;
    private long lastMemberIndex;

    public ClosedCircleResult(Polygon polygon, String lastRole, long lastMemberIndex) {
        this.polygon = polygon;
        this.lastRole = lastRole;
        this.lastMemberIndex = lastMemberIndex;
    }
}
