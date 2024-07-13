package api.geolocation;


import org.locationtech.jts.geom.Coordinate;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Area;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.*;
import java.util.List;

public class MapRenderer {

    double max_lon;
    double max_lat;
    double min_lon;
    double min_lat;

    int tileSize = 512;


    public BufferedImage renderTile(int zoom, int x, int y, String layers) throws IOException {
        BufferedImage image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // https://stackoverflow.com/questions/1094539/how-to-draw-a-decent-looking-circle-in-java
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        var waysMap = MapServiceServer.waysMap;
        var relationsMap = MapServiceServer.relationsMap;
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

        g.setColor(Color.WHITE);
        g.fillRect(0,0, image.getWidth(), image.getHeight());

        BoundingBox bbox = tile2boundingBox(x, y, zoom);

        max_lat = bbox.north;
        max_lon = bbox.east;
        min_lon = bbox.west;
        min_lat = bbox.south;

        String[] layersArray = layers.split(",");

        List<Way> roads = waysMap.values().stream().filter(way -> way.tags.containsKey("highway")).toList();

        List<Relation> landRelations = relationsMap.values().stream().filter(relation -> relation.tags.containsKey("landuse")).toList();

        List<Relation> waterRelations = relationsMap.values().stream().filter(relation -> relation.tags.containsKey("water")).toList();

        List<Way> waterWae = waysMap.values().stream().filter(way -> way.tags.containsKey("water")).toList();

        for (String layer : layersArray) {
            List<Way> selectedRoads = roads.stream()
                    .filter(way -> way.tags.get("highway").equals(layer) || (!(isRoad(way.tags.get("highway"))) && layer.equals("road")))
                    .toList();
            if (layer.equals("road")) {
                drawRoadSPlural(selectedRoads, giveColor("road"), g);
            } else {
                drawRoadSPlural(selectedRoads, giveColor(layer), g);
            }

            List<Relation> selectedRelation = landRelations.stream().filter(relation -> relation.tags.get("landuse").equals(layer)).toList();
            drawLandSPlural(selectedRelation, giveColor(layer), g);

            if (layer.equals("water")){
                drawLandSPlural(waterRelations, giveColor(layer), g);
                drawRoadSPlural(waterWae, giveColor(layer), g);
            }
        }

        image = flipImageCounterClockwise(image);

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ImageIO.write(image, "png", outputStream);
        //return outputStream.toByteArray();
        return image;
    }

    public Color giveColor(String type) {
        Color color = Color.WHITE;
        switch (type) {
            case "motorway":
                color = new Color(255,0,0);
                break;
            case "trunk":
                color = new Color(255,140,0);
                break;
            case "primary":
                color = new Color(255,165,0);
                break;
            case "secondary":
                color = new Color(255,255,0);
                break;
            case "road":
                color = new Color(128,128,128);
                break;
            case "forest":
                color = new Color(173,209,158);
                break;
            case "residential":
                color = new Color(223,233,233);
                break;
            case "vineyard":
                color = new Color(172,224,161);
                break;
            case "grass":
                color = new Color(205,235,176);
                break;
            case "railway":
                color = new Color(235,219,233);
                break;
            case "water":
                color = new Color(0,128,255);
                break;
            default:
                break;
        }
        return color;
    }

    public boolean isRoad(String type) {
        boolean state = false;
        switch (type) {
            case "motorway":
            case "trunk":
            case "primary":
            case "secondary":
            case "water":
                state = true;
                break;
            default:
                break;
        }
        return state;
    }

    // sources: https://wiki.openstreetmap.org/wiki/Slippy_map_tilenames#Java
    static class BoundingBox {
        double north;
        double south;
        double east;
        double west;
    }

    private BoundingBox tile2boundingBox(int x, int y, int zoom) {
        BoundingBox bb = new BoundingBox();
        bb.north = tile2lat(y, zoom);
        bb.south = tile2lat(y + 1, zoom);
        bb.west = tile2lon(x, zoom);
        bb.east = tile2lon(x + 1, zoom);
        return bb;
    }

    static double tile2lon(int x, int z) {
        return x/Math.pow(2.0, z) * 360 - 180;
    }

