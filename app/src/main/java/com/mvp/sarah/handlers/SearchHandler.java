package com.mvp.sarah.handlers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import com.mvp.sarah.BuildConfig;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import android.util.Base64;
import org.json.JSONArray;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.io.UnsupportedEncodingException;
import org.json.JSONException;

public class SearchHandler implements CommandHandler, CommandRegistry.SuggestionProvider {

    public static final String ACTION_SHOW_STOP_BUTTON = "com.mvp.sarah.ACTION_SHOW_STOP_BUTTON";
    public static final String ACTION_HIDE_STOP_BUTTON = "com.mvp.sarah.ACTION_HIDE_STOP_BUTTON";
    public static final String ACTION_STOP_SEARCH = "com.mvp.sarah.ACTION_STOP_SEARCH";

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile Future<?> searchTask;
    private volatile HttpURLConnection connection;
    private String apiKey = null;
    private boolean isFetchingKey = false;
    private final Object keyLock = new Object();

    private void fetchApiKey(Runnable onReady) {
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
        return command.toLowerCase(Locale.ROOT).startsWith("search ");
    }

    @Override
    public void handle(Context context, String command) {
        if (searchTask != null && !searchTask.isDone()) {
            FeedbackProvider.speakAndToast(context, "A search is already in progress.");
            return;
        }

        String query = command.substring(7).trim();
        FeedbackProvider.speakAndToast(context, "Searching for: " + query);

        // Show the stop button
        context.sendBroadcast(new Intent(ACTION_SHOW_STOP_BUTTON));
        
        // Register a receiver to handle the stop action
        BroadcastReceiver stopSearchReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (searchTask != null && !searchTask.isDone()) {
                    searchTask.cancel(true);
                    FeedbackProvider.speakAndToast(context, "Search cancelled.");
                }
            }
        };
        context.registerReceiver(stopSearchReceiver, new IntentFilter(ACTION_STOP_SEARCH), Context.RECEIVER_EXPORTED);

        searchTask = executor.submit(() -> {
            try {
                fetchApiKey(() -> {
                    String finalApiKey = apiKey;
                    if (finalApiKey == null) {
                        FeedbackProvider.speakAndToast(context, "API key not available. Please check app configuration.");
                        return;
                    }
                    try {
                        URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + finalApiKey);
                        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                        connection.setRequestMethod("POST");
                        connection.setRequestProperty("Content-Type", "application/json");
                        connection.setDoOutput(true);
                        String jsonInputString = "{\"contents\":[{\"parts\":[{\"text\":\"" + query.replace("\"", "\\\"") + "\"}]}]}";
                        try (OutputStream os = connection.getOutputStream()) {
                            os.write(jsonInputString.getBytes("utf-8"));
                        }
                        int responseCode = connection.getResponseCode();
                        if (responseCode == HttpURLConnection.HTTP_OK) {
                            try (BufferedReader br = new BufferedReader(new InputStreamReader(connection.getInputStream(), "utf-8"))) {
                                StringBuilder response = new StringBuilder();
                                String responseLine;
                                while ((responseLine = br.readLine()) != null) {
                                    response.append(responseLine.trim());
                                }
                                JSONObject jsonResponse = new JSONObject(response.toString());
                                JSONArray parts = jsonResponse.getJSONArray("candidates")
                                        .getJSONObject(0).getJSONObject("content")
                                        .getJSONArray("parts");
                                String resultText = null;
                                for (int i = 0; i < parts.length(); i++) {
                                    JSONObject part = parts.getJSONObject(i);
                                    if (part.has("text")) {
                                        resultText = part.getString("text");
                                        break;
                                    }
                                }
                                if (resultText != null) {
                                    FeedbackProvider.speakAndToast(context, resultText);
                                } else {
                                    FeedbackProvider.speakAndToast(context, "Sorry, I couldn't get a result for that search.");
                                }
                            }
                        } else {
                            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't get a result for that search.");
                        }
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                        FeedbackProvider.speakAndToast(context, "Invalid URL for Gemini API.");
                    } catch (IOException e) {
                        e.printStackTrace();
                        FeedbackProvider.speakAndToast(context, "Network error while contacting Gemini API.");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        FeedbackProvider.speakAndToast(context, "Error parsing Gemini API response.");
                    } catch (Exception e) {
                        e.printStackTrace();
                        FeedbackProvider.speakAndToast(context, "Unexpected error with Gemini API.");
                    }
                });
            } catch (Exception e) {
                if (Thread.interrupted()) {
                    android.util.Log.w("SearchHandler", "Search thread interrupted.");
                } else {
                    android.util.Log.e("SearchHandler", "Error during Gemini API call", e);
                    FeedbackProvider.speakAndToast(context, "Sorry, I couldn't get a result for that search.");
                }
            } finally {
                // Hide the stop button and unregister the receiver
                context.sendBroadcast(new Intent(ACTION_HIDE_STOP_BUTTON));
                context.unregisterReceiver(stopSearchReceiver);
            }
        });
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "search for the capital of France",
            "search who invented the light bulb"
        );
    }
} 