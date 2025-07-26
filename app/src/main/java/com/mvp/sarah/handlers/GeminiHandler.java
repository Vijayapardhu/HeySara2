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
import com.google.firebase.firestore.FirebaseFirestore;

public class GeminiHandler implements CommandHandler {

    private static final String TAG = "GeminiHandler";
    private String apiKey = null;
    private boolean isFetchingKey = false;
    private final Object keyLock = new Object();

    private void fetchApiKey(Context context, Runnable onReady) {
        synchronized (keyLock) {
            if (apiKey != null) {
                onReady.run();
                return;
            }
            if (isFetchingKey) return;
            isFetchingKey = true;
        }
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("app_config").document("gemini").get()
            .addOnSuccessListener(document -> {
                if (document.exists() && document.contains("api_key")) {
                    apiKey = document.getString("api_key");
                }
                synchronized (keyLock) { isFetchingKey = false; }
                onReady.run();
            })
            .addOnFailureListener(e -> {
                synchronized (keyLock) { isFetchingKey = false; }
                onReady.run();
            });
    }

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase().trim();
        // Match general questions, but not calculations
        boolean isGeneral = lower.startsWith("ask") || lower.startsWith("what is") || lower.startsWith("who is") || lower.startsWith("explain");
        boolean isCalculation = lower.matches(".*\\d.*[+\\-*/].*\\d.*");
        return isGeneral && !isCalculation;
    }

    @Override
    public void handle(Context context, String command) {
        fetchApiKey(context, () -> {
            if (apiKey == null) {
                FeedbackProvider.speakAndToast(context, "Gemini API key not available.");
                return;
            }
        // Extract the actual query from the command
        String query = command;
        String lowercasedCommand = command.toLowerCase();
        if (lowercasedCommand.startsWith("ask")) {
            query = command.substring(3).trim();
        }
        final String finalQuery = query;
        new Thread(() -> {
            try {
                    String apiUrl = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key=" + apiKey;
                    URL url = new URL(apiUrl);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);
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
                        FeedbackProvider.speakAndToast(context, "Sorry, I couldn't get a response from the AI.");
                }
                conn.disconnect();
            } catch (Exception e) {
                Log.e(TAG, "Error calling Gemini API", e);
                FeedbackProvider.speakAndToast(context, "Sorry, there was an error communicating with the AI.");
            }
        }).start();
        });
    }
}