    static double tile2lat(int y, int z) {
        double calc = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(calc)));
    }

    int transLat(double lat) {
        return (int)(((lat - min_lat) / (max_lat - min_lat)) * tileSize);
    }

    int transLon(double lon) {
        return (int)(((lon - min_lon) / (max_lon - min_lon)) * tileSize);
    }

    private void drawRoad(List<Node> ways, Color color, Graphics2D g) {
        Coordinate start_coord = null;
        Coordinate start_coord2 = null;
        Coordinate last_coord = null;
        int i = 0;
        for (Node way : ways) {
            if (way != null) {
                if (i == 0) {
                    start_coord2 = new Coordinate(way.lat, way.lon);
                }
                if (i == (ways.size() - 1)) {
                    last_coord = new Coordinate(way.lat, way.lon);
                }
                i++;
            }
        }

        // TODO: Find better solution
        if (start_coord2 != null && start_coord2.equals(last_coord)) {
            g.setColor(color);
            g.fill(nodelistToPoly(ways));
        }

        for (Node way : ways) {
            if (way != null) {
                Coordinate coordinate = new Coordinate(transLat(way.lat), transLon(way.lon));
                if (start_coord != null) {
                    g.setColor(color);
                    g.drawLine((int) start_coord.x, (int) start_coord.y, (int) coordinate.x, (int) coordinate.y);
                }
                start_coord = coordinate;
            }
        }
    }

    static double maxLat(List<Node> nodeLat) {
        double max_lat = 0;
        for (Node node : nodeLat) {
            if (node.lat > max_lat) {
                max_lat = node.lat;
            }
        }
        return max_lat;
    }

    static double maxLon(List<Node> nodeLon) {
        double max_lon = 0;
        for (Node node : nodeLon) {
            if (node.lon > max_lon) {
                max_lon = node.lon;
            }
        }
        return max_lon;
    }

    static double minLon(List<Node> nodeLon) {
        double min_lon = 0xB00B5;
        for (Node node : nodeLon) {
            if (node.lon < min_lon) {
                min_lon = node.lon;
            }
        }
        return min_lon;
    }

    static double minLat(List<Node> nodeLat) {
        double min_lat = 0xB00B5;
        for (Node node : nodeLat) {
            if (node.lat < min_lat) {
                min_lat = node.lat;
            }
        }
        return min_lat;
    }

    public Polygon nodelistToPoly(List<Node> nodeList) {
        Polygon polygon = new Polygon();
        int[] x = new int[nodeList.size()];
        int[] y = new int[nodeList.size()];

        for (int i = 0; i < nodeList.size(); i++){
            Node node = nodeList.get(i);
            x[i] = transLon(node.lon);
            y[i] = transLat(node.lat);
        }
        return new Polygon(y, x ,nodeList.size());
    }

    private void drawRoadSPlural(List<Way> wayList, Color color, Graphics2D g) {
        for (Way way : wayList) {
            drawRoad(way.getNodeWayList().stream().toList(), color, g);
        }
    }

    private void drawLand(ArrayList<org.locationtech.jts.geom.Polygon> innerPoly, ArrayList<org.locationtech.jts.geom.Polygon> outerPoly, Color color, Graphics2D g) {
        Area area = new Area();
        outerPoly.forEach(poly -> area.add(new Area(transPolygon(poly))));

        innerPoly.forEach(poly -> area.subtract(new Area(transPolygon(poly))));

        /*if (color.equals(new Color(173,209,158))){
            System.out.println("forest");
        } */
        g.setColor(color);
        g.fill(area);
    }

    private void drawLandSPlural(List<Relation> relationList, Color color, Graphics2D g) {
        for (Relation relation : relationList) {
            drawLand(relation.getInnerPolygons(), relation.getOuterPolygons(), color, g);
        }
    }

    private Polygon transPolygon(org.locationtech.jts.geom.Polygon badPolygon) {
        int[] x = new int[badPolygon.getNumPoints()];
        int[] y = new int[badPolygon.getNumPoints()];

        for (int i = 0; i < badPolygon.getNumPoints(); i++){
            x[i] = transLon(badPolygon.getCoordinates()[i].x);
            y[i] = transLat(badPolygon.getCoordinates()[i].y);
        }
        return new Polygon(y, x, badPolygon.getNumPoints());
    }

    public BufferedImage flipImageCounterClockwise(BufferedImage flipped) {
        for (int i = 0; i < 3; i++){
            flipped = flipImag(flipped);
        }
        return flipped;
    }

    public BufferedImage flipImag(BufferedImage imag) {
        int width = tileSize;
        int height = tileSize;

        BufferedImage flipped = new BufferedImage(width, height, imag.getType());
        Graphics2D g2 = flipped.createGraphics();

        AffineTransform transform = new AffineTransform();
        transform.translate(height, 0);
        transform.rotate(Math.toRadians(90));

        g2.setTransform(transform);
        g2.drawImage(imag, 0, 0, null);

        return flipped;
    }
}
