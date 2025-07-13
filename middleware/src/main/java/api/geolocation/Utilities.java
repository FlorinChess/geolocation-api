package api.geolocation;

import org.json.simple.parser.JSONParser;

public class Utilities {
    public static JSONParser jsonParser = new JSONParser();

    public static boolean isLatitudeValid(double latitude) {
        return (-90.0 <= latitude && latitude <= 90.0);
    }

    public static boolean isLongitudeValid(double longitude) {
        return (-180.0 <= longitude && longitude <= 180.0);
    }

    public static boolean areBoundingBoxParametersValid(double bboxTlX, double bboxTlY, double bboxBrX, double bboxBrY) {
        return (Utilities.isLongitudeValid(bboxTlX) &&
                Utilities.isLatitudeValid(bboxTlY)  &&
                Utilities.isLongitudeValid(bboxBrX) &&
                Utilities.isLatitudeValid(bboxBrY));
    }
}
