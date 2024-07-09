package api.geolocation.datamodels;

import lombok.Data;
import org.json.simple.JSONObject;

import java.util.Map;

@Data
public class Amenity {
    String name = "";
    long id;
    JSONObject geom;
    Map<String, String> tags;
    String type;
}
