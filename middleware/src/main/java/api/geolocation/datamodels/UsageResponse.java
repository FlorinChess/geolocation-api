package api.geolocation.datamodels;

import lombok.Data;

import java.util.List;

@Data
public class UsageResponse {
    Double area = 0.0D;
    List<Usage> usages;
}
