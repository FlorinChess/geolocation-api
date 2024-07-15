package api.geolocation;

import api.geolocation.datamodels.AmenityModel;
import api.geolocation.datamodels.Node;
import api.geolocation.datamodels.RoadModel;
import com.google.protobuf.ByteString;
import io.grpc.stub.StreamObserver;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.io.geojson.GeoJsonWriter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import javax.imageio.ImageIO;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CommunicationService extends CommunicationServiceGrpc.CommunicationServiceImplBase {
    private MathTransform transform = null;
    private final GeoJsonWriter writer = new GeoJsonWriter();
    private final MapRenderer mapRenderer = new MapRenderer();

    private MathTransform getMathTransform() {
        try {
            if (transform == null) {
                CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326");
                CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:31256");

                transform = CRS.findMathTransform(sourceCRS, targetCRS, true);
            }
            return transform;
        }
        catch (Exception ex) {
            ex.printStackTrace(System.out);
            return null;

        }
    }
    @Override
    public void getAmenities(AmenitiesRequest request, StreamObserver<AmenitiesResponse> observer) {
        MapLogger.backendLogAmenitiesRequest();

        List<AmenityModel> amenitiesFound = new ArrayList<>();

        var responseBuilder = AmenitiesResponse.newBuilder();

        if (request.getBboxTlX() == 0 && request.getBboxTlY() == 0 && request.getBboxBrX() == 0 && request.getBboxBrY() == 0) {
            try {
                Node nodeFromParameters = new Node();

                nodeFromParameters.setLon(request.getPointX());
                nodeFromParameters.setLat(request.getPointY());

                Geometry geometryPoint = JTS.transform(nodeFromParameters.toGeometry(), getMathTransform());

                for (var entry : MapServiceServer.amenities.entrySet()) {
                    var amenityModel = entry.getValue();
                    try {
                        var transformedGeometry = JTS.transform(amenityModel.getGeometry(), getMathTransform());

                        var distanceInMeters = transformedGeometry.distance(geometryPoint);

                        if (distanceInMeters <= request.getPointD()) {
                            System.out.println(distanceInMeters);
                            checkAmenityParameter(request, amenityModel, "amenity", amenitiesFound);
                        }
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }
            }
            catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }
        else {
            Envelope boundingBox = MapServiceServer.buildBoundingBox(
                    request.getBboxTlX(),
                    request.getBboxTlY(),
                    request.getBboxBrX(),
                    request.getBboxBrY());

            var boundingBoxPolygon = MapServiceServer.geometryFactory.toGeometry(boundingBox);

            for (var entry : MapServiceServer.amenities.entrySet()) {
                AmenityModel amenityModel = entry.getValue();

                try {
                    if (amenityModel.getGeometry() != null && boundingBoxPolygon.intersects(amenityModel.getGeometry())) {
                        checkAmenityParameter(request, amenityModel, "highway", amenitiesFound);
                    }
                }
                catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }
        if (!amenitiesFound.isEmpty()) {
            responseBuilder.setStatus(Status.Success);

            for (var amenityModel : amenitiesFound) {
                responseBuilder.addAmenities(buildResponseAmenity(amenityModel));
            }
        }else{
            responseBuilder.setStatus(Status.NotFound);
        }

        var response = responseBuilder.build();

        observer.onNext(response);
        observer.onCompleted();
    }

    private static void checkAmenityParameter(AmenitiesRequest request, AmenityModel amenityModel, String amenity, List<AmenityModel> amenitiesFound) {
        if (!(request.getAmenity().isEmpty())) {
            if (request.getAmenity().equals(amenityModel.getTags().get(amenity))) {
                amenitiesFound.add(amenityModel);
            }
        } else {
            amenitiesFound.add(amenityModel);
        }
    }

    @Override
    public void getAmenityById(AmenityByIdRequest request, StreamObserver<AmenityResponse> observer) {
        long id = request.getId();

        MapLogger.backendLogAmenityRequest((int)id);

        var amenityModel = MapServiceServer.getAmenityModelById(id);
        var responseBuilder = AmenityResponse.newBuilder();

        if (amenityModel != null) {
            var json = writer.write(amenityModel.getGeometry());
            var type = "";
            var name = "";

            if (!amenityModel.getTags().isEmpty()) {
                type = amenityModel.getTags().getOrDefault("amenity", "");
                name = amenityModel.getTags().getOrDefault("name", "");
            }

            responseBuilder.setId(id);
            responseBuilder.setJson(json);
            responseBuilder.setType(type);
            responseBuilder.setName(name);
            responseBuilder.putAllTags(amenityModel.getTags());
            responseBuilder.setStatus(Status.Success);
        }
        else {
            responseBuilder.setStatus(Status.NotFound);
        }

        AmenityResponse response = responseBuilder.build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void getRoads(RoadsRequest request, StreamObserver<RoadsResponse> observer) {
        MapLogger.backendLogRoadsRequest();

        var responseBuilder = RoadsResponse.newBuilder();

        var roadsFound = new ArrayList<RoadModel>();

        var boundingBox = MapServiceServer.buildBoundingBox(
                request.getBboxTlX(),
                request.getBboxTlY(),
                request.getBboxBrX(),
                request.getBboxBrY());

        var boundingBoxPolygon = MapServiceServer.geometryFactory.toGeometry(boundingBox);

        for (var entry : MapServiceServer.roads.entrySet()) {
            RoadModel roadModel = entry.getValue();

            try {
                if (roadModel.getGeometry() != null && boundingBoxPolygon.intersects(roadModel.getGeometry())) {
                    if (!(request.getRoad().isEmpty())) {
                        if (request.getRoad().equals(roadModel.getTags().get("highway"))) {
                            roadsFound.add(roadModel);
                        }
                    }
                    else {
                        roadsFound.add(roadModel);
                    }
                }
            }
            catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }

        if (!roadsFound.isEmpty()) {
            for (var roadModel : roadsFound) {
                responseBuilder.addRoads(buildResponseRoad(roadModel));
            }
            responseBuilder.setStatus(Status.Success);
        }
        else {
            responseBuilder.setStatus(Status.NotFound);
        }

        var response = responseBuilder.build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void getRoadById(RoadByIdRequest request, StreamObserver<RoadResponse> observer) {
        long id = request.getId();

        MapLogger.backendLogRoadRequest((int) id);

        var road = MapServiceServer.getRoadModelById(id);
        var responseBuilder = RoadResponse.newBuilder();

        if (road != null) {
            var json = writer.write(road.getGeometry());
            var type = "";
            var name = "";

            if (!road.getTags().isEmpty()) {
                type = road.getTags().get("highway");
                name = road.getTags().getOrDefault("name", "");
            }

            responseBuilder.setId(road.getId());
            responseBuilder.setJson(json);
            responseBuilder.setType(type);
            responseBuilder.setName(name);
            responseBuilder.putAllTags(road.getTags());
            responseBuilder.setStatus(Status.Success);
            responseBuilder.addAllChildIds(road.getNodeRefs());
        }
        else {
            responseBuilder.setStatus(Status.NotFound);
        }

        var response = responseBuilder.build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void getTile(TileRequest request, StreamObserver<TileResponse> observer) {
        MapLogger.backendLogMapRequest(
                request.getX(),
                request.getY(),
                request.getZ(),
                Arrays.stream(request.getLayers().split(",")).toList());

        var responseBuilder = TileResponse.newBuilder();

        try {
            var image = mapRenderer.renderTile(
                    request.getZ(),
                    request.getX(),
                    request.getY(),
                    request.getLayers());

            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            ImageIO.write(image, "png", outputStream);
            outputStream.flush();
            byte[] bytes = outputStream.toByteArray();
            outputStream.close();

            var byteString = ByteString.copyFrom(bytes);

            responseBuilder.setPng(byteString);
            responseBuilder.setStatus(Status.Success);

        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            responseBuilder.setStatus(Status.InternalError);
        }

        var response = responseBuilder.build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void getRoute(RouteRequest request, StreamObserver<RouteResponse> observer) {
        var responseBuilder = RouteResponse.newBuilder();

        var response = responseBuilder.build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void getUsage(UsageRequest request, StreamObserver<UsageResponse> observer) {
        var responseBuilder = UsageResponse.newBuilder();

        var response = responseBuilder.build();

        observer.onNext(response);
        observer.onCompleted();
    }
    private Amenity buildResponseAmenity(AmenityModel amenityModel){
        var amenityBuilder = Amenity.newBuilder();

        var json = writer.write(amenityModel.getGeometry());
        var type = "";
        var name = "";

        if (!amenityModel.getTags().isEmpty()) {
            type = amenityModel.getTags().getOrDefault("amenity", "");
            name = amenityModel.getTags().getOrDefault("name", "");
        }

        amenityBuilder.setId(amenityModel.getId());
        amenityBuilder.setJson(json);
        amenityBuilder.setType(type);
        amenityBuilder.setName(name);
        amenityBuilder.putAllTags(amenityModel.getTags());

        return amenityBuilder.build();
    }

    private Road buildResponseRoad(RoadModel roadModel){
        var roadBuilder = Road.newBuilder();
        var json = writer.write(roadModel.getGeometry());
        var type = "";
        var name = "";


        if (!roadModel.getTags().isEmpty()) {
            type = roadModel.getTags().get("highway");
            name = roadModel.getTags().getOrDefault("name", "");
        }

        roadBuilder.setId(roadModel.getId());
        roadBuilder.setJson(json);
        roadBuilder.setType(type);
        roadBuilder.setName(name);
        roadBuilder.putAllTags(roadModel.getTags());
        roadBuilder.addAllChildIds(roadModel.getNodeRefs());
        return roadBuilder.build();
    }
}
