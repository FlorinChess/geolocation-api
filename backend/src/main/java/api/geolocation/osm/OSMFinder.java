package api.geolocation.osm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class OSMFinder {
    private static final String OVERPASS_API_URL = "http://overpass-api.de/api/interpreter?data=[out:json];node(%d);out;";

    public static JsonNode fetchNode(long nodeId) throws IOException {
        String url = String.format(OVERPASS_API_URL, nodeId);

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(url);
            HttpResponse response = httpClient.execute(request);
            HttpEntity entity = response.getEntity();

            if (entity != null) {
                ObjectMapper mapper = new ObjectMapper();
                return mapper.readTree(entity.getContent());
            }
        }
        return null;
    }
}
