package api.geolocation;
import java.util.*;

import org.locationtech.jts.geom.*;
import org.locationtech.jts.geom.impl.CoordinateArraySequenceFactory;

public class Way {
    long id;
    List<Long> nodeRefs;
    Map<String, String> tags;

    public Way() {
        tags = new HashMap<>();
        nodeRefs = new ArrayList<>();
    }

    ArrayList<Node> getNodeWayList() {
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
            LinearRing linearRing = new GeometryFactory().createLinearRing(coordinates);
            return new Polygon(linearRing, null, new GeometryFactory());
        }

        if (coordinates.length == 1) {
            return new Point(CoordinateArraySequenceFactory.instance().create(coordinates), new GeometryFactory());
        }

        return new LineString(CoordinateArraySequenceFactory.instance().create(coordinates), new GeometryFactory());
    }

}
