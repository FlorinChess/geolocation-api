package api.geolocation.datamodels;

import lombok.Data;
import org.locationtech.jts.geom.LinearRing;

@Data
public class ClosedCircle {
    private LinearRing linearRing;
    private String lastRole;
    private long lastMemberIndex;

    public ClosedCircle(LinearRing linearRing, String lastRole, long lastMemberIndex) {
        this.linearRing = linearRing;
        this.lastRole = lastRole;
        this.lastMemberIndex = lastMemberIndex;
    }
}
