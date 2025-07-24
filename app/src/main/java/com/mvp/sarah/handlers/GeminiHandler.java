package com.mvp.sarah.handlers;

import android.content.Context;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class GeminiHandler implements CommandHandler {

    private static final String TAG = "GeminiHandler";
    // IMPORTANT: Replace "YOUR_API_KEY" with your actual Gemini API key
    private static final String API_KEY = "AIzaSyAH4ljHr5OTLWBDIOSPW1xzNzpxNN3L7kA";
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + API_KEY;

    @Override
    public boolean canHandle(String command) {
        String lowercasedCommand = command.toLowerCase();
        return lowercasedCommand.startsWith("ask") ||
               lowercasedCommand.startsWith("what is") ||
               lowercasedCommand.startsWith("who is") ||
               lowercasedCommand.startsWith("explain");
    }

    @Override
    public void handle(Context context, String command) {

        // Extract the actual query from the command
        String query = command;
        String lowercasedCommand = command.toLowerCase();
        if (lowercasedCommand.startsWith("ask")) {
            query = command.substring(3).trim();
        }

        final String finalQuery = query;
        new Thread(() -> {
            try {
                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);

                // Create the JSON request body
                String jsonInputString = "{\"contents\":[{\"parts\":[{\"text\":\"" + JSONObject.quote(finalQuery) + "\"}]}]}";

                try (OutputStream os = conn.getOutputStream()) {
                    byte[] input = jsonInputString.getBytes(StandardCharsets.UTF_8);
                    os.write(input, 0, input.length);
                }

                int responseCode = conn.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), StandardCharsets.UTF_8))) {
                        StringBuilder response = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            response.append(responseLine.trim());
                        }
                        
                        // Parse the JSON response
                        JSONObject jsonResponse = new JSONObject(response.toString());
                        String text = jsonResponse.getJSONArray("candidates")
                                                  .getJSONObject(0)
                                                  .getJSONObject("content")
                                                  .getJSONArray("parts")
                                                  .getJSONObject(0)
                                                  .getString("text");

                        FeedbackProvider.speakAndToast(context, text);
                    }
                } else {
                    String errorMessage = "Error from API: " + responseCode + " " + conn.getResponseMessage();
                    Log.e(TAG, errorMessage);
                    try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getErrorStream(), StandardCharsets.UTF_8))) {
                        StringBuilder errorResponse = new StringBuilder();
                        String responseLine;
                        while ((responseLine = br.readLine()) != null) {
                            errorResponse.append(responseLine.trim());
                        }
                        Log.e(TAG, "Error details: " + errorResponse.toString());
                    }
                    FeedbackProvider.speakAndToast(context, "Sorry, I couldn\'t get a response from the AI.");
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API", e);
                FeedbackProvider.speakAndToast(context, "Sorry, there was an error communicating with the AI.");
            }
        }).start();
    }
}