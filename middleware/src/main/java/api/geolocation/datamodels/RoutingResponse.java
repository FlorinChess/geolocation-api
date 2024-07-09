package api.geolocation.datamodels;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class RoutingResponse {
    Double length = 0.0D;
    Double time = 0.0D;
    List<Road> roads = new ArrayList<>();
}
