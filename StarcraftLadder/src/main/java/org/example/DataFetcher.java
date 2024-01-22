package org.example;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.StringJoiner;
import java.io.IOException;
import java.io.PrintWriter;
import com.fasterxml.jackson.databind.ObjectMapper;


public class DataFetcher {
    private Properties prop = new Properties();
    private String oAuthToken;
    DataFetcher(String cfgPath, String region, int regionID) throws Exception{
        prop = fetchConfig(cfgPath);
        oAuthToken =  fetchOAuthToken(prop);
        fetchLadderData(region, regionID);
    }


    public Properties fetchConfig(String fileName) throws Exception{
        try (InputStream input = new FileInputStream(fileName)) {
            this.prop.load(input);
            return prop;
        }
    }


    public static String fetchOAuthToken(Properties config) throws Exception {
        String tokenUrl = "https://oauth.battle.net/token";
        HttpClient client = HttpClient.newHttpClient();

        String clientId = config.getProperty("API.client_id");
        String clientSecret = config.getProperty("API.client_secret");

        // Prepare the request body
        Map<Object, Object> data = new HashMap<>();
        data.put("client_id", clientId);
        data.put("client_secret", clientSecret);
        data.put("grant_type", "client_credentials");

        StringJoiner requestBody = new StringJoiner("&");
        for (Map.Entry<Object, Object> entry : data.entrySet()) {
            requestBody.add(URLEncoder.encode(entry.getKey().toString(), StandardCharsets.UTF_8) + "="
                    + URLEncoder.encode(entry.getValue().toString(), StandardCharsets.UTF_8));
        }

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(tokenUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody.toString()))
                .build();

        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        // Parse the JSON response
        ObjectMapper mapper = new ObjectMapper();
        Map<String, String> responseMap = mapper.readValue(response.body(), Map.class);

        return responseMap.get("access_token");
    }


    public void fetchLadderData(String region, int regionID) throws IOException, InterruptedException {
        String apiUrl = String.format("https://%s.api.blizzard.com/sc2/ladder/grandmaster/%d", region, regionID);
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiUrl))
                .header("Authorization", "Bearer " + oAuthToken)
                .GET()
                .build();

        HttpClient client = HttpClient.newHttpClient();
        HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            System.out.println("Failed to fetch data. Status Code: " + response.statusCode());
            System.out.println("Response: " + response.body());
            System.exit(1);
        }

        ObjectMapper mapper = new ObjectMapper();
        Object json = mapper.readValue(response.body(), Object.class);
        try (PrintWriter out = new PrintWriter("grandmaster_leaderboard.json", StandardCharsets.UTF_8)) {
            out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(json));
            System.out.println("Saved leaderboard data to grandmaster_leaderboard.json");
        }
        catch (IOException e) {
            System.out.println("Request Exception: " + e);
            System.exit(1);
        } catch (Exception e) {
            System.out.println("General Exception: " + e);
            System.exit(1);
        }
        }


    public String getOAuthToken(){
        return oAuthToken;
    }


    public Properties getConfig(){
        return prop;
    }
}
