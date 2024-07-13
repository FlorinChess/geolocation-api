package api.geolocation;

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

                nodeFromParameters.lon = request.getPointX();
                nodeFromParameters.lat = request.getPointY();

                Geometry geometryPoint = JTS.transform(nodeFromParameters.toPoint(), getMathTransform());

                for (var entry : MapServiceServer.amenities.entrySet()) {
                    var amenityModel = entry.getValue();
                    try {
                        var transformedGeometry = JTS.transform(amenityModel.geometry, getMathTransform());

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

            GeometryFactory factory = new GeometryFactory();
            var boundingBoxPolygon = factory.toGeometry(boundingBox);

            for (var entry : MapServiceServer.amenities.entrySet()) {
                AmenityModel amenityModel = entry.getValue();

                try {
                    if (amenityModel.geometry != null && boundingBoxPolygon.intersects(amenityModel.geometry)) {
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
            if (request.getAmenity().equals(amenityModel.tags.get(amenity))) {
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
            var json = writer.write(amenityModel.geometry);
            var type = "";
            var name = "";

            if (!amenityModel.tags.isEmpty()) {
                type = amenityModel.tags.getOrDefault("amenity", "");
                name = amenityModel.tags.getOrDefault("name", "");
            }

            responseBuilder.setId(id);
            responseBuilder.setJson(json);
            responseBuilder.setType(type);
            responseBuilder.setName(name);
            responseBuilder.putAllTags(amenityModel.tags);
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

        GeometryFactory factory = new GeometryFactory();
        var boundingBoxPolygon = factory.toGeometry(boundingBox);

        for (var entry : MapServiceServer.roads.entrySet()) {
            RoadModel roadModel = entry.getValue();

            try {
                if (roadModel.geometry != null && boundingBoxPolygon.intersects(roadModel.geometry)) {
                    if (!(request.getRoad().isEmpty())) {
                        if (request.getRoad().equals(roadModel.tags.get("highway"))) {
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
            var json = writer.write(road.geometry);
            var type = "";
            var name = "";

            if (!road.tags.isEmpty()) {
                type = road.tags.get("highway");
                name = road.tags.getOrDefault("name", "");
            }

            responseBuilder.setId(road.id);
            responseBuilder.setJson(json);
            responseBuilder.setType(type);
            responseBuilder.setName(name);
            responseBuilder.putAllTags(road.tags);
            responseBuilder.setStatus(Status.Success);
            responseBuilder.addAllChildIds(road.nodeRefs);
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

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(image, "png", baos);
            baos.flush();
            byte[] bytes = baos.toByteArray();
            baos.close();

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

        var json = writer.write(amenityModel.geometry);
        var type = "";
        var name = "";

        if (!amenityModel.tags.isEmpty()) {
            type = amenityModel.tags.getOrDefault("amenity", "");
            name = amenityModel.tags.getOrDefault("name", "");
        }

        amenityBuilder.setId(amenityModel.id);
        amenityBuilder.setJson(json);
        amenityBuilder.setType(type);
        amenityBuilder.setName(name);
        amenityBuilder.putAllTags(amenityModel.tags);

        return amenityBuilder.build();
    }

    private Road buildResponseRoad(RoadModel roadModel){
        var roadBuilder = Road.newBuilder();
        var json = writer.write(roadModel.geometry);
        var type = "";
        var name = "";


        if (!roadModel.tags.isEmpty()) {
            type = roadModel.tags.get("highway");
            name = roadModel.tags.getOrDefault("name", "");
        }

        roadBuilder.setId(roadModel.id);
        roadBuilder.setJson(json);
        roadBuilder.setType(type);
        roadBuilder.setName(name);
        roadBuilder.putAllTags(roadModel.tags);
        roadBuilder.addAllChildIds(roadModel.nodeRefs);
        return roadBuilder.build();
    }
}
