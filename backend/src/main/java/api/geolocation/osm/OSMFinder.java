package api.geolocation.osm;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;

public class OSMFinder {
    private static final String OVERPASS_API_NODE_URL = "http://overpass-api.de/api/interpreter?data=[out:json];node(%d);out;";
    private static final String OVERPASS_API_WAY_URL = "http://overpass-api.de/api/interpreter?data=[out:json];way(%d);out;";

    // Timeout settings in milliseconds
    private static final int CONNECTION_TIMEOUT_MS = 5000; // 5 seconds
    private static final int SOCKET_TIMEOUT_MS = 10000;    // 10 seconds
    private static final int CONNECTION_REQUEST_TIMEOUT_MS = 3000; // 3 seconds

    public static JsonNode fetchNode(long nodeId) throws IOException {
        String url = String.format(OVERPASS_API_NODE_URL, nodeId);

        // Set the request configuration for timeouts
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(CONNECTION_TIMEOUT_MS)
                .setSocketTimeout(SOCKET_TIMEOUT_MS)
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT_MS)
                .build();

        try (CloseableHttpClient httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .build()) {
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

    public static JsonNode fetchWay(long wayId) throws IOException {
        String url = String.format(OVERPASS_API_WAY_URL, wayId);

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
