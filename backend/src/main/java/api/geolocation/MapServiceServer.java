package api.geolocation;

import api.geolocation.datamodels.Node;
import api.geolocation.datamodels.Way;
import api.geolocation.osm.OSMFinder;
import api.geolocation.osm.OSMParser;
import com.fasterxml.jackson.databind.JsonNode;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.ArrayList;
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

        System.out.println("Missing nodes: " + DataStore.getInstance().getMissingNodes().size());
        System.out.println("Missing ways:  " + DataStore.getInstance().getMissingWays().size());
        System.out.println("Invalid ways:  " + DataStore.getInstance().getMissingWays().size());

        // fixInvalidEntries();

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

        List<Long> toRemove = new ArrayList<>();

        System.out.println("Number of invalid ways: " + dataStore.getInvalidWays().size());
        for (var invalidWayEntry : dataStore.getInvalidWays().entrySet()) {

            Way way = invalidWayEntry.getValue();
            int nodesAdded = 0;
            int missingNodesCount = way.getMissingNodes().size();
            for (var missingNodeId : way.getMissingNodes()) {
                if (dataStore.getNodes().containsKey(missingNodeId)) {
                    System.out.println("Node fetched already! Skipping request...");
                    continue;
                }

                try {
                    JsonNode response = OSMFinder.fetchNode(missingNodeId);
                    final Node missingNode = new Node();

                    if (response.has("elements") && response.get("elements").isArray()) {
                        for (JsonNode element : response.get("elements")) {
                            if ("node".equals(element.get("type").asText())) {
                                missingNode.setId(element.get("id").asLong());
                                missingNode.setLat(element.get("lat").asDouble());
                                missingNode.setLon(element.get("lon").asDouble());

                                // Check if the node has tags
                                if (element.has("tags")) {
                                    JsonNode tags = element.get("tags");
                                    tags.fields().forEachRemaining(entry -> missingNode.getTags().put(entry.getKey(), entry.getValue().asText()));
                                }

                                way.getNodes().add(missingNode);

                                dataStore.getNodes().put(missingNodeId, missingNode);
                                System.out.println("Node added! id = " + missingNodeId);

                            }
                        }
                    }

                } catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
                nodesAdded++;
            }

            if (nodesAdded == missingNodesCount) {
                way.getMissingNodes().clear();
                way.setMissingNodes(null);
                System.out.println("Missing nodes of invalid way fixed! id = " + way.getId());

                toRemove.add(way.getId());
            }
            else
            {
                System.out.println("Could not fix invalid way!          id = " + way.getId());
            }

        }

        for (int i = 0; i < toRemove.size(); i++) {
            dataStore.getInvalidWays().remove(toRemove.get(i));
        }

        System.out.println("Invalid ways remaining: " + toRemove.size());
        System.out.println("Finished fixing missing entries!");
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