package api.geolocation.controllers;

import api.geolocation.*;
import api.geolocation.datamodels.PaginatedResult;
import api.geolocation.datamodels.Paging;
import api.geolocation.datamodels.Road;
import api.geolocation.exceptions.InternalIssuesException;
import api.geolocation.exceptions.InvalidRequestException;
import api.geolocation.exceptions.NotFoundException;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/roads")
public class RoadController {
    @GetMapping
    public ResponseEntity<Object> getRoads(
            @RequestParam(required = false) String road,
            @RequestParam(required = false, name = "bbox.tl.x") Double bboxTlX,
            @RequestParam(required = false, name = "bbox.tl.y") Double bboxTlY,
            @RequestParam(required = false, name = "bbox.br.x") Double bboxBrX,
            @RequestParam(required = false, name = "bbox.br.y") Double bboxBrY,
            @RequestParam(defaultValue = "50") int take,
            @RequestParam(defaultValue = "0") int skip) {
        if (bboxTlX != null && bboxTlY != null && bboxBrX != null && bboxBrY != null){
            if (!Utilities.isLatitudeValid(bboxTlX) || !Utilities.isLongitudeValid(bboxTlY) ||
                    !Utilities.isLatitudeValid(bboxBrX) || !Utilities.isLongitudeValid(bboxBrY)){
                throw new InvalidRequestException("Bad request: coordinates are invalid.");
            }

            var roads = loadRoads(road, bboxTlX, bboxTlY, bboxBrX, bboxBrY, take, skip);

            roads.sort(Comparator.comparingLong(api.geolocation.datamodels.Road::getId));

            List<Road> toReturn = new ArrayList<>();

            if((take < 0) || (skip < 0)){
                throw new InternalIssuesException("Take < 0!");
            }

            for (int i = skip; i < (take + skip); i++) {
                if(i >= roads.size()){
                    break;
                }
                toReturn.add(roads.get(i));
            }
            var response = new PaginatedResult<Road>(new Paging(skip, take, roads.size()));
            response.setEntries(toReturn);

            return ResponseEntity.ok(response);
        }

        throw new InvalidRequestException("Bad request: invalid query parameters.");
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getRoadsById(@PathVariable long id) {
        api.geolocation.datamodels.Road road = loadRoadById(id);
        if (road == null) {
            return ResponseEntity
                    .status(404)
                    .body("Entity not found!");
        }
        return ResponseEntity.ok(road);
    }

    @SneakyThrows
    private api.geolocation.datamodels.Road loadRoadById(long id) {
        api.geolocation.datamodels.Road road = null;

        RoadByIdRequest request = RoadByIdRequest.newBuilder().setId(id).build();
        RoadResponse response = MapApplication.stub.getRoadById(request);

        if (response.getStatus() == Status.Success) {
            JSONObject jsonObjectGeometry = (JSONObject) Utilities.jsonParser.parse(response.getRoad().getJson());

            road = new api.geolocation.datamodels.Road();
            road.setId(response.getRoad().getId());
            road.setGeom(jsonObjectGeometry);
            road.setType(response.getRoad().getType());
            road.setName(response.getRoad().getName());
            road.setTags(response.getRoad().getTagsMap());
            road.setChild_ids(response.getRoad().getChildIdsList());
        }
        if(response.getStatus() == Status.NotFound){
            throw new NotFoundException("Error 404: Entity request could not be found.");
        }
        if(response.getStatus() == Status.InternalError) {
            throw new NotFoundException("Error 500: An internal error has occurred.");
        }

        return road;
    }

    @SneakyThrows
    private List<api.geolocation.datamodels.Road> loadRoads (
            String road,
            double bboxTlX, double bboxTlY,
            double bboxBrX, double bboxBrY,
            int take, int skip) {

        var requestBuilder = RoadsByBBOXRequest.newBuilder()
                .setBboxTlX(bboxTlX)
                .setBboxTlY(bboxTlY)
                .setBboxBrX(bboxBrX)
                .setBboxBrY(bboxBrY)
                .setTake(take)
                .setSkip(skip);

        if (road != null) {
            requestBuilder.setRoad(road);
        }

        var request = requestBuilder.build();

        RoadsResponse response = MapApplication.stub.getRoadsByBBOX(request);
        ArrayList<api.geolocation.datamodels.Road> roadsList = new ArrayList<>();
        if (response.getStatus() == Status.Success) {
            for (api.geolocation.Road currentRoad : response.getRoadsList())  {
                buildRoadResponse(currentRoad, roadsList);
            }
        }
        if(response.getStatus() == Status.NotFound){
            throw new NotFoundException("Error 404: Entity request could not be found.");
        }
        if(response.getStatus() == Status.InternalError) {
            throw new NotFoundException("Error 500: An internal error has occurred.");
        }

        return roadsList;
    }

    private void buildRoadResponse(api.geolocation.Road currentRoad, ArrayList<api.geolocation.datamodels.Road> roadsList) throws ParseException {
        JSONObject jsonObjectGeometry = (JSONObject) Utilities.jsonParser.parse(currentRoad.getJson());

        api.geolocation.datamodels.Road newRoad = new api.geolocation.datamodels.Road();

        newRoad.setId(currentRoad.getId());
        newRoad.setGeom(jsonObjectGeometry);
        newRoad.setType(currentRoad.getType());
        newRoad.setName(currentRoad.getName());
        newRoad.setTags(currentRoad.getTagsMap());
        newRoad.setChild_ids(currentRoad.getChildIdsList());
        roadsList.add(newRoad);
    }
}
