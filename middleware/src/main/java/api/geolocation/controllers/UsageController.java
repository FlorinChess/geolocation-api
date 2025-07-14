package api.geolocation.controllers;

import api.geolocation.MapApplication;
import api.geolocation.UsageRequest;
import api.geolocation.Utilities;
import api.geolocation.datamodels.Usage;
import api.geolocation.datamodels.UsageResponse;
import api.geolocation.exceptions.InvalidRequestException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class UsageController {
    @GetMapping("/usage")
    public UsageResponse getUsage(
            @RequestParam(required = false, name = "bbox.tl.x") Double bboxTlX,
            @RequestParam(required = false, name = "bbox.tl.y") Double bboxTlY,
            @RequestParam(required = false, name = "bbox.br.x") Double bboxBrX,
            @RequestParam(required = false, name = "bbox.br.y") Double bboxBrY) throws InvalidRequestException {
        if (bboxTlX == null || bboxTlY == null || bboxBrX == null || bboxBrY == null)
            throw new InvalidRequestException("Invalid request: not all parameters provided!");
        if (!Utilities.areBoundingBoxParametersValid(bboxTlX, bboxTlY, bboxBrX, bboxBrY))
            throw new InvalidRequestException("Invalid request: invalid values for latitude/longitude!");

        var request = UsageRequest.newBuilder()
                .setBboxTlX(bboxTlX)
                .setBboxTlY(bboxTlY)
                .setBboxBrX(bboxBrX)
                .setBboxBrY(bboxBrY)
                .build();

        var response = MapApplication.stub.getUsage(request);

        var requestResponse = new UsageResponse();
        requestResponse.setArea(response.getArea());

        for (var usage : response.getUsagesList()) {
            requestResponse.getUsages().add(new Usage(usage.getType(), usage.getShare(), usage.getArea()));
        }

        return requestResponse;
    }
}
