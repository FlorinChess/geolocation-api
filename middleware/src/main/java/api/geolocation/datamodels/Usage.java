package api.geolocation.datamodels;

import lombok.Data;

@Data
public class Usage {
    String type;
    Double share;
    Double area;
}
