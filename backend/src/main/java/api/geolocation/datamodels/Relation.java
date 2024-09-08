package api.geolocation.datamodels;

import api.geolocation.DataStore;
import lombok.Data;
import org.locationtech.jts.geom.*;
import org.locationtech.jts.operation.linemerge.LineMerger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
public class Relation implements IOSMDataModel {
    private long id;
    private List<Member> members;
    private List<Long> missingWays;
    private Map<String, String> tags;

    public Relation() {
        members = new ArrayList<>();
        missingWays = new ArrayList<>();
        tags = new HashMap<>();
    }

    public ArrayList<LinearRing> getInnerPolygons() {
        ArrayList<LinearRing> innerRings = new ArrayList<>();

        for (int i = 0; i < members.size();) {
            ClosedCircle closedCircle = getNextClosed(i, members);

            if (closedCircle != null && closedCircle.getLastRole().equals("inner")) {
                innerRings.add(closedCircle.getLinearRing());
            }

            i = (int) (closedCircle != null ? closedCircle.getLastMemberIndex() + 1 : i + 1);
        }
        return innerRings;
    }

    public ArrayList<LinearRing> getOuterPolygons() {
        ArrayList<LinearRing> outerRings = new ArrayList<>();

        for (int i = 0; i < members.size();) {
            ClosedCircle closedCircle = getNextClosed(i, members);

            if (members.get(i).getRole().equals("outline")) {
                Way way = DataStore.getInstance().getWays().get(members.get(0).getRef());
                outerRings.add((LinearRing) way.toGeometry());
                return outerRings;
            }

            if (closedCircle != null && closedCircle.getLastRole().equals("outer")) {
                outerRings.add(closedCircle.getLinearRing());
            }

            i = (int) (closedCircle != null ? closedCircle.getLastMemberIndex() + 1 : i + 1);
        }
        return outerRings;
    }

    // TODO: manage geometries with members with roles "outline" and "part" (e.g. id = 8258274)
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

            return buildMultiPolygon(polygons);
        }

        return null;
    }

    private MultiPolygon buildMultiPolygon(List<Polygon> polygons) {
        return DataStore.geometryFactory.createMultiPolygon(polygons.toArray(new Polygon[0]));
    }

    private ClosedCircle getNextClosed(int i, List<Member> members) {
        String lastRole = "";
        List<Geometry> geometries = new ArrayList<>();

        for (; i < members.size(); i++) {
            Member member = members.get(i);
            Way way = DataStore.getInstance().getWays().get(member.getRef());

            Geometry geometry = way.toGeometry();

            if (geometry.getGeometryType().equals(Geometry.TYPENAME_LINEARRING)) {
                return new ClosedCircle((LinearRing) geometry, member.getRole(), i);
            }

            geometries.add(geometry);
            lastRole = member.getRole();
        }

        LineMerger lineMerger = new LineMerger();
        lineMerger.add(geometries);
        var mergedLinesCollection = lineMerger.getMergedLineStrings();
        if (mergedLinesCollection.size() == 1) {
            try {
                var lineString = (LineString) mergedLinesCollection.stream().findFirst().get();

                LinearRing linearRing = DataStore.geometryFactory.createLinearRing(lineString.getCoordinates());

                return new ClosedCircle(linearRing, lastRole, i);
            }
            catch (Exception ex) {
                System.out.println("Invalid geometry! id = " + id);
                ex.printStackTrace(System.out);
            }
        }
        else {
            return null;
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
