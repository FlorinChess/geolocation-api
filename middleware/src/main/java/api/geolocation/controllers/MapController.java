package api.geolocation.controllers;

import api.geolocation.*;
import api.geolocation.datamodels.*;
import api.geolocation.datamodels.Road;
import api.geolocation.datamodels.PaginatedResult;
import api.geolocation.datamodels.Paging;
import api.geolocation.exceptions.InternalIssuesException;
import api.geolocation.exceptions.InvalidRequestException;
import api.geolocation.exceptions.NotFoundException;
import com.google.protobuf.ByteString;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.json.simple.parser.ParseException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
public class MapController {

    @SneakyThrows
    @GetMapping( "/amenities" )
    public ResponseEntity<Object> getAmenities(
        @RequestParam(required = false) String amenity,
        @RequestParam(required = false, name = "bbox.tl.x") Double bboxTlX,
        @RequestParam(required = false, name = "bbox.tl.y") Double bboxTlY,
        @RequestParam(required = false, name = "bbox.br.x") Double bboxBrX,
        @RequestParam(required = false, name = "bbox.br.y") Double bboxBrY,
        @RequestParam(required = false, name = "point.x") Double pointX,
        @RequestParam(required = false, name = "point.y") Double pointY,
        @RequestParam(required = false, name = "point.d") Double pointD,
        @RequestParam(defaultValue = "50") int take,
        @RequestParam(defaultValue = "0") int skip) {
        if (bboxTlX != null && bboxTlY != null && bboxBrX != null && bboxBrY != null) {
            if (!Utilities.isLatitudeValid(bboxTlX) || !Utilities.isLongitudeValid(bboxTlY) ||
                !Utilities.isLatitudeValid(bboxBrX) || !Utilities.isLongitudeValid(bboxBrY)) {
                throw new InvalidRequestException(Constants.badRequestPointValidCoordinatesInvalid);
            }

            if (pointX != null || pointY != null || pointD != null)
            {
                throw new InvalidRequestException("Bad request: bbox provided, but point parameters also provided.");
            }

            var amenities = loadAmenitiesByBoundingBox(amenity, bboxTlX, bboxTlY, bboxBrX, bboxBrY, take, skip);
            amenities.sort(Comparator.comparingLong(api.geolocation.datamodels.Amenity::getId));

            List<api.geolocation.datamodels.Amenity> toReturn = new ArrayList<>();

            if (!amenities.isEmpty()) {
                for (int i = 0; i < take; i++) {
                    toReturn.add(amenities.get(i));
                }
            }
            var response = new PaginatedResult<api.geolocation.datamodels.Amenity>(new Paging(skip, take, amenities.size()));
            response.setEntries(toReturn);

            return ResponseEntity.ok(response);
        }

        if (pointX != null && pointY != null && pointD != null) {
            if (!Utilities.isLatitudeValid(pointX) || !Utilities.isLongitudeValid(pointY) || pointD < 0) {
                throw new InvalidRequestException("Bad request: point provided, but coordinates are invalid.");
            }

            if (bboxBrX != null || bboxBrY != null || bboxTlX != null || bboxTlY != null) {
                throw new InvalidRequestException("Bad request: point provided, but bbox parameters also provided.");
            }

            var amenities = loadAmenitiesByPoint(amenity, pointX, pointY, pointD, take, skip);
            amenities.sort(Comparator.comparingLong(api.geolocation.datamodels.Amenity::getId));

            List<api.geolocation.datamodels.Amenity> toReturn = new ArrayList<>();

            if((take < 0) || (skip < 0)){
                throw new InternalIssuesException("Take < 0!");
            }

            if (!amenities.isEmpty())
            {
                for (int i = skip; i < (take + skip); i++) {
                    if(i >= amenities.size()){
                        break;
                    }
                    toReturn.add(amenities.get(i));
                }
            }


            var response = new PaginatedResult<api.geolocation.datamodels.Amenity>(new Paging(skip, take, amenities.size()));
            response.setEntries(toReturn);

            return ResponseEntity.ok(response);
        }

        throw new InvalidRequestException("Bad request: neither bbox nor point parameters provided.");
    }

    @SneakyThrows
    @GetMapping("/amenities/{id}")
    public ResponseEntity<Object> getAmenitiesById(@PathVariable Long id) {
        api.geolocation.datamodels.Amenity amenity = loadAmenityById(id);

        return ResponseEntity.ok(amenity);
    }

    @GetMapping("/roads")
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

            List<api.geolocation.datamodels.Road> toReturn = new ArrayList<>();

            if((take < 0) || (skip < 0)){
                throw new InternalIssuesException("Take < 0!");
            }

            for (int i = skip; i < (take + skip); i++) {
                if(i >= roads.size()){
                    break;
                }
                toReturn.add(roads.get(i));
            }
            var response = new PaginatedResult<api.geolocation.datamodels.Road>(new Paging(skip, take, roads.size()));
            response.setEntries(toReturn);

