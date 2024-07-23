package api.geolocation;

import java.util.Collections;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class MapApplication {
    public static CommunicationServiceGrpc.CommunicationServiceBlockingStub stub;
    private static int port;
    private static int backendPort;
    private static String backendTarget;
    private static String backendAddress;

    public static void main(String[] args) {

        parseEnvironmentVariables();

        initializeSpring();

        initializeGrpcStub();
    }

    private static void parseEnvironmentVariables() {
        try {
            port = Integer.parseInt(System.getenv().getOrDefault("JMAP_MIDDLEWARE_PORT", Constants.defaultMiddlewarePort));
            backendTarget = System.getenv().getOrDefault("JMAP_BACKEND_TARGET", Constants.defaultBackendTarget);

            if (port < Constants.minPortValue || port > Constants.maxPortValue)
                port = Integer.parseInt(Constants.defaultMiddlewarePort);

            if (backendTarget == null || backendTarget.isEmpty())
                backendTarget = Constants.defaultBackendTarget;

            String[] components = backendTarget.split(":");

            backendAddress = components[0];
            backendPort = Integer.parseInt(components[1]);

            if (backendPort < Constants.minPortValue || backendPort > Constants.maxPortValue || backendAddress.isEmpty())
                backendTarget = Constants.defaultBackendTarget;

        } catch (Exception ex) {
            port = Integer.parseInt(Constants.defaultMiddlewarePort);
            backendTarget = Constants.defaultBackendTarget;
        }
    }

    private static void initializeSpring() {
        SpringApplication app = new SpringApplication(MapApplication.class);
        app.setDefaultProperties(Collections.singletonMap("server.port", port));
        app.run();

        MapLogger.middlewareStartup(port, backendTarget);
    }

    private static void initializeGrpcStub() {
        String[] components = backendTarget.split(":");

        backendAddress = components[0];
        backendPort = Integer.parseInt(components[1]);

        ManagedChannel channel = ManagedChannelBuilder.forAddress(backendAddress, backendPort)
                .usePlaintext()
                .enableRetry()
                .build();

        stub = CommunicationServiceGrpc.newBlockingStub(channel);
    }
}