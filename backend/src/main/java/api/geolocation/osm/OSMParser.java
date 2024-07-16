package api.geolocation.osm;

import api.geolocation.DataStore;
import api.geolocation.datamodels.Member;
import api.geolocation.datamodels.Relation;
import api.geolocation.datamodels.Way;
import api.geolocation.datamodels.AmenityModel;
import api.geolocation.datamodels.Node;
import api.geolocation.datamodels.RoadModel;
import org.locationtech.jts.geom.GeometryFactory;
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
    private final GeometryFactory geometryFactory;
    private DocumentBuilderFactory documentBuilderFactory;

    private int amenityCount = 0;
    private int roadCount = 0;

    public OSMParser(String filePath) {
        this.filePath = filePath;
        this.dataStore = DataStore.getInstance();
        this.geometryFactory = DataStore.geometryFactory;
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
        try {
            int nodeAmenitiesCount = 0;
            int nodeRoadsCount = 0;

            for (int i = 0; i < count; i++) {
                org.w3c.dom.Node currentNode = nodes.item(i);
                var attributes = currentNode.getAttributes();

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

                dataStore.addNode(id, lat, lon, tags);

                if (tags.containsKey("amenity")) {
                    dataStore.getAmenities().put(id, new AmenityModel(dataStore.getNodes().get(id)));
                    nodeAmenitiesCount++;
                }

                if (tags.containsKey("highway")) {
                    // TODO: nodeRefs
                    // TODO: might be able to remove this
                    dataStore.getRoads().put(id, new RoadModel(dataStore.getNodes().get(id), new ArrayList<>()));
                    nodeRoadsCount++;
                }
            }

            System.out.println("Number of nodes representing amenities: " + nodeAmenitiesCount);
            System.out.println("Number of nodes representing roads:     " + nodeRoadsCount);
            amenityCount += nodeAmenitiesCount;
            roadCount += nodeRoadsCount;
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    private void parseWays(NodeList ways, long count) {
        try {
            int wayAmenitiesCount = 0;
            int wayRoadsCount = 0;

            for (int i = 0; i < count; i++) {
                org.w3c.dom.Node currentNode = ways.item(i);

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

                        newWay.getNodeRefs().add(refId);
                        Node toBeRemoved = dataStore.getNodes().remove(refId);

                        if (toBeRemoved != null) {
                            dataStore.getRoadsNodesMap().put(toBeRemoved.getId(), toBeRemoved);
                        }
                    }

                    if (currentChildNode.getNodeName().equals("tag"))
                    {
                        String key = childNodeAttributes.item(0).getNodeValue();
                        String val = childNodeAttributes.item(1).getNodeValue();
                        newWay.getTags().put(key, val);
                    }
                }

                if (newWay.getTags().containsKey("amenity")) {
                    wayAmenitiesCount++;

                    var geometry = newWay.toGeometry();
                    AmenityModel newAmenity = new AmenityModel(newWay.getId(), geometry, newWay.getTags());
                    dataStore.getAmenities().put(newWay.getId(), newAmenity);
                }

                if (newWay.getTags().containsKey("highway")) {
                    wayRoadsCount++;

                    var geometry = newWay.toGeometry();
                    RoadModel newRoad = new RoadModel(newWay.getId(), geometry, newWay.getTags(), newWay.getNodeRefs());
                    dataStore.getRoads().put(newWay.getId(), newRoad);
                }

                dataStore.getWays().put(newWay.getId(), newWay);
            }

            System.out.println("Number of ways representing amenities: " + wayAmenitiesCount);
            System.out.println("Number of ways representing roads:     " + wayRoadsCount);

            amenityCount += wayAmenitiesCount;
            roadCount += wayRoadsCount;
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
    }

    private void parseRelations(NodeList relations, long count) {
        try {
            int relationAmenityCount = 0;
            int relationRoadsCount = 0;

            for (int i = 0; i < count; i++) {
                org.w3c.dom.Node currentNode = relations.item(i);

                Relation newRelation = new Relation();
                newRelation.setId(Long.parseLong(currentNode.getAttributes().item(0).getNodeValue()));

                NodeList childNodes = currentNode.getChildNodes();
                for (int j = 0; j < childNodes.getLength(); j++) {
                    var currentChildNode = childNodes.item(j);
                    if (currentChildNode.getNodeType() != org.w3c.dom.Node.ELEMENT_NODE)
                        continue;

                    var childNodeAttributes = currentChildNode.getAttributes();

                    // Set members
                    if (currentChildNode.getNodeName().equals("member")) {
                        Member newMember = new Member(geometryFactory);
                        newMember.setRef(Long.parseLong(childNodeAttributes.item(0).getNodeValue()));
                        newMember.setRole(childNodeAttributes.item(1).getNodeValue());
                        newMember.setType(childNodeAttributes.item(2).getNodeValue());

                        newRelation.getMembers().add(newMember);

                        var refId = newMember.getRef();

                        Way toBeRemoved = dataStore.getWays().remove(refId);

                        if (toBeRemoved != null) {
                            dataStore.getWaysRelationMap().put(toBeRemoved.getId(), toBeRemoved);
                        }
                    }

                    // Set tags
                    if (currentChildNode.getNodeName().equals("tag")) {
                        String key = childNodeAttributes.item(0).getNodeValue();
                        String val = childNodeAttributes.item(1).getNodeValue();
                        newRelation.getTags().put(key, val);
                    }
                }

                if (newRelation.getTags().containsKey("amenity")) {
                    try {
                        var geometry = newRelation.toGeometry();
                        AmenityModel newAmenity = new AmenityModel(newRelation.getId(), geometry, newRelation.getTags());
                        dataStore.getAmenities().put(newRelation.getId(), newAmenity);
                        relationAmenityCount++;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }

                if (newRelation.getTags().containsKey("highway")) {
                    try {
                        var geometry = newRelation.toGeometry();
                        // TODO: nodeRefs
                        RoadModel newRoad = new RoadModel(newRelation.getId(), geometry, newRelation.getTags(), new ArrayList<>());
                        dataStore.getRoads().put(newRelation.getId(), newRoad);
                        relationRoadsCount++;
                    }
                    catch (Exception ex) {
                        ex.printStackTrace(System.out);
                    }
                }

                dataStore.getRelations().put(newRelation.getId(), newRelation);
            }

            System.out.println("Number of relations representing amenities: " + relationAmenityCount);
            System.out.println("Number of relations representing roads:     " + relationRoadsCount);

            amenityCount += relationAmenityCount;
            roadCount += relationRoadsCount;
        }
        catch (Exception e) {
            e.printStackTrace(System.out);
            throw e;
        }
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