            return ResponseEntity.ok(response);
        }

        throw new InvalidRequestException("Bad request: invalid query parameters.");
    }

    @GetMapping("/roads/{id}")
    public ResponseEntity<Object> getRoadsById(@PathVariable long id) {
        api.geolocation.datamodels.Road road = loadRoadById(id);
        if (road == null) {
            return ResponseEntity
                    .status(404)
                    .body("Entity not found!");
        }
        return ResponseEntity.ok(road);
    }

    @CrossOrigin(origins = "*", allowedHeaders = "*")
    @GetMapping("/tile/{z}/{x}/{y}.png")
    public ResponseEntity<Object> getTile(
        @PathVariable int z,
        @PathVariable int x,
        @PathVariable int y,
        @RequestParam(required = false) String layers) {
        var byteString = loadTile(z, x, y, layers);

        byte[] pngBytes = new byte[byteString.size()];

        byteString.copyTo(pngBytes, 0);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.IMAGE_PNG);

        return new ResponseEntity<>(pngBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/route")
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

    public ByteString loadTile(int z, int x, int y, String layers) {
        var request = TileRequest.newBuilder()
                .setZ(z)
                .setX(x)
                .setY(y)
                .setLayers(layers)
                .build();

        var response = MapApplication.stub.getTile(request);

        if (response.getStatus() == Status.Success) {
            return response.getPng();
        }

        if (response.getStatus() == Status.NotFound) {
            throw new NotFoundException("Tile not found");
        }

        if (response.getStatus() == Status.InternalError) {
            throw new InvalidRequestException("Tile request invalid!");
        }

        return ByteString.EMPTY;
    }

    @SneakyThrows
    public api.geolocation.datamodels.Amenity loadAmenityById(long id) {
        api.geolocation.datamodels.Amenity amenity = null;

        AmenityByIdRequest request = AmenityByIdRequest.newBuilder().setId(id).build();
        AmenityResponse response = MapApplication.stub.getAmenityById(request);

        if (response.getStatus() == Status.Success) {
            JSONObject jsonObjectGeometry = (JSONObject) Utilities.jsonParser.parse(response.getAmenity().getJson());

            amenity = new api.geolocation.datamodels.Amenity();
            amenity.setId(response.getAmenity().getId());
            amenity.setGeom(jsonObjectGeometry);
            amenity.setType(response.getAmenity().getType());
            amenity.setName(response.getAmenity().getName());
            amenity.setTags(response.getAmenity().getTagsMap());
        }
        if(response.getStatus() == Status.NotFound){
            throw new NotFoundException("Error 404: Entity request could not be found.");
        }
        if(response.getStatus() == Status.InternalError) {
            throw new NotFoundException("Error 500: An internal error has occurred.");
        }

        return amenity;
    }

    @SneakyThrows
    public api.geolocation.datamodels.Road loadRoadById(long id) {
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

    public List<api.geolocation.datamodels.Amenity> loadAmenitiesByBoundingBox(
            String amenity,
            double bboxTlX,
            double bboxTlY,
            double bboxBrX,
            double bboxBrY,
            int take,
            int skip) {

        var requestBuilder = AmenitiesByBBOXRequest.newBuilder()
                .setBboxTlX(bboxTlX)
                .setBboxTlY(bboxTlY)
                .setBboxBrX(bboxBrX)
                .setBboxBrY(bboxBrY)
                .setTake(take)
                .setSkip(skip);

        if (amenity != null)
            requestBuilder.setAmenity(amenity);

        AmenitiesByBBOXRequest request = requestBuilder.build();

        AmenitiesResponse response = MapApplication.stub.getAmenitiesByBBOX(request);
        ArrayList<api.geolocation.datamodels.Amenity> amenitiesList = new ArrayList<>();

        if (response.getStatus() == Status.Success) {
            for (api.geolocation.Amenity currentAmenity : response.getAmenitiesList())  {
                buildAmenityResponse(currentAmenity, amenitiesList);
            }
        }
        if(response.getStatus() == Status.NotFound){
            throw new NotFoundException("Error 404: Entity request could not be found.");
        }
        if(response.getStatus() == Status.InternalError) {
            throw new NotFoundException("Error 500: An internal error has occurred.");
        }

        return amenitiesList;
    }

    @SneakyThrows
    private void buildAmenityResponse(api.geolocation.Amenity currentAmenity, ArrayList<api.geolocation.datamodels.Amenity> amenitiesList) {
        JSONObject jsonObjectGeometry = (JSONObject) Utilities.jsonParser.parse(currentAmenity.getJson());

        api.geolocation.datamodels.Amenity newAmenity = new api.geolocation.datamodels.Amenity();

        newAmenity.setId(currentAmenity.getId());
        newAmenity.setGeom(jsonObjectGeometry);
        newAmenity.setType(currentAmenity.getType());
        newAmenity.setName(currentAmenity.getName());
        newAmenity.setTags(currentAmenity.getTagsMap());
        amenitiesList.add(newAmenity);
    }

    public List<api.geolocation.datamodels.Amenity> loadAmenitiesByPoint(
        String amenity,
        double pointX,
        double pointY,
        double pointD,
        int take,
        int skip) {

        var requestBuilder = AmenitiesByPointRequest.newBuilder()
                .setPointX(pointX)
                .setPointY(pointY)
                .setPointD(pointD)
                .setTake(take)
                .setSkip(skip);

        if(amenity != null) {
            requestBuilder.setAmenity(amenity);
        }
        AmenitiesByPointRequest request = requestBuilder.build();

        AmenitiesResponse response = MapApplication.stub.getAmenitiesByPoint(request);
        ArrayList<api.geolocation.datamodels.Amenity> amenitiesList = new ArrayList<>();

        if (response.getStatus() == Status.Success) {
            for (api.geolocation.Amenity currentAmenity : response.getAmenitiesList())  {
                buildAmenityResponse(currentAmenity, amenitiesList);
            }
        }
        if(response.getStatus() == Status.NotFound){
            throw new NotFoundException("Error 404: Entity request could not be found.");
        }
        if(response.getStatus() == Status.InternalError) {
            throw new NotFoundException("Error 500: An internal error has occurred.");
        }

        return amenitiesList;
    }

    @SneakyThrows
    public List<api.geolocation.datamodels.Road> loadRoads (
        String road,
        double bboxTlX,
        double bboxTlY,
        double bboxBrX,
        double bboxBrY,
        int take,
        int skip) {

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
