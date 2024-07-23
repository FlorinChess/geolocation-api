package api.geolocation;

import org.json.simple.parser.JSONParser;

public class Utilities {
    public static JSONParser jsonParser = new JSONParser();

    public static boolean latitudeIsValid(double latitude) {
        return (-90.0 <= latitude && latitude <= 90.0);
    }

    public static boolean longitudeIsValid(double longitude) {
        return (-180.0 <= longitude && longitude <= 180.0);
    }
}
