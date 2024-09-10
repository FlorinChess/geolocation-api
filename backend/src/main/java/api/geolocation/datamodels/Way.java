package api.geolocation.datamodels;

import java.util.*;

import api.geolocation.DataStore;
import lombok.Data;
import org.locationtech.jts.geom.*;

@Data
public class Way {
    private long id;
    private Map<String, String> tags;
    private List<Long> missingNodes;
    private List<Node> nodes;
    private String role = null;

    public Way() {
        tags = new HashMap<>();
        missingNodes = new ArrayList<>();
        nodes = new LinkedList<>();
    }

    public Geometry toGeometry() throws RuntimeException {
        int nodeCount = nodes.size();

        if (nodeCount < 2)
            throw new RuntimeException("Way with id " + id + " has less than 2 referenced nodes!");

        Coordinate[] coordinates = new Coordinate[nodeCount];
        for (int i = 0;  i < nodeCount; i++) {
            coordinates[i] = new Coordinate(nodes.get(i).getLon(), nodes.get(i).getLat());
        }

        if (coordinates.length > 2 && Objects.equals(nodes.get(0), nodes.get((nodes.size() - 1))))
            return DataStore.geometryFactory.createLinearRing(coordinates);

        return DataStore.geometryFactory.createLineString(coordinates);
    }

    public List<Long> getNodeIds() {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            ids.add(nodes.get(i).getId());
        }

        return ids;
    }
}
