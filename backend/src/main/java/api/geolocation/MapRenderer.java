package api.geolocation;

import api.geolocation.datamodels.Node;
import api.geolocation.datamodels.Relation;
import api.geolocation.datamodels.Way;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;

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
    private final List<String> predefinedDrawingOrder =
            Arrays.asList(
                "residential", "garages", "commercial", "industrial", "education", "vineyard", "grass", "meadow", "flowerbed",
                "village_green", "recreation_ground", "cemetery", "garden", "park", "greenfield", "pitch", "stadium", "sports_centre",
                "track", "playground", "forest", "wood", "farmland", "farmyard", "water", "motorway", "trunk", "road", "secondary", "primary",
                "railway", "building");

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

        List<String> layersArray = new ArrayList<>(List.of(layers.split(",")));

        // Create a map for faster lookup of the predefined order
        Map<String, Integer> orderMap = new HashMap<>();
        for (int i = 0; i < predefinedDrawingOrder.size(); i++) {
            orderMap.put(predefinedDrawingOrder.get(i), i);
        }

        // Sort the list using a custom comparator based on the predefined order
        layersArray.sort((a, b) -> {
            Integer indexA = orderMap.getOrDefault(a, Integer.MAX_VALUE); // Default to max if not found
            Integer indexB = orderMap.getOrDefault(b, Integer.MAX_VALUE);
            return indexA.compareTo(indexB);
        });

        List<Way> roads = waysMap.values().stream().parallel()
                .filter(way -> way.getTags().containsKey("highway")).toList();

        List<Relation> landuseRelations = relationsMap.values().stream().parallel()
                .filter(relation -> relation.getTags().containsKey("landuse")).toList();

        List<Way> landuseWays = waysMap.values().stream().parallel()
                .filter(way -> way.getTags().containsKey("landuse")).toList();

        for (String layer : layersArray) {
            switch (layer) {
                case "water" -> {
                    List<Relation> waterRelations = relationsMap.values().stream().parallel()
                            .filter(relation -> relation.getTags().containsKey("water")).toList();

                    List<Way> waterWays = waysMap.values().stream().parallel()
                            .filter(way -> way.getTags().containsKey("water")).toList();
                    drawLands(waterRelations, giveColor(layer), g);
                    drawRoads(waterWays, giveColor(layer), g);
                }
                case "building" -> {
                    List<Relation> buildingRelations = relationsMap.values().stream().parallel()
                            .filter(relation -> relation.getTags().containsKey("building")).toList();
                    List<Way> buildingWays = waysMap.values().stream().parallel()
                            .filter(way -> way.getTags().containsKey("building")).toList();
                    drawLands(buildingRelations, giveColor(layer), g);
                    drawRoads(buildingWays, giveColor(layer), g);
                }
                case "residential", "garages", "education", "industrial", "cemetery", "commercial", "forest", "greenfield",
                     "grass", "meadow", "flowerbed", "vineyard", "farmland", "farmyard", "village_green", "recreation_ground" -> {
                    List<Way> selectedWays = landuseWays.stream().parallel()
                            .filter(way -> way.getTags().get("landuse").equals(layer)).toList();
                    List<Relation> selectedRelations = landuseRelations.stream().parallel()
                            .filter(relation -> relation.getTags().get("landuse").equals(layer)).toList();
                    drawLands(selectedRelations, giveColor(layer), g);
                    drawRoads(selectedWays, giveColor(layer), g);
                }
                case "railway" -> {
                    List<Way> railways = waysMap.values().stream()
                            .filter(way -> way.getTags().containsKey("railway")).toList();
                    drawRoads(railways, giveColor("railway"), g);
                }
                case "park", "garden", "pitch", "stadium", "sports_centre", "track", "playground" -> {
                    List<Way> parks = waysMap.values().stream().parallel()
                            .filter(way -> way.getTags().containsKey("leisure") && way.getTags().get("leisure").equals(layer)).toList();
                    drawRoads(parks, giveColor(layer), g);
                }
                case "wood" -> {
                    List<Way> woods = waysMap.values().stream().parallel()
                            .filter(way -> way.getTags().containsKey("natural") && way.getTags().get("natural").equals(layer)).toList();
                    drawRoads(woods, giveColor(layer), g);
                }
                default -> {
                    // TODO: Simplify this maybe
                    List<Way> selectedRoads = roads.stream().parallel()
                            .filter(way -> way.getTags().get("highway").equals(layer) ||
                                    (!(isRoad(way.getTags().get("highway"))) && layer.equals("road"))).toList();
                    drawRoads(selectedRoads, giveColor(layer), g);
                }
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
            case "motorway" -> color = new Color(255, 0, 0);
            case "trunk" -> color = new Color(255, 140, 0);
            case "primary" -> color = new Color(255, 165, 0);
            case "secondary" -> color = new Color(255, 255, 0);
            case "road" -> color = new Color(128, 128, 128);
            case "forest", "wood" -> color = new Color(173, 209, 158);
            case "residential", "garages", "commercial", "industrial" -> color = new Color(223, 233, 233);
            case "vineyard" -> color = new Color(172, 224, 161);
            case "grass", "meadow", "flowerbed", "garden", "park", "greenfield", "village_green", "recreation_ground", "playground" -> color = new Color(205, 235, 176);
            case "pitch", "stadium", "sports_centre", "track" -> color = new Color(150, 227, 196);
            case "farmland", "farmyard" -> color = new Color(250, 231, 147);
            case "cemetery" -> color = new Color(182, 201, 167);
            case "railway" -> color = new Color(235, 219, 233);
            case "water" -> color = new Color(0, 128, 255);
            case "building" -> color = new Color(189, 146, 123);
            case "education" -> color = new Color(255, 236, 184);
            default -> {
            }
        }
        return color;
    }

    public boolean isRoad(String type) {
        return switch (type) {
            case "motorway", "trunk", "primary", "secondary", "water" -> true;
            default -> false;
        };
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
                // Seems to help with performance, but some buildings don't show up
                return;
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

    private void drawLand(List<LinearRing> innerLinearRings, List<LinearRing> outerLinearRings, Color color, Graphics2D g) {
        Area area = new Area();
        outerLinearRings.forEach(linearRing -> area.add(new Area(convertToPolygon(linearRing))));
        innerLinearRings.forEach(linearRing -> area.subtract(new Area(convertToPolygon(linearRing))));

        g.setColor(color);
        g.fill(area);
    }

    private void drawLands(List<Relation> relations, Color color, Graphics2D g) {
        for (Relation relation : relations) {
            drawLand(relation.getInnerLinearRings(), relation.getOuterLinearRings(), color, g);
        }
    }

    private Polygon convertToPolygon(LinearRing jtsLinearRing) {
        int[] x = new int[jtsLinearRing.getNumPoints()];
        int[] y = new int[jtsLinearRing.getNumPoints()];

        for (int i = 0; i < jtsLinearRing.getNumPoints(); i++){
            x[i] = transLon(jtsLinearRing.getCoordinates()[i].x);
            y[i] = transLat(jtsLinearRing.getCoordinates()[i].y);
        }
        return new Polygon(y, x, jtsLinearRing.getNumPoints());
    }

    public BufferedImage flipImageCounterClockwise(BufferedImage flipped) {
        for (int i = 0; i < 3; i++){
            flipped = flipImage(flipped);
        }
        return flipped;
    }

    public BufferedImage flipImage(BufferedImage image) {
        BufferedImage flippedImage = new BufferedImage(tileSize, tileSize, image.getType());
        Graphics2D g2 = flippedImage.createGraphics();

        AffineTransform transform = new AffineTransform();
        transform.translate(tileSize, 0);
        transform.rotate(Math.toRadians(90));

        g2.setTransform(transform);
        g2.drawImage(image, 0, 0, null);

        return flippedImage;
    }
}
