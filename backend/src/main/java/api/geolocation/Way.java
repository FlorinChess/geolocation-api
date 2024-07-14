package api.geolocation;

import java.util.*;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;

public class Way {
    public long id;
    Map<String, String> tags;
    List<Long> nodeRefs;

    public Way() {
        tags = new HashMap<>();
        nodeRefs = new ArrayList<>();
    }

    public Way(long id,  Map<String, String> tags, List<Long> nodeRefs)
    {
        this.id = id;
        this.tags = tags;
        this.nodeRefs = nodeRefs;
    }

    List<Node> getListOfNodes() {
        ArrayList<Node> nodeWay = new ArrayList<>();
        for (var ref : nodeRefs) {
            nodeWay.add(MapServiceServer.getWayNodeById(ref));
        }
        return nodeWay;
    }

    public Geometry toGeometry() {
        Coordinate[] coordinates = new Coordinate[nodeRefs.size()];
        int i = 0;
        for (var ref : nodeRefs){
            var node = MapServiceServer.getWayNodeById(ref);

            if (node == null) continue;

            coordinates[i] = new Coordinate(node.lon, node.lat);
            i++;
        }

        if (coordinates.length > 2 && Objects.equals(nodeRefs.get(0), nodeRefs.get((nodeRefs.size() - 1)))){
            LinearRing linearRing = MapServiceServer.geometryFactory.createLinearRing(coordinates);
            return new Polygon(linearRing, null, MapServiceServer.geometryFactory);
        }

        if (coordinates.length == 1) {
            return new Point(CoordinateArraySequenceFactory.instance().create(coordinates), MapServiceServer.geometryFactory);
        }

        return new LineString(CoordinateArraySequenceFactory.instance().create(coordinates), MapServiceServer.geometryFactory);
    }

}
