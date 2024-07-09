package api.geolocation;

import java.util.List;
import java.util.logging.Logger;

public class MapLogger {
    private static final Logger logger = Logger.getLogger(MapLogger.class.getName());

    /**
     * Log in the Middleware after parsing the environment variables and pass the ones you will use!
     *
     * @param port
     * @param backend_target
     */
    public static void middlewareStartup(int port, String backend_target) {
        logger.info("Middleware starting, port: " + port + ", backend target:" + backend_target + "!");
    }

    /**
     * Log in the Backend after parsing the environment variables and pass the ones you will use!
     *
     * @param port
     * @param osm_file
     */

    public static void backendStartup(int port, String osm_file) {
        logger.info("Backend starting, port: " + port + ", OSM File:" + osm_file + "!");
    }

    /**
     * Log in the Backend after finishing loading all the nodes (this should not take longer than 5 seconds!) and log
     * the amount of types *not* referenced by other entities!
     *
     * @param nodes
     * @param ways
     * @param relations
     */
    public static void backendLoadFinished(long nodes, long ways, long relations) {
        logger.info("Finished Loading: " + nodes + " nodes, " + ways + " ways, " + relations + " relations!");   
    }

    /**
     * Log in the Backend after receiving a request for amenities
     */
    public static void backendLogAmenitiesRequest() {
        logger.info("Backend got request for amenities!");
    }

    /**
     * Log in the Backend after receiving a request for a single amenity
     * @param id the id requested
     */

    public static void backendLogAmenityRequest(int id) {
        logger.info("Backend got request for single amenity, id: " + id);
    }

    /**
     * Log in the Backend after receiving a request for roads
     */
    public static void backendLogRoadsRequest() {
        logger.info("Backend got request for roads!");
    }
    /**
     * Log in the Backend after receiving a request for a single road
     * @param id the id requested
     */

    public static void backendLogRoadRequest(int id) {
        logger.info("Backend got request for single road, id: " + id);
    }

    /**
     * Log on the backend after receiving a request for a Map Tile
     * @param x x-tile ID
     * @param y y-tile ID
     * @param z z-tile ID
     * @param layers the layers you are requested to draw (in the correct order!)
     */
    public static void backendLogMapRequest(int x, int y, int z, List<String> layers) {
        logger.info("Backend got request for map " + x + "/" + y + "/" + z + " with layers: " + String.join(",", layers));
    }

    /**
     * Log on the Backend after determining all applicable OSM entities
     * @param ids the ID's of all entities you are about to draw
     */
    public static void backendLogMapEntities(List<Long> ids) {
        logger.info("Backend handled request for map, found " + ids.size() + " drawable entities!");

    }
}
