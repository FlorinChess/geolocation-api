package api.geolocation;

import api.geolocation.datamodels.*;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import org.locationtech.jts.geom.*;
import org.w3c.dom.*;

import javax.xml.parsers.*;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

public class MapServiceServer {
    private static final Logger logger = Logger.getLogger(MapServiceServer.class.getName());
    public static final HashMap<Long, api.geolocation.datamodels.Node> nodesMap = new HashMap<>();
    public static final HashMap<Long, api.geolocation.datamodels.Node> roadsNodesMap = new HashMap<>();
    public static final HashMap<Long, Way> waysMap = new HashMap<>();
    public static final HashMap<Long, Way> waysRelationMap = new HashMap<>();
    public static final HashMap<Long, Relation> relationsMap = new HashMap<>();
    public static final HashMap<Long, AmenityModel> amenities = new HashMap<>();
    public static final HashMap<Long, RoadModel> roads = new HashMap<>();
    public static final GeometryFactory geometryFactory = new GeometryFactory();

    private static int amenityCount = 0;
    private static int roadCount = 0;

    public static void main(String[] args) {
        logger.info("Starting backend...");

        int port;
        String backendOsmFile;

        try {
            port = Integer.parseInt(System.getenv().getOrDefault("JMAP_BACKEND_PORT", Constants.defaultBackendPort));
            backendOsmFile = System.getenv().getOrDefault("JMAP_BACKEND_OSMFILE", Constants.defaultBackendOsmFile);

            if (port < Constants.minPortValue || port > Constants.maxPortValue)
                port = Integer.parseInt(Constants.defaultBackendPort);

            if (backendOsmFile == null || backendOsmFile.isEmpty())
                backendOsmFile = Constants.defaultBackendOsmFile;

        } catch (Exception ex) {
            port = Integer.parseInt(Constants.defaultBackendPort);
            backendOsmFile = Constants.defaultBackendOsmFile;
        }

        parseOSMFile(backendOsmFile);

        startServer(port, backendOsmFile);
    }

    private static void startServer(int port, String backendOsmFile) {
        Server grpcServer = ServerBuilder.forPort(port)
                .addService(new CommunicationService())
                .build();

        try
        {
            grpcServer.start();
            MapLogger.backendStartup(port, backendOsmFile);
            grpcServer.awaitTermination();
        }
        catch (Exception e)
        {
            e.printStackTrace(System.out);
        }
    }

