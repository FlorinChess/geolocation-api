package api.geolocation.datamodels;

import java.util.*;

import api.geolocation.DataStore;
import lombok.Data;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;

@Data
public class Way implements IOSMDataModel {
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

        if (nodeCount == 0)
            throw new RuntimeException("Way with id " + id + " has no referenced nodes!");

        Coordinate[] coordinates = new Coordinate[nodeCount];
        for (int i = 0;  i < nodeCount; i++) {
            if (nodes.get(i) == null)
                throw new RuntimeException("Node referenced by way is null!!?");

            coordinates[i] = new Coordinate(nodes.get(i).getLon(), nodes.get(i).getLat());
        }

        // TODO: check if equality check is correct
        if (coordinates.length > 2 && Objects.equals(nodes.get(0), nodes.get((nodes.size() - 1)))){
            LinearRing linearRing = DataStore.geometryFactory.createLinearRing(coordinates);
            return new Polygon(linearRing, null, DataStore.geometryFactory);
        }

        if (coordinates.length == 1) {
            return new Point(CoordinateArraySequenceFactory.instance().create(coordinates), DataStore.geometryFactory);
        }

        return new LineString(CoordinateArraySequenceFactory.instance().create(coordinates), DataStore.geometryFactory);
    }

    public List<Long> getNodeIds() {
        List<Long> ids = new ArrayList<>();
        for (int i = 0; i < nodes.size(); i++) {
            ids.add(nodes.get(i).getId());
        }

        return ids;
    }

    public OSMDataModelType getType() {
        return OSMDataModelType.Way;
    }

}
