package api.geolocation.datamodels;

import lombok.Data;
import org.json.simple.JSONObject;

import java.util.List;
import java.util.Map;

@Data
public class Road {
    String name = "";
    long id;
    JSONObject geom;
    Map<String, String> tags;
    String type;
    List<Long> child_ids;
}