    private static void parseOSMFile(String filename) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(false);
            factory.setValidating(false);
            factory.setFeature("http://xml.org/sax/features/namespaces", false);
            factory.setFeature("http://xml.org/sax/features/validation", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            factory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);

            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new File(filename));
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("node");
            long nodesLength = nodes.getLength();

            NodeList ways = doc.getElementsByTagName("way");
            long waysLength = ways.getLength();

            NodeList relations = doc.getElementsByTagName("relation");
            long relationsLength = relations.getLength();

            parseNodes(nodes, nodesLength);

            parseWays(ways, waysLength);

            parseRelations(relations, relationsLength);

            MapLogger.backendLoadFinished(nodesMap.size(), waysMap.size(), relationsLength);

            System.out.println("Total amenity count: " + amenityCount);
            System.out.println("Total road count:    " + roadCount);
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    private static void parseNodes(NodeList nodes, long count) {
        try {
            int nodeAmenitiesCount = 0;
            int nodeRoadsCount = 0;

            for (int i = 0; i < count; i++) {
                org.w3c.dom.Node currentNode = nodes.item(i);
                var attributes = currentNode.getAttributes();

                api.geolocation.datamodels.Node newNode = new api.geolocation.datamodels.Node();
                newNode.setId(Long.parseLong(attributes.item(0).getNodeValue()));
                newNode.setLat(Double.parseDouble(attributes.item(1).getNodeValue()));
                newNode.setLon(Double.parseDouble(attributes.item(2).getNodeValue()));

                var childNodes = currentNode.getChildNodes();

                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                        continue;
                    var childNodeAttributes = currentChildNode.getAttributes();

                    String key = childNodeAttributes.item(0).getNodeValue();
                    String val = childNodeAttributes.item(1).getNodeValue();
                    newNode.getTags().put(key, val);
                }

                if (newNode.getTags().containsKey("amenity")) {
                    nodeAmenitiesCount++;

                    var geometry = newNode.toGeometry();
                    AmenityModel newAmenity = new AmenityModel(newNode.getId(), geometry, newNode.getTags());
                    amenities.put(newNode.getId(), newAmenity);

                }

                if (newNode.getTags().containsKey("highway")) {
                    nodeRoadsCount++;

                    var geometry = newNode.toGeometry();
                    // TODO: nodeRefs
                    RoadModel newRoad = new RoadModel(newNode.getId(), geometry, newNode.getTags(), new ArrayList<>());
                    roads.put(newNode.getId(), newRoad);
                }

                nodesMap.put(newNode.getId(), newNode);
            }

            System.out.println("Number of nodes representing amenities: " + nodeAmenitiesCount);
            System.out.println("Number of nodes representing roads:     " + nodeRoadsCount);
            amenityCount += nodeAmenitiesCount;
            roadCount += nodeRoadsCount;
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    private static void parseWays(NodeList ways, long count) {
        try {
            int wayAmenitiesCount = 0;
            int wayRoadsCount = 0;

            for (int i = 0; i < count; i++) {
                org.w3c.dom.Node currentNode = ways.item(i);

                Way newWay = new Way();
                newWay.setId(Long.parseLong(currentNode.getAttributes().item(0).getNodeValue()));

                var childNodes = currentNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;

                    var childNodeAttributes = currentChildNode.getAttributes();

                    if (currentChildNode.getNodeName().equals("nd"))
                    {
                        var refId = Long.parseLong(childNodeAttributes.item(0).getNodeValue());

                        newWay.getNodeRefs().add(refId);
                        api.geolocation.datamodels.Node toBeRemoved = nodesMap.remove(refId);

                        if (toBeRemoved != null) {
                            roadsNodesMap.put(toBeRemoved.getId(), toBeRemoved);
                        }
                    }

                    if (currentChildNode.getNodeName().equals("tag"))
                    {
                        String key = childNodeAttributes.item(0).getNodeValue();
                        String val = childNodeAttributes.item(1).getNodeValue();
                        newWay.getTags().put(key, val);
                    }
                }

                if (newWay.getTags().containsKey("amenity")) {
                    wayAmenitiesCount++;

                    var geometry = newWay.toGeometry();
                    AmenityModel newAmenity = new AmenityModel(newWay.getId(), geometry, newWay.getTags());
                    amenities.put(newWay.getId(), newAmenity);
                }

                if (newWay.getTags().containsKey("highway")) {
                    wayRoadsCount++;

                    var geometry = newWay.toGeometry();
                    RoadModel newRoad = new RoadModel(newWay.getId(), geometry, newWay.getTags(), newWay.getNodeRefs());
                    roads.put(newWay.getId(), newRoad);
                }

                waysMap.put(newWay.getId(), newWay);
            }

            System.out.println("Number of ways representing amenities: " + wayAmenitiesCount);
            System.out.println("Number of ways representing roads:     " + wayRoadsCount);

            amenityCount += wayAmenitiesCount;
            roadCount += wayRoadsCount;
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    private static void parseRelations(NodeList relations, long count) {
        try {
            int relationAmenityCount = 0;
            int relationRoadsCount = 0;

            for (int i = 0; i < count; i++) {
                org.w3c.dom.Node currentNode = relations.item(i);

                Relation newRelation = new Relation();
                newRelation.setId(Long.parseLong(currentNode.getAttributes().item(0).getNodeValue()));

                NodeList childNodes = currentNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                        continue;

                    var childNodeAttributes = currentChildNode.getAttributes();

                    // Set members
                    if (currentChildNode.getNodeName().equals("member")) {
                        Member newMember = new Member(geometryFactory);
                        newMember.setRef(Long.parseLong(childNodeAttributes.item(0).getNodeValue()));
                        newMember.setRole(childNodeAttributes.item(1).getNodeValue());
                        newMember.setType(childNodeAttributes.item(2).getNodeValue());

                        newRelation.getMembers().add(newMember);

                        var refId = newMember.getRef();

                        Way toBeRemoved = waysMap.remove(refId);

                        if (toBeRemoved != null) {
                            waysRelationMap.put(toBeRemoved.getId(), toBeRemoved);
                        }
                    }

                    // Set tags
                    if (currentChildNode.getNodeName().equals("tag")) {
                        String key = childNodeAttributes.item(0).getNodeValue();
                        String val = childNodeAttributes.item(1).getNodeValue();
                        newRelation.getTags().put(key, val);
                    }
                }

                if (newRelation.getTags().containsKey("amenity")){
                    try {
                        var geometry = newRelation.toGeometry();
                        AmenityModel newAmenity = new AmenityModel(newRelation.getId(), geometry, newRelation.getTags());
                        amenities.put(newRelation.getId(), newAmenity);
                        relationAmenityCount++;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }

                if (newRelation.getTags().containsKey("highway")) {
                    try {
                        var geometry = newRelation.toGeometry();
                        // TODO: nodeRefs
                        RoadModel newRoad = new RoadModel(newRelation.getId(), geometry, newRelation.getTags(), new ArrayList<>());
                        roads.put(newRelation.getId(), newRoad);
                        relationRoadsCount++;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }

                relationsMap.put(newRelation.getId(), newRelation);
            }

            System.out.println("Number of relations representing amenities: " + relationAmenityCount);
            System.out.println("Number of relations representing roads:     " + relationRoadsCount);

            amenityCount += relationAmenityCount;
            roadCount += relationRoadsCount;
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    public static Way getRelationWayById(long id) {
        return waysRelationMap.get(id);
    }

    public static api.geolocation.datamodels.Node getWayNodeById(long id) {
        return roadsNodesMap.get(id);
    }

    public static Envelope buildBoundingBox(double bboxTlX, double bboxTlY, double bboxBrX, double bboxBrY) {
        return new Envelope(bboxTlX, bboxBrX, bboxBrY, bboxTlY);
    }

    public static AmenityModel getAmenityModelById(long id){
        return amenities.get(id);
    }

    public static RoadModel getRoadModelById(long id) {
        return roads.get(id);
    }
}