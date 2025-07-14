package api.geolocation.controllers;

import api.geolocation.MapApplication;
import api.geolocation.RouteRequest;
import api.geolocation.Utilities;
import api.geolocation.datamodels.Road;
import api.geolocation.datamodels.RoutingResponse;
import api.geolocation.exceptions.InvalidRequestException;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;

@RestController
@RequestMapping("/route")
public class RouteController {
    @GetMapping
    public ResponseEntity<Object> getRoute(
            @RequestParam(required = false) Long from,
            @RequestParam(required = false) Long to,
            @RequestParam(required = false, defaultValue = "length") String weighting) {
        if (from == null || to == null || weighting == null)  {
            throw new InvalidRequestException("Invalid parameters!");
        }

        if (!weighting.equals("time") && !weighting.equals("length")) {
            throw new InvalidRequestException("Invalid weighting!!");
        }

        var routeResponse = loadRoute(from, to, weighting);

        return ResponseEntity.ok(routeResponse);
    }

    @SneakyThrows
    private RoutingResponse loadRoute(long from, long to, String weighting) {
        var request = RouteRequest.newBuilder()
                .setFrom(from)
                .setTo(to)
                .setWeighting(weighting)
                .build();

        var response = MapApplication.stub.getRoute(request);

        var routeResponse = new RoutingResponse();

        var roadsList = new ArrayList<Road>();

        for (var road : response.getRoadsList()) {
            JSONObject jsonObjectGeometry = (JSONObject) Utilities.jsonParser.parse(road.getJson());

            var newRoad = new Road();

            newRoad.setId(road.getId());
            newRoad.setGeom(jsonObjectGeometry);
            newRoad.setType(road.getType());
            newRoad.setName(road.getName());
            newRoad.setTags(road.getTagsMap());
            newRoad.setChild_ids(road.getChildIdsList());
            roadsList.add(newRoad);
        }

        routeResponse.setRoads(roadsList);
        routeResponse.setTime(response.getTime());
        routeResponse.setLength(response.getLength());

        return routeResponse;
    }
}
