package com.mvp.sarah;

import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImagenHelper {
    public static JSONArray generateImages(String prompt, int sampleCount, String apiKey) throws Exception {
        JSONObject requestBody = new JSONObject();
        JSONArray instances = new JSONArray();
        JSONObject instance = new JSONObject();
        instance.put("prompt", prompt);
        instances.put(instance);
        requestBody.put("instances", instances);

        JSONObject parameters = new JSONObject();
        parameters.put("sampleCount", sampleCount);
        requestBody.put("parameters", parameters);

        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/imagen-4.0-generate-preview-06-06:predict?key=" + apiKey);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setDoOutput(true);

        try (OutputStream os = connection.getOutputStream()) {
            os.write(requestBody.toString().getBytes("utf-8"));
        }

        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
            String responseLine;
            while ((responseLine = br.readLine()) != null) {
                response.append(responseLine.trim());
            }
        }

        JSONObject jsonResponse = new JSONObject(response.toString());
        return jsonResponse.getJSONArray("predictions");
    }
} 