package api.geolocation;

import api.geolocation.datamodels.*;
import lombok.Data;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class DataStore {
    public static DataStore instance = null;
    private final HashMap<Long, Node> nodes;
    private final HashMap<Long, Way> ways;
    private final HashMap<Long, Relation> relations;
    private final HashMap<Long, AmenityModel> amenities;
    private final HashMap<Long, RoadModel> roads;

    private DataStore() {
        nodes = new HashMap<>();
        ways = new HashMap<>();
        relations = new HashMap<>();
        amenities = new HashMap<>();
        roads = new HashMap<>();
    }

    public void addNode(long id, double lat, double lon, Map<String, String> tags) {
        nodes.put(id, new Node(id, lat, lon, tags));
    }

    public void addWay(long id, Map<String, String> tags, List<Long> nodeReferences) {
        ways.put(id, new Way(id, tags, nodeReferences));
    }

    public void addRelation(long id, List<Member> members, Map<String, String> tags) {
        relations.put(id, new Relation(id, members, tags));
    }

    public static DataStore getInstance() {
        if (instance == null)
            instance = new DataStore();
        return instance;
    }
}
