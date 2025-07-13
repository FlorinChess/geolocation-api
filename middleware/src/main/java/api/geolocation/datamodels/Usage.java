package api.geolocation.datamodels;

import lombok.Data;

@Data
public class Usage {
    String type;
    double share;
    double area;

    public Usage(String type, double share, double area) {
        this.type = type;
        this.share = share;
        this.area = area;
    }
}
