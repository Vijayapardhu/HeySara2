package com.mvp.sarah.handlers;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Looper;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import android.location.Location;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WeatherReportHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "weather report",
        "what's the weather",
        "weather in [location]"
    );
    private static final String WEATHER_API_KEY = "YOUR_OPENWEATHERMAP_API_KEY";
    private static final String DEFAULT_LOCATION = "Hyderabad";

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("weather report") ||
               lower.contains("what's the weather") ||
               lower.matches(".*weather in .*");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        String location = null;
        if (lower.matches(".*weather in .+")) {
            String[] parts = lower.split("weather in ", 2);
            if (parts.length > 1) {
                location = parts[1].trim();
            }
        }
        if (location != null) {
            FeedbackProvider.speakAndToast(context, "Fetching weather for " + location + "...");
            new FetchWeatherTask(context, location, null, null).execute();
        } else {
            // Use device location
            fetchWeatherForCurrentLocation(context);
        }
    }

    @SuppressLint("MissingPermission")
    private void fetchWeatherForCurrentLocation(Context context) {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            FeedbackProvider.speakAndToast(context, "Location permission not granted. Please enable location for weather updates. Using default city.");
            new FetchWeatherTask(context, DEFAULT_LOCATION, null, null).execute();
            return;
        }
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                new FetchWeatherTask(context, null, location.getLatitude(), location.getLongitude()).execute();
            } else {
                FeedbackProvider.speakAndToast(context, "Could not get current location. Using default city.");
                new FetchWeatherTask(context, DEFAULT_LOCATION, null, null).execute();
            }
        }).addOnFailureListener(e -> {
            FeedbackProvider.speakAndToast(context, "Could not get current location. Using default city.");
            new FetchWeatherTask(context, DEFAULT_LOCATION, null, null).execute();
        });
    }

    private static class FetchWeatherTask extends AsyncTask<Void, Void, String> {
        private final Context context;
        private final String location;
        private final Double lat;
        private final Double lon;
        FetchWeatherTask(Context ctx, String location, Double lat, Double lon) {
            this.context = ctx; this.location = location; this.lat = lat; this.lon = lon;
        }
        @Override
        protected String doInBackground(Void... voids) {
            try {
                String urlStr;
                if (lat != null && lon != null) {
                    urlStr = "https://api.openweathermap.org/data/2.5/weather?lat=" + lat + "&lon=" + lon + "&appid=" + WEATHER_API_KEY + "&units=metric";
                } else {
                    urlStr = "https://api.openweathermap.org/data/2.5/weather?q=" + URLEncoder.encode(location, "UTF-8") + "&appid=" + WEATHER_API_KEY + "&units=metric";
                }
                URL url = new URL(urlStr);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("GET");
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                JSONObject json = new JSONObject(sb.toString());
                String city = json.getString("name");
                JSONObject main = json.getJSONObject("main");
                double temp = main.getDouble("temp");
                int humidity = main.getInt("humidity");
                String desc = json.getJSONArray("weather").getJSONObject(0).getString("description");
                return "Weather in " + city + ": " + desc + ", temperature " + temp + "°C, humidity " + humidity + "%";
            } catch (Exception e) {
                return "Sorry, I couldn't fetch the weather right now.";
            }
        }
        @Override
        protected void onPostExecute(String weather) {
            FeedbackProvider.speakAndToast(context, weather);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 