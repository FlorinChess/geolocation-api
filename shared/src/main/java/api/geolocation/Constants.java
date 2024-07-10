package api.geolocation;

public class Constants {
    public static final int minPortValue = 0;
    public static final int maxPortValue = 65535;
    public static final String defaultMiddlewarePort = "8010";
    public static final String defaultBackendTarget = "localhost:8020";
    public static final String defaultBackendPort = "8020";
    public static final String defaultBackendOsmFile = "data/styria_reduced.osm";
    public static final String badRequestPointValidCoordinatesInvalid = "Bad request: bbox provided, but coordinates are invalid.";
}
