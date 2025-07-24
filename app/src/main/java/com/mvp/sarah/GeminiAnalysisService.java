package com.mvp.sarah;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Binder;
import android.os.IBinder;
import android.util.Log;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import android.util.Base64;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOException;

public class GeminiAnalysisService extends Service {

    private static final String TAG = "GeminiAnalysisService";
    private static final String API_KEY = "AIzaSyAH4ljHr5OTLWBDIOSPW1xzNzpxNN3L7kA"; // Replace with your API key
    private static final String API_URL = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro-vision:generateContent?key=" + API_KEY;
    private final IBinder binder = new LocalBinder();

    public class LocalBinder extends Binder {
        public GeminiAnalysisService getService() {
            return GeminiAnalysisService.this;
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public void analyzeImage(Bitmap image, AnalysisCallback callback) {
        new Thread(() -> {
            try {
                ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                image.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream);
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                String base64Image = Base64.encodeToString(byteArray, Base64.DEFAULT);

                URL url = new URL(API_URL);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json; charset=utf-8");
                conn.setDoOutput(true);

                String prompt = "Analyze the screen for a question and its options. Also, look for a submit button. " +
                              "Return a JSON object with the following structure: " +
                              "{\"action\": \"click\" or \"type\", \"x\": X_COORDINATE, \"y\": Y_COORDINATE, \"text\": \"ANSWER_TEXT\", \"submit_button\": {\"found\": true/false, \"x\": SUBMIT_X, \"y\": SUBMIT_Y}}. " +
                              "If no question is found, return null for the answer fields. If no submit button is found, set found to false.";

                String jsonInputString = "{\"contents\":[{\"parts\":[{\"text\":\"" + JSONObject.quote(prompt) + "\"},{\"inline_data\":{\"mime_type\":\"image/jpeg\",\"data\":\"" + base64Image + "\"}}]}]}";

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
                            response.append(responseLine);
                        }
                        // Extract the JSON string from the response
                        String jsonStr = response.toString();
                        // A simple way to extract JSON from a string that might contain other text
                        int startIndex = jsonStr.indexOf('{');
                        int endIndex = jsonStr.lastIndexOf('}');
                        if (startIndex != -1 && endIndex != -1) {
                            String jsonResult = jsonStr.substring(startIndex, endIndex + 1);
                            callback.onAnalysisComplete(jsonResult);
                        } else {
                             callback.onAnalysisComplete(null); // Or handle error
                        }
                    }
                } else {
                    Log.e(TAG, "Error from API: " + responseCode);
                    callback.onAnalysisComplete(null);
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Error analyzing image", e);
            }
        }).start();
    }

    public interface AnalysisCallback {
        void onAnalysisComplete(String result);
    }
}