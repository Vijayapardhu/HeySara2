// WeatherHandler.java
package com.mvp.sarah.handlers;

import android.content.Context;
import android.os.AsyncTask;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class WeatherHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String API_KEY = "e98c61523bec402aad9152317251306";
    private static final List<String> COMMANDS = Arrays.asList(
            "what's the weather",
            "weather in",
            "current weather",
            "weather forecast"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("weather");
    }

    @Override
    public void handle(Context context, String command) {
        String city = extractCity(command);
        if (city == null || city.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify a city, for example: 'weather in London'.");
            return;
        }
        new FetchWeatherTask(context, city).execute();
    }

    private String extractCity(String command) {
        command = command.toLowerCase();
        if (command.contains("weather in ")) {
            return command.substring(command.indexOf("weather in ") + 11).trim();
        } else if (command.startsWith("weather ")) {
            return command.substring(8).trim();
        } else if (command.contains("in ")) {
            return command.substring(command.indexOf("in ") + 3).trim();
        }
        return null;
    }

    private static class FetchWeatherTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final String city;
        FetchWeatherTask(Context context, String city) {
            this.context = context;
            this.city = city;
        }
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlString = "https://api.weatherapi.com/v1/current.json?key=" + API_KEY + "&q=" + city;
                URL url = new URL(urlString);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder response = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                JSONObject json = new JSONObject(response.toString());
                JSONObject current = json.getJSONObject("current");
                String condition = current.getJSONObject("condition").getString("text");
                double tempC = current.getDouble("temp_c");
                return "The weather in " + city + " is " + condition + ", " + tempC + " degrees Celsius.";
            } catch (Exception e) {
                return null;
            }
        }
        @Override
        protected void onPostExecute(String result) {
            if (result != null) {
                FeedbackProvider.speakAndToast(context, result);
            } else {
                FeedbackProvider.speakAndToast(context, "Sorry, I couldn't get the weather for " + city + ".");
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 