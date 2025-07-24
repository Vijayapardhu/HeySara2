package com.mvp.sarah.handlers;

import android.content.Context;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import android.app.AlertDialog;
import android.content.Context;
import android.text.InputType;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import javax.annotation.Nullable;
import android.content.Intent;
import android.widget.EditText;

public class ShowLocationPersonHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private Handler handler = new Handler(Looper.getMainLooper());
    private AlertDialog locationDialog;
    private com.google.firebase.firestore.ListenerRegistration locationListener;

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.startsWith("show me location of ");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        String person = lower.replaceFirst("show me location of ", "").trim();
        if (person.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify the person whose location you want to see.");
            return;
        }
        promptForPin(context, person);
    }

    private void promptForPin(Context context, String person) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        new AlertDialog.Builder(context)
            .setTitle("Enter 4-digit PIN for " + person)
            .setView(input)
            .setPositiveButton("OK", (dialog, which) -> {
                String pin = input.getText().toString().trim();
                if (pin.length() != 4) {
                    FeedbackProvider.speakAndToast(context, "Invalid PIN. Please enter a 4-digit PIN.");
                    return;
                }
                fetchAndShowLocationRealtime(context, pin);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void fetchAndShowLocationRealtime(Context context, String pin) {
        FeedbackProvider.speakAndToast(context, "Fetching location...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("shared_locations").document(pin);
        if (locationListener != null) locationListener.remove();
        locationListener = docRef.addSnapshotListener(new EventListener<DocumentSnapshot>() {
            @Override
            public void onEvent(@Nullable DocumentSnapshot doc, @Nullable FirebaseFirestoreException error) {
                if (error != null) {
                    FeedbackProvider.speakAndToast(context, "Error fetching location.");
                    return;
                }
                if (doc != null && doc.exists() && doc.getBoolean("active") != null && doc.getBoolean("active")) {
                    double lat = doc.getDouble("lat");
                    double lon = doc.getDouble("lon");
                    long timestamp = doc.getLong("timestamp");
                    showLocationDialog(context, lat, lon, timestamp);
                } else {
                    FeedbackProvider.speakAndToast(context, "Location not found or sharing stopped.");
                    if (locationDialog != null) locationDialog.dismiss();
                }
            }
        });
    }

    private void showLocationDialog(Context context, double lat, double lon, long timestamp) {
        String timeStr = android.text.format.DateFormat.format("hh:mm:ss a", new java.util.Date(timestamp)).toString();
        String message = String.format(Locale.ENGLISH, "Latitude: %.5f\nLongitude: %.5f\nLast updated: %s", lat, lon, timeStr);
        handler.post(() -> {
            if (locationDialog != null && locationDialog.isShowing()) {
                locationDialog.setMessage(message);
            } else {
                locationDialog = new AlertDialog.Builder(context)
                    .setTitle("Shared Location")
                    .setMessage(message)
                    .setPositiveButton("Open in Maps", (dialog, which) -> openGoogleMaps(context, lat, lon))
                    .setNegativeButton("Close", (dialog, which) -> {
                        if (locationListener != null) locationListener.remove();
                        locationDialog = null;
                    })
                    .setCancelable(false)
                    .show();
            }
        });
    }

    private void openGoogleMaps(Context context, double lat, double lon) {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(Shared+Location)", lat, lon, lat, lon);
        Intent intent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("show me location of [person]");
    }
} 