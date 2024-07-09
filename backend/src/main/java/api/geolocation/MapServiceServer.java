package api.geolocation;

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
    public static final HashMap<Long, api.geolocation.Node> nodesMap = new HashMap<>();
    public static final HashMap<Long, api.geolocation.Node> roadsNodesMap = new HashMap<>();
    public static final HashMap<Long, Way> waysMap = new HashMap<>();
    public static final HashMap<Long, Way> waysRelationMap = new HashMap<>();
    public static final HashMap<Long, Relation> relationsMap = new HashMap<>();
    public static final HashMap<Long, AmenityModel> amenities = new HashMap<>();
    public static final HashMap<Long, RoadModel> roads = new HashMap<>();

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

                api.geolocation.Node newNode = new api.geolocation.Node();
                newNode.id = Long.parseLong(attributes.item(0).getNodeValue());
                newNode.lat = Double.parseDouble(attributes.item(1).getNodeValue());
                newNode.lon = Double.parseDouble(attributes.item(2).getNodeValue());

                var childNodes = currentNode.getChildNodes();

                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                        continue;
                    var childNodeAttributes = currentChildNode.getAttributes();

                    String key = childNodeAttributes.item(0).getNodeValue();
                    String val = childNodeAttributes.item(1).getNodeValue();
                    newNode.tags.put(key, val);
                }

                if (newNode.tags.containsKey("amenity")) {
                    nodeAmenitiesCount++;

                    var geometry = newNode.toPoint();
                    AmenityModel newAmenity = new AmenityModel(geometry, newNode.tags, newNode.id);
                    amenities.put(newNode.id, newAmenity);

                }

                if (newNode.tags.containsKey("highway")) {
                    nodeRoadsCount++;

                    var geometry = newNode.toPoint();
                    // TODO: nodeRefs
                    RoadModel newRoad = new RoadModel(newNode.id, geometry, newNode.tags, new ArrayList<>());
                    roads.put(newNode.id, newRoad);
                }

                nodesMap.put(newNode.id, newNode);
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
                newWay.id = Long.parseLong(currentNode.getAttributes().item(0).getNodeValue());

                var childNodes = currentNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;

                    var childNodeAttributes = currentChildNode.getAttributes();

                    if (currentChildNode.getNodeName().equals("nd"))
                    {
                        var refId = Long.parseLong(childNodeAttributes.item(0).getNodeValue());

                        newWay.nodeRefs.add(refId);
                        api.geolocation.Node toBeRemoved = nodesMap.remove(refId);

                        if (toBeRemoved != null) {
                            roadsNodesMap.put(toBeRemoved.id, toBeRemoved);
                        }
                    }

                    if (currentChildNode.getNodeName().equals("tag"))
                    {
                        String key = childNodeAttributes.item(0).getNodeValue();
                        String val = childNodeAttributes.item(1).getNodeValue();
                        newWay.tags.put(key, val);
                    }
                }

                if (newWay.tags.containsKey("amenity")) {
                    wayAmenitiesCount++;

                    var geometry = newWay.toGeometry();
                    AmenityModel newAmenity = new AmenityModel(geometry, newWay.tags, newWay.id);
                    amenities.put(newWay.id, newAmenity);
                }

                if (newWay.tags.containsKey("highway")) {
                    wayRoadsCount++;

                    var geometry = newWay.toGeometry();
                    RoadModel newRoad = new RoadModel(newWay.id, geometry, newWay.tags, newWay.nodeRefs);
                    roads.put(newWay.id, newRoad);
                }

                waysMap.put(newWay.id, newWay);
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
                newRelation.id = Long.parseLong(currentNode.getAttributes().item(0).getNodeValue());

                NodeList childNodes = currentNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                        continue;

                    var childNodeAttributes = currentChildNode.getAttributes();

                    // Set members
                    if (currentChildNode.getNodeName().equals("member")) {
                        Member newMember = new Member();
                        newMember.ref = Long.parseLong(childNodeAttributes.item(0).getNodeValue());
                        newMember.role = childNodeAttributes.item(1).getNodeValue();
                        newMember.type = childNodeAttributes.item(2).getNodeValue();

                        newRelation.members.add(newMember);

                        var refId = newMember.ref;

                        Way toBeRemoved = waysMap.remove(refId);

                        if (toBeRemoved != null) {
                            waysRelationMap.put(toBeRemoved.id, toBeRemoved);
                        }
                    }

                    // Set tags
                    if (currentChildNode.getNodeName().equals("tag")) {
                        String key = childNodeAttributes.item(0).getNodeValue();
                        String val = childNodeAttributes.item(1).getNodeValue();
                        newRelation.tags.put(key, val);
                    }
                }

                if (newRelation.tags.containsKey("amenity")){
                    try {
                        var geometry = newRelation.toGeometry();
                        AmenityModel newAmenity = new AmenityModel(geometry, newRelation.tags, newRelation.id);
                        amenities.put(newRelation.id, newAmenity);
                        relationAmenityCount++;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }

                if (newRelation.tags.containsKey("highway")) {
                    try {
                        var geometry = newRelation.toGeometry();
                        // TODO: nodeRefs
                        RoadModel newRoad = new RoadModel(newRelation.id, geometry, newRelation.tags, new ArrayList<>());
                        roads.put(newRelation.id, newRoad);
                        relationRoadsCount++;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }

                relationsMap.put(newRelation.id, newRelation);
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

    public static api.geolocation.Node getWayNodeById(long id) {
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