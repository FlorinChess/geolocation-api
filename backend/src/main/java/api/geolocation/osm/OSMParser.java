package api.geolocation.osm;

import api.geolocation.DataStore;
import api.geolocation.datamodels.Member;
import api.geolocation.datamodels.Relation;
import api.geolocation.datamodels.Way;
import api.geolocation.datamodels.AmenityModel;
import api.geolocation.datamodels.Node;
import api.geolocation.datamodels.RoadModel;
import org.locationtech.jts.geom.Geometry;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class OSMParser {
    private final String filePath;
    private final DataStore dataStore;
    private DocumentBuilderFactory documentBuilderFactory;

    private int amenityCount = 0;
    private int roadCount = 0;

    public OSMParser(String filePath) {
        this.filePath = filePath;
        this.dataStore = DataStore.getInstance();
        initialize();
    }

    public void parse() {
        try {
            DocumentBuilder builder = documentBuilderFactory.newDocumentBuilder();
            Document doc = builder.parse(new File(filePath));
            doc.getDocumentElement().normalize();

            NodeList nodes = doc.getElementsByTagName("node");
            long nodesLength = nodes.getLength();

            NodeList ways = doc.getElementsByTagName("way");
            long waysLength = ways.getLength();

            NodeList relations = doc.getElementsByTagName("relation");
            long relationsLength = relations.getLength();

            parseNodes(nodes, nodesLength);

            parseWays(ways, waysLength);

            parseRelations(relations, relationsLength);

            System.out.println("Total amenity count: " + amenityCount);
            System.out.println("Total road count:    " + roadCount);
        }
        catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }

    private void parseNodes(NodeList nodes, long count) {
        int nodeAmenitiesCount = 0;
        int nodeRoadsCount = 0;
        int nodeTotalCount = 0;

        for (int i = 0; i < count; i++) {
            org.w3c.dom.Node currentNode = nodes.item(i);
            var attributes = currentNode.getAttributes();

            try {
                long id = Long.parseLong(attributes.item(0).getNodeValue());
                double lat = Double.parseDouble(attributes.item(1).getNodeValue());
                double lon = Double.parseDouble(attributes.item(2).getNodeValue());
                Map<String, String> tags = new HashMap<>();

                var childNodes = currentNode.getChildNodes();

                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                        continue;
                    var childNodeAttributes = currentChildNode.getAttributes();

                    String key = childNodeAttributes.item(0).getNodeValue();
                    String val = childNodeAttributes.item(1).getNodeValue();
                    tags.put(key, val);
                }

                Node newNode = new Node(id, lat, lon, tags);

                if (tags.containsKey("amenity")) {
                    Geometry geometry = newNode.toGeometry();
                    dataStore.getAmenities().put(id, new AmenityModel(id, geometry, tags));
                    nodeAmenitiesCount++;
                }

                // TODO: these could be crossings; not sure if they should be counted as roads
                if (tags.containsKey("highway")) {
                    Geometry geometry = newNode.toGeometry();
                    dataStore.getRoads().put(id, new RoadModel(id, geometry, tags, new ArrayList<>()));
                    nodeRoadsCount++;
                }

                dataStore.getNodes().put(id, newNode);
                nodeTotalCount++;
            }
            catch (NumberFormatException ex) {
                ex.printStackTrace(System.out);
                System.out.println("Node at index " + i + " contains attributes that cannot be converted to their respective numeric representations!");
            }
        }

        System.out.println("Number of nodes representing amenities: " + nodeAmenitiesCount);
        System.out.println("Number of nodes representing roads:     " + nodeRoadsCount);
        System.out.println("Total number of nodes:                  " + nodeTotalCount);
        System.out.println("Finished processing nodes!");
        amenityCount += nodeAmenitiesCount;
        roadCount += nodeRoadsCount;
    }

    private void parseWays(NodeList ways, long count) {
        int wayAmenitiesCount = 0;
        int wayRoadsCount = 0;
        int waysTotalCount = 0;

        for (int i = 0; i < count; i++) {
            boolean valid = true;

            org.w3c.dom.Node currentNode = ways.item(i);

            try {
                Way newWay = new Way();
                newWay.setId(Long.parseLong(currentNode.getAttributes().item(0).getNodeValue()));

                var childNodes = currentNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE) continue;

                    var childNodeAttributes = currentChildNode.getAttributes();

                    if (currentChildNode.getNodeName().equals("nd"))
                    {
                        var refId = Long.parseLong(childNodeAttributes.item(0).getNodeValue());

                        if (dataStore.getNodes().containsKey(refId)) {
                            newWay.getNodes().add(dataStore.getNodes().get(refId));
                        }
                        else {
                            newWay.getMissingNodes().add(refId);
                            valid = false;
                        }
                    }

                    if (currentChildNode.getNodeName().equals("tag"))
                    {
                        String key = childNodeAttributes.item(0).getNodeValue();
                        String val = childNodeAttributes.item(1).getNodeValue();
                        newWay.getTags().put(key, val);
                    }
                }

                if (!valid) {
                    dataStore.getInvalidWays().add(newWay);
                    continue;
                }

                // Set it back to null for garbage collection
                // TODO: improve this; find a way to do this without garbage collection
                newWay.setMissingNodes(null);

                if (newWay.getTags().containsKey("amenity")) {
                    var geometry = newWay.toGeometry();

                    AmenityModel newAmenity = new AmenityModel(newWay.getId(), geometry, newWay.getTags());
                    dataStore.getAmenities().put(newWay.getId(), newAmenity);

                    wayAmenitiesCount++;
                }

                if (newWay.getTags().containsKey("highway")) {
                    var geometry = newWay.toGeometry();

                    RoadModel newRoad = new RoadModel(newWay.getId(), geometry, newWay.getTags(), newWay.getNodeIds());
                    dataStore.getRoads().put(newWay.getId(), newRoad);

                    wayRoadsCount++;
                }

                dataStore.getWays().put(newWay.getId(), newWay);
                waysTotalCount++;
            }
            catch (NumberFormatException ex) {
                System.out.println("Way at index " + i + " contains attributes that cannot be converted to their respective numeric representations!");
                ex.printStackTrace(System.out);
            }
            catch (RuntimeException ex) {
                System.out.println("Way at index  " + i + " does not form a valid Geometry!");
                ex.printStackTrace(System.out);
            }
            catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }

        System.out.println("Number of ways representing amenities: " + wayAmenitiesCount);
        System.out.println("Number of ways representing roads:     " + wayRoadsCount);
        System.out.println("Number of ways with missing nodes:     " + dataStore.getInvalidWays().size());
        System.out.println("Total number of ways:                  " + waysTotalCount);
        System.out.println("Finished processing ways!");

        amenityCount += wayAmenitiesCount;
        roadCount += wayRoadsCount;
    }

    private void parseRelations(NodeList relations, long count) {
        int relationAmenityCount = 0;
        int relationRoadsCount = 0;
        int relationTotalCount = 0;
        int invalidRelationGeometries = 0;

        for (int i = 0; i < count; i++) {
            org.w3c.dom.Node currentNode = relations.item(i);

            boolean valid = true;
            Relation newRelation = new Relation();

            try {
                newRelation.setId(Long.parseLong(currentNode.getAttributes().item(0).getNodeValue()));

                NodeList childNodes = currentNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                        continue;

                    var childNodeAttributes = currentChildNode.getAttributes();

                    // Set members
                    if (currentChildNode.getNodeName().equals("member")) {
                        Member newMember = new Member();
                        newMember.setRef(Long.parseLong(childNodeAttributes.item(0).getNodeValue()));
                        newMember.setRole(childNodeAttributes.item(1).getNodeValue());
                        newMember.setType(childNodeAttributes.item(2).getNodeValue());

                        var refId = newMember.getRef();

                        if (dataStore.getWays().containsKey(refId)) {
                            newRelation.getMembers().add(newMember);
                        }
                        else {
                            newRelation.getMissingWays().add(refId);
                            valid = false;
                        }
                    }

                    // Set tags
                    if (currentChildNode.getNodeName().equals("tag")) {
                        String key = childNodeAttributes.item(0).getNodeValue();
                        String val = childNodeAttributes.item(1).getNodeValue();
                        newRelation.getTags().put(key, val);
                    }
                }

                if (!valid) {
                    relationTotalCount++;
                    dataStore.getInvalidRelations().add(newRelation);
                    continue;
                }

                newRelation.toGeometryExperimental();

//                Geometry geometry = newRelation.toGeometry();
//                if (geometry == null) {
//                    System.out.println("Invalid geometry! id = " + newRelation.getId());
//                    invalidRelationGeometries++;
//                    continue;
//                }
//                if (newRelation.getTags().containsKey("amenity")) {
//                    AmenityModel newAmenity = new AmenityModel(newRelation.getId(), geometry, newRelation.getTags());
//                    dataStore.getAmenities().put(newRelation.getId(), newAmenity);
//                    relationAmenityCount++;
//                }
//
//                if (newRelation.getTags().containsKey("highway")) {
//                    RoadModel newRoad = new RoadModel(newRelation.getId(), geometry, newRelation.getTags(), new ArrayList<>());
//                    dataStore.getRoads().put(newRelation.getId(), newRoad);
//                    relationRoadsCount++;
//                }

                dataStore.getRelations().put(newRelation.getId(), newRelation);
                relationTotalCount++;
            }
            catch (NumberFormatException ex) {
                System.out.println("Relation at index " + i + " contains attributes that cannot be converted to their respective numeric representations!");
                ex.printStackTrace(System.out);
            }
            catch (RuntimeException ex) {
                System.out.println("Invalid relation geometry! id = " + newRelation.getId());
                ex.printStackTrace(System.out);
                invalidRelationGeometries++;
            }
            catch (Exception ex) {
                ex.printStackTrace(System.out);
            }
        }

        System.out.println("Number of relations representing amenities:  " + relationAmenityCount);
        System.out.println("Number of relations representing roads:      " + relationRoadsCount);
        System.out.println("Number of relations with missing references: " + dataStore.getInvalidRelations().size());
        System.out.println("Number of relations with invalid geometries: " + invalidRelationGeometries);
        System.out.println("Total number of relations:                   " + relationTotalCount);
        System.out.println("Finished processing relations!");

        amenityCount += relationAmenityCount;
        roadCount += relationRoadsCount;
    }

    private void initialize() {
        try {
            documentBuilderFactory = DocumentBuilderFactory.newInstance();
            documentBuilderFactory.setNamespaceAware(false);
            documentBuilderFactory.setValidating(false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/namespaces", false);
            documentBuilderFactory.setFeature("http://xml.org/sax/features/validation", false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-dtd-grammar", false);
            documentBuilderFactory.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
        }
        catch (Exception ex) {
            ex.printStackTrace(System.out);
        }
    }
}
