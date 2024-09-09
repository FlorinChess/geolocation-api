package api.geolocation.datamodels;

import api.geolocation.DataStore;
import lombok.Data;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;

import java.util.*;

@Data
public class Relation implements IOSMDataModel {
    private long id;
    private List<Member> members;
    private List<Long> missingWays;
    private Map<String, String> tags;
    private List<LinearRing> innerLinearRings;
    private List<LinearRing> outerLinearRings;

    public Relation() {
        members = new ArrayList<>();
        missingWays = new ArrayList<>();
        tags = new HashMap<>();
        innerLinearRings = new ArrayList<>();
        outerLinearRings = new ArrayList<>();
    }

    public Geometry toGeometryExperimental() {
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
                default:
                    // TODO buildings
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
        return null;
    }

    // TODO: manage geometries with members with roles "outline" and "part" (e.g. id = 8258274)
    // TODO: rework this shit; it is way too complicated and buggy
    public MultiPolygon toGeometry() throws RuntimeException {
        if (tags.containsValue("multipolygon")) {
            List<Polygon> polygons = new ArrayList<>();
            List<LinearRing> inners = new ArrayList<>();
            LinearRing outer = null;
            for (int i = 0; i < members.size();) {
                ClosedCircle closedCircle = getNextClosed(i, members);

                if (closedCircle != null) {
                    if (closedCircle.getLastRole().equals("outer")) {
                        if (outer != null) {
                            polygons.add(buildPolygon(outer, inners));
                            inners.clear();
                        }
                        outer = closedCircle.getLinearRing();
                    }
                    else if (closedCircle.getLastRole().equals("inner")) {
                        inners.add(closedCircle.getLinearRing());
                    }

                    i = (int) (closedCircle.getLastMemberIndex() + 1);
                }
                else {
                    throw new RuntimeException("Error when generating geometry for relation with id: " + id);
                }
            }

            if (outer != null) {
                polygons.add(buildPolygon(outer, inners));
                inners.clear();
            }
            return buildMultiPolygon(polygons);
        }
        else if (tags.containsKey("building")) {
            // TODO: implement mapping of building outlines; check https://wiki.openstreetmap.org/wiki/Simple_3D_Buildings#Building_outlines
            List<LinearRing> inners = new ArrayList<>();
            LinearRing outer = null;

            for (int i = 0; i < members.size(); i++) {
                if (members.get(i).getRole().equals("outline")) {
                    Way way = DataStore.getInstance().getWays().get(members.get(i).getRef());
                    outer = (LinearRing) way.toGeometry();
                }

                if (members.get(i).getRole().equals("part")) {
                    Way way = DataStore.getInstance().getWays().get(members.get(i).getRef());
                    inners.add((LinearRing) way.toGeometry());
                }
            }

            var polygons = new ArrayList<Polygon>();
            polygons.add(buildPolygon(outer, inners));

            innerLinearRings.addAll(inners);
            outerLinearRings.add(outer);

            return buildMultiPolygon(polygons);
        }

        return null;
    }

    private MultiPolygon buildMultiPolygon(List<Polygon> polygons) {
        return DataStore.geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
    }

    private ClosedCircle getNextClosed(int i, List<Member> members) {
        String role = members.get(i).getRole();
        List<Geometry> geometries = new ArrayList<>();

        if (id == 22203)
            System.out.println("test");

        // TODO: BUG
        // 1) outer1, inner1, inner2, outer2, outer3 -> outer ring is correctly formed, inner ring(s) get ignored
        // 2) start an outer completion, reach another outer member that forms a complete ring, continue from there
        // -> the previous and the following outer rings cannot be closed

        for (; i < members.size(); i++) {
            Member member = members.get(i);
            Way way = DataStore.getInstance().getWays().get(member.getRef());

            Geometry geometry = way.toGeometry();

            // Explanation:
            // if the member is references a full LinearRing then return immediately
            // if not avoid this part
            if (geometry.getGeometryType().equals(Geometry.TYPENAME_LINEARRING) && member.getRole().equals(role)) {
                if (role.equals("inner"))
                    innerLinearRings.add((LinearRing) geometry);
                else if (role.equals("outer"))
                    outerLinearRings.add((LinearRing) geometry);
                return new ClosedCircle((LinearRing) geometry, member.getRole(), i);
            }
            else if (geometry.getGeometryType().equals(Geometry.TYPENAME_LINESTRING) && member.getRole().equals(role)) {
                geometries.add(geometry);

                LineMerger lineMerger = new LineMerger();
                lineMerger.add(geometries);
                var mergedLinesCollection = lineMerger.getMergedLineStrings();
                if (mergedLinesCollection.size() == 1) {
                    try {
                        var lineString = (LineString) mergedLinesCollection.stream().findFirst().get();

                        LinearRing linearRing = DataStore.geometryFactory.createLinearRing(lineString.getCoordinates());

                        if (role.equals("inner"))
                            innerLinearRings.add(linearRing);
                        else if (role.equals("outer"))
                            outerLinearRings.add(linearRing);

                        return new ClosedCircle(linearRing, role, i);
                    }
                    catch (Exception ex) {
                        continue;
                    }
                }
            }
        }

        return null;
    }

    private Polygon buildPolygon(LinearRing shell, List<LinearRing> holes) {
        return DataStore.geometryFactory.createPolygon(shell, holes.toArray(new LinearRing[0]));
    }

    public OSMDataModelType getType() {
        return OSMDataModelType.Relation;
    }
}
