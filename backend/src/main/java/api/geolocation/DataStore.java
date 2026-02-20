package api.geolocation;

import api.geolocation.datamodels.*;
import lombok.Data;
import org.locationtech.jts.geom.GeometryFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DataStore {
    public static final GeometryFactory geometryFactory = new GeometryFactory();
    private static DataStore instance = null;
    private final Map<Long, Node> nodes;
    private final Map<Long, Way> ways;
    private final Map<Long, Way> waysWithHighwayTag;
    private final Map<Long, Way> waysWithWaterTag;
    private final Map<Long, Way> waysWithRailwayTag;
    private final Map<Long, Way> waysWithBuildingTag;
    private final Map<Long, Way> waysWithLeisureTag;
    private final Map<Long, Way> waysWithLanduseTag;
    private final Map<Long, Relation> relations;
    private final Map<Long, Relation> relationsWithLanduseTag;
    private final Map<Long, Relation> relationsWithLeisureTag;
    private final Map<Long, Relation> relationsWithWaterTag;
    private final Map<Long, Relation> relationsWithBuildingTag;
    private final Map<Long, Relation> relationsWithHighwayTag;
    private final Map<Long, AmenityModel> amenities;
    private final Map<Long, RoadModel> roads;
    private final List<Way> invalidWays;
    private final List<Relation> invalidRelations;

    private DataStore() {
        nodes = new HashMap<>();
        ways = new HashMap<>();
        waysWithHighwayTag = new HashMap<>();
        waysWithWaterTag = new HashMap<>();
        waysWithRailwayTag = new HashMap<>();
        waysWithBuildingTag = new HashMap<>();
        waysWithLeisureTag = new HashMap<>();
        waysWithLanduseTag = new HashMap<>();
        relations = new HashMap<>();
        relationsWithLanduseTag = new HashMap<>();
        relationsWithLeisureTag = new HashMap<>();
        relationsWithWaterTag = new HashMap<>();
        relationsWithBuildingTag = new HashMap<>();
        relationsWithHighwayTag = new HashMap<>();
        amenities = new HashMap<>();
        roads = new HashMap<>();
        invalidWays = new ArrayList<>();
        invalidRelations = new ArrayList<>();
    }

    public static DataStore getInstance() {
        if (instance == null)
            instance = new DataStore();
        return instance;
    }
}
