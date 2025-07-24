package com.mvp.sarah.handlers;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Looper;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.SharedPreferences;

public class ShareLocationHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String FIRESTORE_COLLECTION = "shared_locations";
    private static String activeCode = null;
    private static LocationCallback locationCallback = null;

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.contains("share my location");
    }

    @Override
    public void handle(Context context, String command) {
        if (activeCode != null) {
            FeedbackProvider.speakAndToast(context, "You are already sharing your location. Your PIN is " + formatPinForSpeech(activeCode));
            copyPinToClipboard(context, activeCode);
            return;
        }
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            FeedbackProvider.speakAndToast(context, "Location permission not granted. Please enable location.");
            return;
        }
        String code = generatePin();
        activeCode = code;
        // Store PIN in SharedPreferences
        SharedPreferences prefs = context.getSharedPreferences("LocationSharePrefs", Context.MODE_PRIVATE);
        prefs.edit().putString("last_location_pin", code).apply();
        // Speak PIN digit by digit
        FeedbackProvider.speakAndToast(context, "Sharing your location. Your PIN is " + formatPinForSpeech(code));
        copyPinToClipboard(context, code);
        startLocationUpdates(context, code);
    }

    private String generatePin() {
        Random rand = new Random();
        int num = 1000 + rand.nextInt(9000);
        return String.valueOf(num);
    }

    private String formatPinForSpeech(String pin) {
        // e.g., "1 2 3 4"
        return pin.replaceAll("", " ").trim();
    }

    private void copyPinToClipboard(Context context, String pin) {
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Location PIN", pin);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "PIN copied to clipboard!", Toast.LENGTH_SHORT).show();
    }

    private void startLocationUpdates(Context context, String code) {
        FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setInterval(10000); // 10 seconds
        locationRequest.setFastestInterval(5000);
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) return;
                Location location = locationResult.getLastLocation();
                if (location != null) {
                    uploadLocationToFirebase(code, location);
                }
            }
        };
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        } else {
            FeedbackProvider.speakAndToast(context, "Location permission not granted. Please enable location.");
        }
    }

    private void uploadLocationToFirebase(String code, Location location) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        Map<String, Object> data = new HashMap<>();
        data.put("lat", location.getLatitude());
        data.put("lon", location.getLongitude());
        data.put("timestamp", System.currentTimeMillis());
        data.put("active", true);
        db.collection(FIRESTORE_COLLECTION).document(code).set(data);
    }

    public static void stopSharing(Context context) {
        if (activeCode == null) return;
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection(FIRESTORE_COLLECTION).document(activeCode).update("active", false);
        if (locationCallback != null) {
            FusedLocationProviderClient fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
            fusedLocationClient.removeLocationUpdates(locationCallback);
            locationCallback = null;
        }
        FeedbackProvider.speakAndToast(context, "Stopped sharing your location.");
        activeCode = null;
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("share my location");
    }
} 