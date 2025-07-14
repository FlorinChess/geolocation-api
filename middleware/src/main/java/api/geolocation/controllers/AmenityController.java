package api.geolocation.controllers;

import api.geolocation.*;
import api.geolocation.datamodels.Amenity;
import api.geolocation.datamodels.PaginatedResult;
import api.geolocation.datamodels.Paging;
import api.geolocation.exceptions.InternalIssuesException;
import api.geolocation.exceptions.InvalidRequestException;
import api.geolocation.exceptions.NotFoundException;
import lombok.SneakyThrows;
import org.json.simple.JSONObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@RestController
@RequestMapping("/amenities")
public class AmenityController {
    @GetMapping
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

            List<Amenity> toReturn = new ArrayList<>();

            if (!amenities.isEmpty()) {
                for (int i = 0; i < take; i++) {
                    toReturn.add(amenities.get(i));
                }
            }
            var response = new PaginatedResult<Amenity>(new Paging(skip, take, amenities.size()));
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
    @GetMapping("/{id}")
    public ResponseEntity<Object> getAmenitiesById(@PathVariable Long id) {
        api.geolocation.datamodels.Amenity amenity = loadAmenityById(id);

        return ResponseEntity.ok(amenity);
    }

    private List<api.geolocation.datamodels.Amenity> loadAmenitiesByBoundingBox(
            String amenity,
            double bboxTlX, double bboxTlY,
            double bboxBrX, double bboxBrY,
            int take, int skip) {

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

    private List<api.geolocation.datamodels.Amenity> loadAmenitiesByPoint(
            String amenity,
            double pointX, double pointY, double pointD,
            int take, int skip) {

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
    private api.geolocation.datamodels.Amenity loadAmenityById(long id) {
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
}
