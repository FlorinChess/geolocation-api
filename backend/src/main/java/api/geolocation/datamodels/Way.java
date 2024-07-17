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
    private List<Long> nodeRefs;

    public Way() {
        tags = new HashMap<>();
        nodeRefs = new ArrayList<>();
    }

    public Way(long id, Map<String, String> tags, List<Long> nodeRefs) {
        this.id = id;
        this.tags = tags;
        this.nodeRefs = nodeRefs;
    }

    public List<Node> getListOfNodes() {
        ArrayList<Node> nodeWay = new ArrayList<>();
        for (var ref : nodeRefs) {
            nodeWay.add(DataStore.getInstance().getRoadsNodesMap().get(ref));
        }
        return nodeWay;
    }

    public Geometry toGeometry() throws RuntimeException {
        if (nodeRefs.isEmpty())
            throw new RuntimeException("Way with id " + id + " has no referenced nodes!");

        Coordinate[] coordinates = new Coordinate[nodeRefs.size()];
        int i = 0;
        for (var ref : nodeRefs){
            var node = DataStore.getInstance().getRoadsNodesMap().get(ref);

            if (node == null) continue;

            coordinates[i] = new Coordinate(node.getLon(), node.getLat());
            i++;
        }

        if (coordinates.length > 2 && Objects.equals(nodeRefs.get(0), nodeRefs.get((nodeRefs.size() - 1)))){
            LinearRing linearRing = DataStore.geometryFactory.createLinearRing(coordinates);
            return new Polygon(linearRing, null, DataStore.geometryFactory);
        }

        if (coordinates.length == 1) {
            return new Point(CoordinateArraySequenceFactory.instance().create(coordinates), DataStore.geometryFactory);
        }

        return new LineString(CoordinateArraySequenceFactory.instance().create(coordinates), DataStore.geometryFactory);
    }

}
