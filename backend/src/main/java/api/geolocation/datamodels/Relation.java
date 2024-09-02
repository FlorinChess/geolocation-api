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
    private Map<String, String> tags;

    public Relation() {
        members = new ArrayList<>();
        tags = new HashMap<>();
    }

    public Relation(long id, List<Member> members, Map<String, String> tags) {
        this.id = id;
        this.members = members;
        this.tags = tags;
    }

    public ArrayList<Polygon> getInnerPolygons() {
        ArrayList<Polygon> innerPolygons = new ArrayList<>();

        for (int i = 0; i < members.size();) {
            ClosedCircleResult closedCircle = getNextClosed(i, members);

            if (closedCircle != null && closedCircle.getLastRole().equals("inner")) {
                innerPolygons.add(closedCircle.getPolygon());
            }

            i = (int) (closedCircle != null ? closedCircle.getLastMemberIndex() + 1 : i + 1);
        }
        return innerPolygons;
    }

    public ArrayList<Polygon> getOuterPolygons() {
        ArrayList<Polygon> outerPolygons = new ArrayList<>();

        for (int i = 0; i < members.size();) {
            ClosedCircleResult closedCircle = getNextClosed(i, members);

            if (closedCircle != null && closedCircle.getLastRole().equals("outer")) {
                outerPolygons.add(closedCircle.getPolygon());
            }

            i = (int) (closedCircle != null ? closedCircle.getLastMemberIndex() + 1 : i + 1);
        }
        return outerPolygons;
    }

    public GeometryCollection toGeometry() throws RuntimeException {
        if (tags.containsValue("multipolygon")) {
            List<MultiPolygon> multiPolygons = new ArrayList<>();
            List<Polygon> inners = new ArrayList<>();
            Polygon outer = null;

            for (int i = 0; i < members.size();) {
                ClosedCircleResult closedCircle = getNextClosed(i, members);

                if (closedCircle != null) {
                    if (closedCircle.getLastRole().equals("outer")) {
                        if (outer != null) {
                            var polygons = new ArrayList<Polygon>();
                            polygons.add(outer);
                            polygons.addAll(inners);

                            multiPolygons.add(buildMultipolygon(polygons));
                            inners.clear();
                        }
                        outer = closedCircle.getPolygon();
                    }
                    else if (closedCircle.getLastRole().equals("inner")) {
                        inners.add(closedCircle.getPolygon());
                    }

                    i = (int) (closedCircle.getLastMemberIndex() + 1);
                }
                else {
                    throw new RuntimeException("Error when generating geometry for relation with id: " + id);
                }
            }

            if (outer != null) {
                var polygons = new ArrayList<Polygon>();
                polygons.add(outer);
                polygons.addAll(inners);

                multiPolygons.add(buildMultipolygon(polygons));
                inners.clear();
            }
            return multiPolygonsToGeometryCollection(multiPolygons);
        }
        else {
            // TODO: split into 2 separate functions
//            return buildGeometryCollection(members);
            return null;
        }
    }

    private GeometryCollection multiPolygonsToGeometryCollection(List<MultiPolygon> geometries) {
        Geometry[] geometryArray = new Geometry[geometries.size()];

        for (int i = 0; i < geometries.size(); i++) {
            geometryArray[i] = geometries.get(i);
        }

        return new GeometryCollection(geometryArray, DataStore.geometryFactory);
    }

    private GeometryCollection membersToGeometryCollection(List<Member> members) {
        return null;
    }

    private ClosedCircleResult getNextClosed(int i, List<Member> members) {
        String lastRole = "";
        List<Geometry> geometries = new ArrayList<>();

        for (; i < members.size(); i++) {
            Member member = members.get(i);
            Way way = DataStore.getInstance().getWaysRelationMap().get(member.getRef());

            if (way == null)
                throw new RuntimeException("Relation (id = "  + id + ") is missing referenced way (ref = " + member.getRef() + ")!");

            Geometry geometry = way.toGeometry();

            if (geometry.getGeometryType().equals(Geometry.TYPENAME_POLYGON)) {
                return new ClosedCircleResult((Polygon) geometry, member.getRole(), i);
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

                Polygon polygon = new Polygon(linearRing, null, DataStore.geometryFactory);
                return new ClosedCircleResult(polygon, lastRole, i);
            }
            catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }

        return null;
    }

    private MultiPolygon buildMultipolygon(ArrayList<Polygon> polygons) {
        var polygonArray = new Polygon[polygons.size()];

        for (int i = 0; i < polygons.size(); i++) {
            polygonArray[i] = polygons.get(i);
        }

        return new MultiPolygon(polygonArray, DataStore.geometryFactory);
    }

    public OSMDataModelType getType() {
        return OSMDataModelType.Relation;
    }
}
