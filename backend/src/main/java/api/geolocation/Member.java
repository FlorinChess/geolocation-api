package api.geolocation;

import org.locationtech.jts.geom.*;

public class Member extends Geometry {
    String type;
    Long ref;
    String role;

    public Member(GeometryFactory factory) {
        super(factory);
    }

    @Override
    public String getGeometryType() {
        return null;
    }

    @Override
    public Coordinate getCoordinate() {
        return null;
    }

    @Override
    public Coordinate[] getCoordinates() {
        return new Coordinate[0];
    }

    @Override
    public int getNumPoints() {
        return 0;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public int getDimension() {
        return 0;
    }

    @Override
    public Geometry getBoundary() {
        return null;
    }

    @Override
    public int getBoundaryDimension() {
        return 0;
    }

    @Override
    protected Geometry reverseInternal() {
        return null;
    }

    @Override
    public boolean equalsExact(Geometry geometry, double v) {
        return false;
    }

    @Override
    public void apply(CoordinateFilter coordinateFilter) {

    }

    @Override
    public void apply(CoordinateSequenceFilter coordinateSequenceFilter) {

    }

    @Override
    public void apply(GeometryFilter geometryFilter) {

    }

    @Override
    public void apply(GeometryComponentFilter geometryComponentFilter) {

    }

    @Override
    protected Geometry copyInternal() {
        return null;
    }

    @Override
    public void normalize() {

    }

    @Override
    protected Envelope computeEnvelopeInternal() {
        return null;
    }

    @Override
    protected int compareToSameClass(Object o) {
        return 0;
    }

    @Override
    protected int compareToSameClass(Object o, CoordinateSequenceComparator coordinateSequenceComparator) {
        return 0;
    }

    @Override
    protected int getTypeCode() {
        return 0;
    }

    @Override
    public String toString() {
        return "Member{" +
                "type='" + type + '\'' +
                ", ref=" + ref +
                ", role='" + role + '\'' +
                '}';
    }
}
