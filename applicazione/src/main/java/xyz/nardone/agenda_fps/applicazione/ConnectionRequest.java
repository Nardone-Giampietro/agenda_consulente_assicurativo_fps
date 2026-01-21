package xyz.nardone.agenda_fps.applicazione;

import com.google.gson.Gson;

import java.io.*;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Simple HTTP client wrapper used by the JavaFX application.
 */
public class ConnectionRequest<T> {
    private final String type;
    private final T body;
    private final String urlBase = "http://localhost:" + ServerPort.get().getPort();
    private final String path;

    public ConnectionRequest(String type, String path, T body) {
        this.type = type;
        this.body = body;
        this.path = path;
    }

    public ConnectionRequest(String type, String path) {
        this(type, path, null);
    }

    /**
     * Sends the request and deserializes the JSON response into the provided type.
     */
    public <R> R send(Type typeOfResponse) throws IOException {
        URL url = new URL(urlBase + path);
        HttpURLConnection con = (HttpURLConnection) url.openConnection();
        con.setRequestMethod(type);

        con.setRequestProperty("Content-Type", "application/json");
        con.setRequestProperty("Accept", "application/json");

        // Handle body for POST/PUT
        if (!"GET".equalsIgnoreCase(type) && body != null) {
            con.setDoOutput(true);
            try (OutputStream os = con.getOutputStream()) {
                Gson gson = new Gson();
                String jsonBody = gson.toJson(body);
                os.write(jsonBody.getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        }

        // Check response code
        int responseCode = con.getResponseCode();
        if (responseCode >= 200 && responseCode < 300) {
            // Read response
            try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                StringBuilder response = new StringBuilder();
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
                Gson gson = new Gson();
                return gson.fromJson(response.toString(), typeOfResponse);
            }
        } else {
            throw new IOException("HTTP error: " + responseCode + " - " + con.getResponseMessage());
        }
    }
}
