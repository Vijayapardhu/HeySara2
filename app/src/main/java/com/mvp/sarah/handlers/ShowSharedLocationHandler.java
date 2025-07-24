package com.mvp.sarah.handlers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.text.InputType;
import android.widget.EditText;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import android.os.Handler;
import android.os.Looper;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.DocumentSnapshot;
import javax.annotation.Nullable;
import android.view.WindowManager;


public class ShowSharedLocationHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String PREFS = "SharedLocationPrefs";
    private static final String KEY_CODES = "saved_codes";

    private ListenerRegistration locationListener;
    private AlertDialog locationDialog;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    public boolean canHandle(String command) {
        String lower = command.trim().toLowerCase(Locale.ROOT);
        return lower.contains("show location");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        if (lower.equals("show location")) {
            promptForCode(context);
        } else if (lower.startsWith("show me the location of ")) {
            String name = lower.replaceFirst("show me the location of ", "").trim();
            if (name.isEmpty()) {
                FeedbackProvider.speakAndToast(context, "Please specify the name.");
                return;
            }
            String code = getSavedCode(context, name);
            if (code == null) {
                FeedbackProvider.speakAndToast(context, "No code saved for " + name + ". Say 'show location' and enter the code first.");
                return;
            }
            fetchAndShowLocation(context, code, name, false);
        }
    }

    private void promptForCode(Context context) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle("Enter 4-digit PIN")
            .setView(input)
            .setPositiveButton("OK", (d, which) -> {
                String code = input.getText().toString().trim();
                if (code.length() != 4) {
                    FeedbackProvider.speakAndToast(context, "Invalid PIN. Please enter a 4-digit PIN.");
                    return;
                }
                fetchAndShowLocationRealtime(context, code);
            })
            .setNegativeButton("Cancel", null)
            .create();

        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
    }

    private void promptForName(Context context, String code, double lat, double lon) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        AlertDialog dialog = new AlertDialog.Builder(context)
            .setTitle("Save as (name)")
            .setView(input)
            .setPositiveButton("Save", (d, which) -> {
                String name = input.getText().toString().trim();
                if (!name.isEmpty()) {
                    saveCode(context, name, code);
                    FeedbackProvider.speakAndToast(context, "Saved as " + name);
                }
                openGoogleMaps(context, lat, lon);
            })
            .setNegativeButton("Just Show", (d, which) -> openGoogleMaps(context, lat, lon))
            .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
        dialog.show();
    }

    private void fetchAndShowLocation(Context context, String code, String name, boolean promptName) {
        FeedbackProvider.speakAndToast(context, "Fetching location...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("shared_locations").document(code).get().addOnSuccessListener(doc -> {
            if (doc.exists() && doc.getBoolean("active") != null && doc.getBoolean("active")) {
                double lat = doc.getDouble("lat");
                double lon = doc.getDouble("lon");
                if (promptName) {
                    promptForName(context, code, lat, lon);
                } else {
                    openGoogleMaps(context, lat, lon);
                }
            } else {
                FeedbackProvider.speakAndToast(context, "Location not found or sharing stopped.");
            }
        }).addOnFailureListener(e -> FeedbackProvider.speakAndToast(context, "Error fetching location."));
    }

    private void fetchAndShowLocationRealtime(Context context, String code) {
        FeedbackProvider.speakAndToast(context, "Fetching location...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("shared_locations").document(code);
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
                    showLocationDialog(context, code, lat, lon, timestamp);
                } else {
                    FeedbackProvider.speakAndToast(context, "Location not found or sharing stopped.");
                    if (locationDialog != null) locationDialog.dismiss();
                }
            }
        });
    }

    private void showLocationDialog(Context context, String code, double lat, double lon, long timestamp) {
        String timeStr = android.text.format.DateFormat.format("hh:mm:ss a", new java.util.Date(timestamp)).toString();
        String message = String.format(Locale.ENGLISH, "Latitude: %.5f\nLongitude: %.5f\nLast updated: %s", lat, lon, timeStr);
        handler.post(() -> {
            if (locationDialog != null && locationDialog.isShowing()) {
                locationDialog.setMessage(message);
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context)
                    .setTitle("Shared Location")
                    .setMessage(message)
                    .setPositiveButton("Open in Maps", (dialog, which) -> openGoogleMaps(context, lat, lon))
                    .setNegativeButton("Close", (dialog, which) -> {
                        if (locationListener != null) locationListener.remove();
                        locationDialog = null;
                    })
                    .setNeutralButton("Save", (d, w) -> promptForName(context, code, lat, lon))
                    .setCancelable(false);
                
                locationDialog = builder.create();
                locationDialog.getWindow().setType(WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY);
                locationDialog.show();
            }
        });
    }

    private void openGoogleMaps(Context context, double lat, double lon) {
        String uri = String.format(Locale.ENGLISH, "geo:%f,%f?q=%f,%f(Shared+Location)", lat, lon, lat, lon);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    private void saveCode(Context context, String name, String code) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().putString(name.toLowerCase(Locale.ROOT), code).apply();
    }

    private String getSavedCode(Context context, String name) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getString(name.toLowerCase(Locale.ROOT), null);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("show location", "show me the location of [name]");
    }
} 