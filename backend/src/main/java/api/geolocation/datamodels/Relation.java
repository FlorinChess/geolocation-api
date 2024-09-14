package api.geolocation.datamodels;

import api.geolocation.DataStore;
import lombok.Data;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;
import java.util.*;

@Data
public class Relation {
    private long id;
    private List<Member> members;
    private List<Member> missingMembers;
    private Map<String, String> tags;
    private List<LinearRing> innerLinearRings;
    private List<LinearRing> outerLinearRings;

    public Relation() {
        members = new ArrayList<>();
        missingMembers = new ArrayList<>();
        tags = new HashMap<>();
        innerLinearRings = new ArrayList<>();
        outerLinearRings = new ArrayList<>();
    }

    public Geometry toGeometry() {
        List<LineString> outerLineStrings = new ArrayList<>();
        List<LineString> innerLineStrings = new ArrayList<>();

        members.forEach(member -> {
            Way way = DataStore.getInstance().getWays().get(member.getRef());
            Geometry geometry = way.toGeometry();

            switch (member.getRole()) {
                case "outer":
                    if (geometry.getGeometryType().equals(Geometry.TYPENAME_LINESTRING))
                        outerLineStrings.add((LineString) geometry);
                    else if (geometry.getGeometryType().equals(Geometry.TYPENAME_LINEARRING))
                        outerLinearRings.add((LinearRing) geometry);
                    break;
                case "inner":
                    if (geometry.getGeometryType().equals(Geometry.TYPENAME_LINESTRING))
                        innerLineStrings.add((LineString) geometry);
                    else if (geometry.getGeometryType().equals(Geometry.TYPENAME_LINEARRING))
                        innerLinearRings.add((LinearRing) geometry);
                    break;
                case "outline":
                    outerLinearRings.add((LinearRing) geometry);
                    break;
                case "part":
                    innerLinearRings.add((LinearRing) geometry);
                    break;
                default:
                    break;
            }
        });

        // Build outer rings from line strings
        if (!outerLineStrings.isEmpty()) {
            LineMerger lineMerger = new LineMerger();
            lineMerger.add(outerLineStrings);

            var mergedLineStrings = lineMerger.getMergedLineStrings();
            for (int i = 0; i < mergedLineStrings.size(); i++) {
                try {
                    LineString lineString = (LineString) mergedLineStrings.stream().findFirst().get();
                    LinearRing linearRing = DataStore.geometryFactory.createLinearRing(lineString.getCoordinates());
                    outerLinearRings.add(linearRing);
                }
                catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }

        // Build inner rings from line strings
        if (!innerLineStrings.isEmpty()) {
            LineMerger lineMerger = new LineMerger();
            lineMerger.add(innerLineStrings);

            var mergedLineStrings = lineMerger.getMergedLineStrings();
            for (int i = 0; i < mergedLineStrings.size(); i++) {
                try {
                    LineString lineString = (LineString) mergedLineStrings.stream().findFirst().get();
                    LinearRing linearRing = DataStore.geometryFactory.createLinearRing(lineString.getCoordinates());
                    innerLinearRings.add(linearRing);
                }
                catch (Exception ex) {
                    ex.printStackTrace(System.out);
                }
            }
        }

        // TODO: build outer and inner polygons, populate multipolygon and return it
        List<Polygon> polygons = new ArrayList<>();
        for (LinearRing outerLinearRing : outerLinearRings) {

            List<LinearRing> holes = new ArrayList<>();

            for (LinearRing innerLinearRing : innerLinearRings) {
                if (outerLinearRing.contains(innerLinearRing)) {
                    holes.add(innerLinearRing);
                }
            }

            polygons.add(buildPolygon(outerLinearRing, holes));
        }

        return buildMultiPolygon(polygons);
    }

    private MultiPolygon buildMultiPolygon(List<Polygon> polygons) {
        if (polygons.isEmpty())
            return null;

        return DataStore.geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
    }

    private Polygon buildPolygon(LinearRing shell, List<LinearRing> holes) {
        if (holes.isEmpty())
            return DataStore.geometryFactory.createPolygon(shell, null);
        else
            return DataStore.geometryFactory.createPolygon(shell, holes.toArray(new LinearRing[0]));
    }
}
