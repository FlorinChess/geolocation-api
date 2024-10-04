package api.geolocation;

import api.geolocation.datamodels.Member;
import api.geolocation.datamodels.Node;
import api.geolocation.datamodels.Relation;
import api.geolocation.datamodels.Way;
import api.geolocation.osm.OSMFinder;
import api.geolocation.osm.OSMParser;
import com.fasterxml.jackson.databind.JsonNode;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

public class MapServiceServer {
    private final static Logger logger = Logger.getLogger(MapServiceServer.class.getName());
    private static int port;
    private static String backendOsmFile;

    public static void main(String[] args) {
        logger.info("Starting backend...");

        parseEnvironmentVariables();

        parseOSMFile();

         fixInvalidEntries();

        startServer();
    }

    private static void parseEnvironmentVariables() {
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
    }

    private static void parseOSMFile() {
        OSMParser osmParser = new OSMParser(backendOsmFile);
        osmParser.parse();
    }

    private static void fixInvalidEntries() {
        DataStore dataStore = DataStore.getInstance();

        // Fetch missing nodes
        for (Way invalidWay : dataStore.getInvalidWays()) {
            List<Long> missingNodes = new ArrayList<>(invalidWay.getMissingNodes());

            for (long missingNodeId : missingNodes) {
                if (dataStore.getNodes().containsKey(missingNodeId)) {
                    System.out.println("Node fetched already! Skipping request...");
                    continue;
                }

                try {
                    System.out.println("Querying for id = " + missingNodeId);
                    JsonNode response = OSMFinder.fetchNode(missingNodeId);

                    if (response.has("elements") && response.get("elements").isArray()) {
                        for (JsonNode element : response.get("elements")) {
                            if ("node".equals(element.get("type").asText())) {
                                Node missingNode = new Node();
                                missingNode.setId(element.get("id").asLong());
                                missingNode.setLat(element.get("lat").asDouble());
                                missingNode.setLon(element.get("lon").asDouble());

                                // Check if the node has tags
                                if (element.has("tags")) {
                                    JsonNode tagsNode = element.get("tags");
                                    Iterator<String> fieldNames = tagsNode.fieldNames();
                                    while (fieldNames.hasNext()) {
                                        String key = fieldNames.next();
                                        String value = tagsNode.get(key).asText();
                                        missingNode.getTags().put(key, value);
                                    }
                                }

                                // Add new node
                                invalidWay.getNodes().add(missingNode);

                                // Add to all nodes
                                dataStore.getNodes().put(missingNodeId, missingNode);

                                // remove from missing nodes
                                invalidWay.getMissingNodes().remove(missingNodeId);
                                System.out.println("Success!");
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }

            if (invalidWay.getMissingNodes().isEmpty()) {
                // Add to valid ways
                dataStore.getWays().put(invalidWay.getId(), invalidWay);

                // Set to null for garbage collection
                invalidWay.setMissingNodes(null);
            }
        }

        System.out.println("Finished fixing invalid ways!\nAttempting to fix relations with missing ways...");

        // Fetch missing ways
        for (Relation invalidRelation : dataStore.getInvalidRelations()) {

            List<Member> missingMembers = new ArrayList<>(invalidRelation.getMissingMembers());
            for (Member missingMember : missingMembers) {
                if (dataStore.getWays().containsKey(missingMember.getRef())) {
                    invalidRelation.getMembers().add(missingMember);
                    invalidRelation.getMissingMembers().remove(missingMember);
                    System.out.println("Missing member added!");
                }
                else {
                    try {
                        System.out.println("Querying for id = " + missingMember.getRef());
                        JsonNode response = OSMFinder.fetchWay(missingMember.getRef());

                        if (response != null && response.has("elements") && response.get("elements").isArray()) {
                            for (JsonNode element : response.get("elements")) {
                                if ("way".equals(element.get("type").asText())) {
                                    Way missingWay = new Way();
                                    missingWay.setId(element.get("id").asLong());

                                    // Check if the way has nodes (should always have)
                                    if (element.has("nodes") && element.get("nodes").isArray()) {
                                        for (JsonNode nodeRef : element.get("nodes")) {
                                            long nodeId = nodeRef.asLong();

                                            Node node = dataStore.getNodes().get(nodeId);

                                            if (node != null) {
                                                System.out.println("Add node id = " + nodeId);
                                                missingWay.getNodes().add(node);
                                            }
                                            else {
                                                // Get all missing nodes for the missing way
                                                System.out.println("Fetching missing node id = " + nodeId);
                                                JsonNode responseNode = OSMFinder.fetchNode(nodeId);

                                                if (responseNode != null && responseNode.has("elements") && responseNode.get("elements").isArray()) {
                                                    for (JsonNode elementNode : responseNode.get("elements")) {
                                                        if ("node".equals(elementNode.get("type").asText())) {
                                                            Node missingNode = new Node();
                                                            missingNode.setId(elementNode.get("id").asLong());
                                                            missingNode.setLat(elementNode.get("lat").asDouble());
                                                            missingNode.setLon(elementNode.get("lon").asDouble());

                                                            // Check if the node has tags
                                                            if (elementNode.has("tags")) {
                                                                JsonNode tags = element.get("tags");
                                                                Iterator<String> fieldNames = tags.fieldNames();
                                                                while (fieldNames.hasNext()) {
                                                                    String key = fieldNames.next();
                                                                    String value = tags.get(key).asText();
                                                                    missingWay.getTags().put(key, value);
                                                                }
                                                            }

                                                            // Add new node
                                                            missingWay.getNodes().add(missingNode);

                                                            // Add to all nodes
                                                            dataStore.getNodes().put(nodeId, missingNode);

                                                            System.out.println("Node for way successfully fetched!");
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    // Check if the way has tags
                                    if (element.has("tags")) {
                                        JsonNode tags = element.get("tags");
                                        Iterator<String> fieldNames = tags.fieldNames();
                                        while (fieldNames.hasNext()) {
                                            String key = fieldNames.next();
                                            String value = tags.get(key).asText();
                                            missingWay.getTags().put(key, value);
                                        }
                                    }

                                    // Add new member
                                    invalidRelation.getMembers().add(missingMember);

                                    // Add to all ways
                                    dataStore.getWays().put(missingWay.getId(), missingWay);

                                    // remove from missing members of relation
                                    invalidRelation.getMissingMembers().remove(missingMember);
                                    System.out.println("Success!");
                                }
                            }
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }
            }

            if (invalidRelation.getMissingMembers().isEmpty()) {
                System.out.println("Relation fixed");

                // Add to valid relations
                invalidRelation.toGeometry();
                dataStore.getRelations().put(invalidRelation.getId(), invalidRelation);

                // Set to null for garbage collection
                invalidRelation.setMissingMembers(null);
            }
        }

        for (var entry : dataStore.getInvalidRelations()) {
            System.out.println(entry);
        }

        System.out.println("Finished fixing invalid relations!");
    }

    private static void startServer() {
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

}