package api.geolocation;

import api.geolocation.datamodels.*;
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
import java.util.*;

public class CommunicationService extends CommunicationServiceGrpc.CommunicationServiceImplBase {
    private MathTransform transform = null;
    private final GeoJsonWriter writer = new GeoJsonWriter();
    private final MapRenderer mapRenderer = new MapRenderer();
    private final DataStore dataStore = DataStore.getInstance();

    @Override
    public void getAmenitiesByBBOX(AmenitiesByBBOXRequest request, StreamObserver<AmenitiesResponse> observer) {
        MapLogger.backendLogAmenitiesRequest();

        Envelope boundingBox = buildBoundingBox(
                request.getBboxTlX(),
                request.getBboxTlY(),
                request.getBboxBrX(),
                request.getBboxBrY());

        var boundingBoxPolygon = DataStore.geometryFactory.toGeometry(boundingBox);

        var responseBuilder = AmenitiesResponse.newBuilder();
        for (var entry : dataStore.getAmenities().entrySet()) {
            AmenityModel amenityModel = entry.getValue();

            try {
                Geometry geometry = amenityModel.getGeometry();

                if (geometry == null || !boundingBoxPolygon.intersects(geometry)) continue;

                String amenityType = request.getAmenity();
                if (amenityType.isEmpty()) {
                    // If amenity type not specified -> add all
                    responseBuilder.addAmenities(buildResponseAmenity(amenityModel));
                } else if (amenityModel.getTags().get(amenityType).equals(amenityType)) {
                    // If amenity type specified -> add only those with matching tags
                    responseBuilder.addAmenities(buildResponseAmenity(amenityModel));
                }
            }
            catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }

        if (responseBuilder.getAmenitiesList().isEmpty())
            responseBuilder.setStatus(Status.NotFound);
        else
            responseBuilder.setStatus(Status.Success);

        var response = responseBuilder.build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void getAmenitiesByPoint(AmenitiesByPointRequest request, StreamObserver<AmenitiesResponse> responseObserver) {
        MapLogger.backendLogAmenitiesRequest();

        try {
            Node nodeFromParameters = new Node();

            nodeFromParameters.setLon(request.getPointX());
            nodeFromParameters.setLat(request.getPointY());

            Geometry geometryPoint = JTS.transform(nodeFromParameters.toGeometry(), getMathTransform());

            for (var entry : dataStore.getAmenities().entrySet()) {
                var amenityModel = entry.getValue();
                try {
                    var transformedGeometry = JTS.transform(amenityModel.getGeometry(), getMathTransform());

                    var distanceInMeters = transformedGeometry.distance(geometryPoint);

                    if (distanceInMeters <= request.getPointD()) {
                        System.out.println(distanceInMeters);
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

    @Override
    public void getAmenityById(AmenityByIdRequest request, StreamObserver<AmenityResponse> observer) {
        long id = request.getId();

        MapLogger.backendLogAmenityRequest((int)id);

        var amenityModel = dataStore.getAmenities().get(id);
        var responseBuilder = AmenityResponse.newBuilder();

        if (amenityModel != null) {
            responseBuilder.setAmenity(buildResponseAmenity(amenityModel));
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
    public void getRoadsByBBOX(RoadsByBBOXRequest request, StreamObserver<RoadsResponse> observer) {
        MapLogger.backendLogRoadsRequest();

        var responseBuilder = RoadsResponse.newBuilder();

        var boundingBox = buildBoundingBox(
                request.getBboxTlX(),
                request.getBboxTlY(),
                request.getBboxBrX(),
                request.getBboxBrY());

        var boundingBoxPolygon = DataStore.geometryFactory.toGeometry(boundingBox);

        for (var entry : dataStore.getRoads().entrySet()) {
            RoadModel roadModel = entry.getValue();

            try {
                Geometry geometry = roadModel.getGeometry();

                if (geometry == null || !boundingBoxPolygon.intersects(roadModel.getGeometry())) continue;

                String roadType = request.getRoad();

                if (roadType.isEmpty())
                    responseBuilder.addRoads(buildResponseRoad(roadModel));
                else if (request.getRoad().equals(roadModel.getTags().get("highway")))
                    responseBuilder.addRoads(buildResponseRoad(roadModel));
            }
            catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }

        if (responseBuilder.getRoadsList().isEmpty())
            responseBuilder.setStatus(Status.NotFound);
        else
            responseBuilder.setStatus(Status.Success);

        var response = responseBuilder.build();

        observer.onNext(response);
        observer.onCompleted();
    }

    @Override
    public void getRoadById(RoadByIdRequest request, StreamObserver<RoadResponse> observer) {
        long id = request.getId();

        MapLogger.backendLogRoadRequest((int) id);

        var roadModel = dataStore.getRoads().get(id);
        var responseBuilder = RoadResponse.newBuilder();

        if (roadModel != null) {
            responseBuilder.setRoad(buildResponseRoad(roadModel));
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
    public void getTile(TileRequest request, StreamObserver<TileResponse> observer) {
        var responseBuilder = TileResponse.newBuilder();

        try {
            var image = mapRenderer.renderTile(request.getZ(), request.getX(), request.getY(), request.getLayers());

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

        MapLogger.backendLogMapRequest(
                request.getX(),
                request.getY(),
                request.getZ(),
                Arrays.stream(request.getLayers().split(",")).toList());
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
        double bbox_tl_x = request.getBboxTlX();
        double bbox_tl_y = request.getBboxTlY();
        double bbox_br_x = request.getBboxBrX();
        double bbox_br_y = request.getBboxBrY();

        Envelope BBOX = new Envelope(bbox_tl_x,bbox_br_x,bbox_tl_y,bbox_br_y);
        var bboxGeometry = DataStore.geometryFactory.toGeometry(BBOX);

        // apply math transform
        bboxGeometry = applyMathTransform(bboxGeometry);

        // this maps landuse (aka usage) type to absolute area of intersection with the bbox
        Map<String, Double> usageTypeToAbsoluteArea = new HashMap<>();

        List<Way> waysWithLanduse = DataStore.getInstance().getWays().values().stream().parallel()
                .filter(way -> way.getTags().containsKey("landuse")).toList();

        // check intersection
        for (var way : waysWithLanduse) {
            Geometry wayGeometry = applyMathTransform(way.toGeometry());

            Geometry intersection = wayGeometry.intersection(bboxGeometry);

            if (!intersection.isEmpty()) {
                double intersectionArea = intersection.getArea();

                if (intersectionArea > 0) {
                    if (usageTypeToAbsoluteArea.containsKey(way.getTags().get("landuse"))) {
                        // we take out the old value
                        Double oldLanduseArea = usageTypeToAbsoluteArea.get(way.getTags().get("landuse"));

                        // add the area of the intersection to it
                        Double totalLanduseArea = oldLanduseArea + intersectionArea;

                        // overwrite the old value in the map with the new total area
                        usageTypeToAbsoluteArea.put(way.getTags().get("landuse"), totalLanduseArea);
                    }
                    else {
                        usageTypeToAbsoluteArea.put(way.getTags().get("landuse"), intersectionArea);
                    }
                }
            }
        }

        List<Relation> relationsWithLanduse = dataStore.getRelations().values().stream().parallel()
                .filter(relation -> relation.getTags().containsKey("landuse")).toList();

        int invalidGeometries = 0;

        for (var relation : relationsWithLanduse) {
            Geometry relationGeometry = applyMathTransform(relation.toGeometry());

            Geometry intersection = relationGeometry.intersection(bboxGeometry);

            if (!intersection.isEmpty()) {
                double intersectionArea = intersection.getArea();

                if (intersectionArea > 0) {
                    if (usageTypeToAbsoluteArea.containsKey(relation.getTags().get("landuse"))) {
                        // we take out the old value
                        Double oldLanduseArea = usageTypeToAbsoluteArea.get(relation.getTags().get("landuse"));

                        // add the area of the intersection to it
                        Double totalLanduseArea = oldLanduseArea + intersectionArea;

                        // overwrite the old value in the map with the new total area
                        usageTypeToAbsoluteArea.put(relation.getTags().get("landuse"), totalLanduseArea);
                    }
                    else {
                        usageTypeToAbsoluteArea.put(relation.getTags().get("landuse"), intersectionArea);
                    }
                }
            }
        }

        System.out.println(invalidGeometries);

        Map<String, Double> usageTypeToShareOfBbox = new HashMap<>();
        double bboxArea = bboxGeometry.getArea();

        for (var usageType : usageTypeToAbsoluteArea.entrySet()) {
            double share = usageType.getValue() / bboxArea;

            usageTypeToShareOfBbox.put(usageType.getKey(), share);
        }

        // Step 1: Convert Map to List of Map.Entry
        List<Map.Entry<String, Double>> entryList = new ArrayList<>(usageTypeToShareOfBbox.entrySet());

        // Step 2: Sort the List by values in ascending order
        entryList.sort(Comparator.comparingDouble(Map.Entry::getValue));

        // Step 3: Convert to List of Tuples (Key-Value pairs)
        List<Tuple<String, Double>> sortedTuples = entryList.stream()
                .map(entry -> new Tuple<>(entry.getKey(), entry.getValue()))
                .toList();

        var responseBuilder = UsageResponse.newBuilder();
        responseBuilder.setArea(bboxArea);

        for (var entry : sortedTuples) {
            String type = entry.first();
            double share = usageTypeToShareOfBbox.get(type);
            double area = usageTypeToAbsoluteArea.get(type);

            var usageBuilder = Usage.newBuilder();
            usageBuilder.setArea(area);
            usageBuilder.setShare(share);
            usageBuilder.setType(type);

            responseBuilder.addUsages(usageBuilder);
        }

        observer.onNext(responseBuilder.build());
        observer.onCompleted();
    }

    private Amenity buildResponseAmenity(AmenityModel amenityModel) {
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

    private Road buildResponseRoad(RoadModel roadModel) {
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

    private Envelope buildBoundingBox(double bboxTlX, double bboxTlY, double bboxBrX, double bboxBrY) {
        return new Envelope(bboxTlX, bboxBrX, bboxBrY, bboxTlY);
    }

    private MathTransform getMathTransform() {
        try {
            if (transform == null) {
                CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:4326", true);
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

    private Geometry applyMathTransform(Geometry geometry) {
        try {
            return JTS.transform(geometry, getMathTransform());
        } catch (Exception ex) {
            ex.printStackTrace(System.out);
            return geometry;
        }
    }
}
