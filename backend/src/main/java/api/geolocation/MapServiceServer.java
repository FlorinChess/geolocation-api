package api.geolocation;

import api.geolocation.osm.OSMParser;
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

        if (!dataStore.getInvalidWays().isEmpty()) {
            System.out.println("Ways containing invalid ");
        }
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