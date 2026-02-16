package api.geolocation;

import api.geolocation.datamodels.Node;
import api.geolocation.datamodels.Relation;
import api.geolocation.datamodels.Way;
import org.apache.commons.lang3.tuple.Pair;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.LinearRing;

import javax.imageio.ImageIO;
import java.awt.*;
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
    private double maxLatitude;
    private double minLon;
    private double minLatitude;

    private final int tileSize = 512;
    private final DataStore dataStore = DataStore.getInstance();
    private final List<String> predefinedDrawingOrder =
            Arrays.asList(
                "residential", "garages", "commercial", "industrial", "education", "vineyard", "grass", "meadow", "flowerbed",
                "village_green", "recreation_ground", "cemetery", "garden", "park", "greenfield", "pitch", "stadium", "sports_centre",
                "track", "playground", "forest", "wood", "farmland", "farmyard", "water", "motorway", "trunk", "road", "secondary", "primary",
                "railway", "building");

    private final Map<String, Pair<Color, Integer>> colorsMap = new HashMap<>();

    public MapRenderer() {
        initializeColorsMap();
    }

    private void initializeColorsMap() {
        colorsMap.put("motorway", Pair.of(Color.RED, 3));
        colorsMap.put("trunk", Pair.of(new Color(255, 140, 0), 2));
        colorsMap.put("primary", Pair.of(new Color(255, 165, 0), 2));
        colorsMap.put("secondary", Pair.of(new Color(255, 255, 0), 2));
        colorsMap.put("road", Pair.of(new Color(180, 180, 180), 2));

        Color forest_wood = new Color(173, 209, 158);
        colorsMap.put("forest", Pair.of(forest_wood, 1));
        colorsMap.put("wood", Pair.of(forest_wood, 1));

        Color building_usage = new Color(223, 233, 233);
        colorsMap.put("garages", Pair.of(building_usage, 1));
        colorsMap.put("commercial", Pair.of(building_usage, 1));
        colorsMap.put("industrial", Pair.of(building_usage, 1));
        colorsMap.put("residential", Pair.of(building_usage, 1));

        colorsMap.put("vineyard", Pair.of(new Color(172,224, 161), 1));

        Color grass = new Color(205, 235, 176);
        colorsMap.put("grass", Pair.of(grass, 1));
        colorsMap.put("meadow", Pair.of(grass, 1));
        colorsMap.put("flowerbed", Pair.of(grass, 1));
        colorsMap.put("garden", Pair.of(grass, 1));
        colorsMap.put("park", Pair.of(grass, 1));
        colorsMap.put("greenfield", Pair.of(grass, 1));
        colorsMap.put("village_green", Pair.of(grass, 1));
        colorsMap.put("recreation_ground", Pair.of(grass, 1));
        colorsMap.put("playground", Pair.of(grass, 1));

        Color sportsFields = new Color(150, 227, 196);
        colorsMap.put("pitch", Pair.of(sportsFields, 1));
        colorsMap.put("stadium", Pair.of(sportsFields, 1));
        colorsMap.put("sports_centre", Pair.of(sportsFields, 1));
        colorsMap.put("track", Pair.of(sportsFields, 1));

        Color farmland = new Color(250, 231, 147);
        colorsMap.put("farmland", Pair.of(farmland, 1));
        colorsMap.put("farmyard", Pair.of(farmland, 1));

        colorsMap.put("cemetery", Pair.of(new Color(182, 201, 167), 1));
        colorsMap.put("railway", Pair.of(new Color(80, 80, 80), 1));
        colorsMap.put("water", Pair.of(new Color(0, 128, 255), 1));
        colorsMap.put("building", Pair.of(new Color(189, 146, 123), 1));
        colorsMap.put("education", Pair.of(new Color(255, 236, 184), 1));
    }

    public ByteArrayOutputStream renderTile(int zoom, int x, int y, String layers) throws IOException {
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

        // create drawing bounding box based on x,y (tile coordinates, not real coordinates) and zoom level
        BoundingBox bbox = tileToBoundingBox(x, y, zoom);

        maxLatitude = bbox.north;
        maxLon = bbox.east;
        minLon = bbox.west;
        minLatitude = bbox.south;

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
            case "road" -> color = new Color(180, 180, 180);
            case "forest", "wood" -> color = new Color(173, 209, 158);
            case "residential", "garages", "commercial", "industrial" -> color = new Color(223, 233, 233);
            case "vineyard" -> color = new Color(172, 224, 161);
            case "grass", "meadow", "flowerbed", "garden", "park", "greenfield", "village_green", "recreation_ground", "playground" -> color = new Color(205, 235, 176);
            case "pitch", "stadium", "sports_centre", "track" -> color = new Color(150, 227, 196);
            case "farmland", "farmyard" -> color = new Color(250, 231, 147);
            case "cemetery" -> color = new Color(182, 201, 167);
            case "railway" -> color = new Color(80, 80, 80);
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

    private BoundingBox tileToBoundingBox(int x, int y, int zoom) {
        BoundingBox boundingBox = new BoundingBox();
        boundingBox.north = tileToLatitude(y, zoom);
        boundingBox.south = tileToLatitude(y + 1, zoom);
        boundingBox.west = tileToLongitude(x, zoom);
        boundingBox.east = tileToLongitude(x + 1, zoom);
        return boundingBox;
    }

    static double tileToLongitude(int x, int z) {
        return x / Math.pow(2.0, z) * 360 - 180;
    }

    static double tileToLatitude(int y, int z) {
        double calc = Math.PI - (2.0 * Math.PI * y) / Math.pow(2.0, z);
        return Math.toDegrees(Math.atan(Math.sinh(calc)));
    }

    /**
     * This function converts real-world latitude values to screen Y coordinate.
     * NOTE: Since screen Y coordinates grow downwards, the conversion is inverted.
     * @param latitude Real-world latitude value.
     * @return Screen Y coordinate.
     */
    int translateLatitude(double latitude) {
        return (int)(((maxLatitude - latitude) / (maxLatitude - minLatitude)) * tileSize);
    }

    /**
     * This function converts real-world longitude values to screen Y coordinate.
     * @param longitude Real-world longitude value.
     * @return Screen X coordinate
     */
    int translateLongitude(double longitude) {
        return (int)(((longitude - minLon) / (maxLon - minLon)) * tileSize);
    }

    private void drawRoad(List<Node> nodes, Color color, Graphics2D g) {
        if (nodes == null || nodes.isEmpty())
            throw new IllegalArgumentException("List of nodes should never be empty or null!");

        try {
            // first node
            Node tmpNode = nodes.get(0);
            Coordinate startCoordinate = new Coordinate(tmpNode.getLon(), tmpNode.getLat());

            // last node
            tmpNode = nodes.get(nodes.size() - 1);
            Coordinate endCoordinate = new Coordinate(tmpNode.getLon(), tmpNode.getLat());

            // Closed polygon
            if (startCoordinate.equals(endCoordinate)) {
                g.setColor(color);
                g.fill(nodelistToPolygon(nodes));
                // Seems to help with performance, but some buildings don't show up
                return;
            }

            // reset
            startCoordinate = null;
            for (Node node : nodes) {
                if (node != null) {
                    Coordinate coordinate = new Coordinate(translateLongitude(node.getLon()), translateLatitude(node.getLat()));
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
            x[i] = translateLongitude(node.getLon());
            y[i] = translateLatitude(node.getLat());
        }
        return new Polygon(x, y ,nodeList.size());
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
            x[i] = translateLongitude(jtsLinearRing.getCoordinates()[i].x);
            y[i] = translateLatitude(jtsLinearRing.getCoordinates()[i].y);
        }
        return new Polygon(x, y, jtsLinearRing.getNumPoints());
    }
}
