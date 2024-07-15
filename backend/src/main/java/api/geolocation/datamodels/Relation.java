package api.geolocation.datamodels;
import api.geolocation.IOSMDataModel;
import api.geolocation.MapServiceServer;
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

            if (closedCircle != null && closedCircle.lastRole.equals("inner")) {
                innerPolygons.add(closedCircle.polygon);
            }

            i = (int) (closedCircle != null ? closedCircle.lastMemberIndex + 1 : i + 1);
        }
        return innerPolygons;
    }

    public ArrayList<Polygon> getOuterPolygons() {
        ArrayList<Polygon> outerPolygons = new ArrayList<>();

        for (int i = 0; i < members.size();) {
            ClosedCircleResult closedCircle = getNextClosed(i, members);

            if (closedCircle != null && closedCircle.lastRole.equals("outer")) {
                outerPolygons.add(closedCircle.polygon);
            }

            i = (int) (closedCircle != null ? closedCircle.lastMemberIndex + 1 : i + 1);
        }
        return outerPolygons;
    }

    public GeometryCollection toGeometry() {
        if (tags.containsValue("multipolygon")) {
            List<MultiPolygon> multiPolygons = new ArrayList<>();
            List<Polygon> inners = new ArrayList<>();
            Polygon outer = null;

            for (int i = 0; i < members.size();) {
                ClosedCircleResult closedCircle = getNextClosed(i, members);

                if (closedCircle != null) {
                    if (closedCircle.lastRole.equals("outer")) {
                        if (outer != null) {
                            var polygons = new ArrayList<Polygon>();
                            polygons.add(outer);
                            polygons.addAll(inners);

                            multiPolygons.add(buildMultipolygon(polygons));
                            inners.clear();
                        }
                        outer = closedCircle.polygon;
                    }
                    else if (closedCircle.lastRole.equals("inner")) {
                        inners.add(closedCircle.polygon);
                    }

                    i = (int) (closedCircle.lastMemberIndex + 1);
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
            return buildGeometryCollection(multiPolygons);
        }
        else {
            return buildGeometryCollection(members);
        }
    }


    private GeometryCollection buildGeometryCollection(List<? extends Geometry> geometries) {
        Geometry[] geometryArray = new Geometry[geometries.size()];

        for (int i = 0; i < geometries.size(); i++) {
            geometryArray[i] = geometries.get(i);
        }

        return new GeometryCollection(geometryArray, MapServiceServer.geometryFactory);
    }

    private static ClosedCircleResult getNextClosed(int i, List<Member> members) {
        String lastRole = "";
        List<Geometry> geometries = new ArrayList<>();

        for (; i < members.size(); i++) {
            Member member = members.get(i);
            Way way = MapServiceServer.getRelationWayById(member.ref);

            if (way == null)
                continue;

            var geometry = way.toGeometry();

            if (geometry.getGeometryType().equals(Geometry.TYPENAME_POLYGON)) {
                return new ClosedCircleResult((Polygon) geometry, member.role, i);
            }

            geometries.add(geometry);
            lastRole = member.role;
        }


        LineMerger lineMerger = new LineMerger();
        lineMerger.add(geometries);
        var mergedLinesCollection = lineMerger.getMergedLineStrings();
        if (mergedLinesCollection.size() == 1) {
            try {
                var lineString = (LineString) mergedLinesCollection.stream().findFirst().get();

                LinearRing linearRing = MapServiceServer.geometryFactory.createLinearRing(lineString.getCoordinates());

                Polygon polygon = new Polygon(linearRing, null, MapServiceServer.geometryFactory);
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

        return new MultiPolygon(polygonArray, MapServiceServer.geometryFactory);
    }
}
