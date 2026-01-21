package xyz.nardone.agenda_fps.applicazione;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Utility for discovering the service port by probing the /port endpoint.
 */
public class DynamicPortQuery {

    private static final Logger logger = LogManager.getLogger(DynamicPortQuery.class);

    /**
     * Scans a port range and returns the first service that responds.
     */
    public static int findServerPort(int startPort, int endPort) {
        for (int port = startPort; port <= endPort; port++) {
            try {
                return queryPort(port); // Query each port in the range
            } catch (Exception ignored) {
                // Continue if the port is not accessible
            }
        }
        throw new RuntimeException("Server is not running on any port in the range " + startPort + " to " + endPort);
    }

    /**
     * Queries the /port endpoint on a specific port.
     */
    public static int queryPort(int port) throws Exception {
        String endpoint = "http://localhost:" + port + "/port";
        URL url = new URL(endpoint);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        int responseCode = connection.getResponseCode();
        if (responseCode == 200) {
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                String response = reader.readLine();
                return Integer.parseInt(response);
            }
        } else {
            throw new RuntimeException("Failed to query port. Response code: " + responseCode);
        }
    }
}
