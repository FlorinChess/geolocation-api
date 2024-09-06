package api.geolocation;

import api.geolocation.datamodels.Node;
import api.geolocation.datamodels.Relation;
import api.geolocation.datamodels.Way;
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

    private double maxLon;
    private double maxLat;
    private double minLon;
    private double minLat;

    private final int tileSize = 512;
    private final DataStore dataStore = DataStore.getInstance();

    public BufferedImage renderTile(int zoom, int x, int y, String layers) throws IOException {
        BufferedImage image = new BufferedImage(tileSize, tileSize, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();

        // https://stackoverflow.com/questions/1094539/how-to-draw-a-decent-looking-circle-in-java
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        var waysMap = dataStore.getWays();
        var relationsMap = dataStore.getRelations();
        g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_ROUND));

        g.setColor(Color.WHITE);
        g.fillRect(0,0, image.getWidth(), image.getHeight());

        BoundingBox bbox = tile2boundingBox(x, y, zoom);

        maxLat = bbox.north;
        maxLon = bbox.east;
        minLon = bbox.west;
        minLat = bbox.south;

        String[] layersArray = layers.split(",");

        List<Way> roads = waysMap.values().stream().filter(way -> way.getTags().containsKey("highway")).toList();

        List<Relation> landRelations = relationsMap.values().stream().filter(relation -> relation.getTags().containsKey("landuse")).toList();

        List<Relation> waterRelations = relationsMap.values().stream().filter(relation -> relation.getTags().containsKey("water")).toList();

        List<Way> waterWae = waysMap.values().stream().filter(way -> way.getTags().containsKey("water")).toList();

        for (String layer : layersArray) {
            List<Way> selectedRoads = roads.stream()
                    .filter(way -> way.getTags().get("highway").equals(layer) || (!(isRoad(way.getTags().get("highway"))) && layer.equals("road")))
                    .toList();
            if (layer.equals("road")) {
                drawRoads(selectedRoads, giveColor("road"), g);
            } else {
                drawRoads(selectedRoads, giveColor(layer), g);
            }

            List<Relation> selectedRelation = landRelations.stream().filter(relation -> relation.getTags().get("landuse").equals(layer)).toList();
            drawLands(selectedRelation, giveColor(layer), g);

            if (layer.equals("water")){
                drawLands(waterRelations, giveColor(layer), g);
                drawRoads(waterWae, giveColor(layer), g);
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
        BoundingBox boundingBox = new BoundingBox();
        boundingBox.north = tile2lat(y, zoom);
        boundingBox.south = tile2lat(y + 1, zoom);
        boundingBox.west = tile2lon(x, zoom);
        boundingBox.east = tile2lon(x + 1, zoom);
        return boundingBox;
    }

    static double tile2lon(int x, int z) {
        return x / Math.pow(2.0, z) * 360 - 180;
    }

    static double tile2lat(int y, int z) {
        double calc = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(calc)));
    }

    int transLat(double lat) {
        return (int)(((lat - minLat) / (maxLat - minLat)) * tileSize);
    }

    int transLon(double lon) {
        return (int)(((lon - minLon) / (maxLon - minLon)) * tileSize);
    }

    private void drawRoad(List<Node> nodes, Color color, Graphics2D g) {
        if (nodes == null || nodes.isEmpty())
            throw new IllegalArgumentException("List of nodes should never be empty or null!");

        try {

            Node tmpNode = nodes.get(0);
            Coordinate startCoordinate = new Coordinate(tmpNode.getLat(), tmpNode.getLon());

            tmpNode = nodes.get(nodes.size() - 1);
            Coordinate endCoordinate = new Coordinate(tmpNode.getLat(), tmpNode.getLon());

            // Closed polygon
            if (startCoordinate.equals(endCoordinate)) {
                g.setColor(color);
                g.fill(nodelistToPolygon(nodes));
            }

            startCoordinate = null;
            for (Node node : nodes) {
                if (node != null) {
                    Coordinate coordinate = new Coordinate(transLat(node.getLat()), transLon(node.getLon()));
                    if (startCoordinate != null) {
                        g.setColor(color);
                        g.drawLine((int) startCoordinate.x, (int) startCoordinate.y, (int) coordinate.x, (int) coordinate.y);
                    }
                    startCoordinate = coordinate;
                }
            }
        }
        catch (Exception ex) {
            ex.printStackTrace(System.out);
        }

    }

    static double maxLat(List<Node> nodeLat) {
        double maximumLatitude = 0;
        for (Node node : nodeLat) {
            if (node.getLat() > maximumLatitude) {
                maximumLatitude = node.getLat();
            }
        }
        return maximumLatitude;
    }

    static double maxLon(List<Node> nodeLon) {
        double maximumLongitude = 0;
        for (Node node : nodeLon) {
            if (node.getLon() > maximumLongitude) {
                maximumLongitude = node.getLon();
            }
        }
        return maximumLongitude;
    }

    static double minLon(List<Node> nodeLon) {
        double minimumLongitude = 0xB00B5;
        for (Node node : nodeLon) {
            if (node.getLon() < minimumLongitude) {
                minimumLongitude = node.getLon();
            }
        }
        return minimumLongitude;
    }

    static double minLat(List<Node> nodeLat) {
        double minimumLatitude = 0xB00B5;
        for (Node node : nodeLat) {
            if (node.getLat() < minimumLatitude) {
                minimumLatitude = node.getLat();
            }
        }
        return minimumLatitude;
    }

    public Polygon nodelistToPolygon(List<Node> nodeList) {
        int[] x = new int[nodeList.size()];
        int[] y = new int[nodeList.size()];

        for (int i = 0; i < nodeList.size(); i++){
            Node node = nodeList.get(i);
            x[i] = transLon(node.getLon());
            y[i] = transLat(node.getLat());
        }
        return new Polygon(y, x ,nodeList.size());
    }

    private void drawRoads(List<Way> ways, Color color, Graphics2D g) {
        for (Way way : ways) {
            drawRoad(way.getNodes(), color, g);
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

    private void drawLands(List<Relation> relations, Color color, Graphics2D g) {
        for (Relation relation : relations) {
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
            flipped = flipImage(flipped);
        }
        return flipped;
    }

    public BufferedImage flipImage(BufferedImage image) {
        int width = tileSize;
        int height = tileSize;

        BufferedImage flippedImage = new BufferedImage(width, height, image.getType());
        Graphics2D g2 = flippedImage.createGraphics();

        AffineTransform transform = new AffineTransform();
        transform.translate(height, 0);
        transform.rotate(Math.toRadians(90));

        g2.setTransform(transform);
        g2.drawImage(image, 0, 0, null);

        return flippedImage;
    }
}
