package api.geolocation;

import api.geolocation.datamodels.Node;
import api.geolocation.datamodels.Way;
import api.geolocation.osm.OSMFinder;
import api.geolocation.osm.OSMParser;
import com.fasterxml.jackson.databind.JsonNode;
import io.grpc.Server;
import io.grpc.ServerBuilder;

import java.util.logging.Logger;

public class MapServiceServer {
    private final static Logger logger = Logger.getLogger(MapServiceServer.class.getName());
    private static int port;
    private static String backendOsmFile;

    public static void main(String[] args) {
        logger.info("Starting backend...");

        parseEnvironmentVariables();

        parseOSMFile();

        //fixInvalidEntries();

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
        System.out.println("Number of invalid ways: " + dataStore.getInvalidWays().size());
        for (Way invalidWay : dataStore.getInvalidWays()) {
            for (var missingNodeId : invalidWay.getMissingNodes()) {
                if (dataStore.getNodes().containsKey(missingNodeId)) {
                    System.out.println("Node fetched already! Skipping request...");
                    continue;
                }

                try {
                    System.out.println("Querying for id = " + missingNodeId);
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

                                invalidWay.getNodes().add(missingNode);

                                dataStore.getNodes().put(missingNodeId, missingNode);
                                System.out.println("Node added! id = " + missingNodeId);
                            }
                        }
                    }
                } catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }

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