package api.geolocation;

import api.geolocation.osm.OSMParser;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import java.util.logging.Logger;

public class MapServiceServer {
    private final static Logger logger = Logger.getLogger(MapServiceServer.class.getName());

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

        OSMParser osmParser = new OSMParser(backendOsmFile);
        osmParser.parse();

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
}