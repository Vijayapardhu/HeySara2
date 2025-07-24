package com.mvp.sarah;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.Uri;
import android.os.Handler;
import android.telephony.SmsManager;
import android.telephony.PhoneNumberUtils;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.tasks.OnSuccessListener;
import java.util.List;
import android.content.BroadcastReceiver;
import android.util.Log;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationResult;

public class EmergencyActions {
    private static final String TAG = "EmergencyActions";
    private static boolean emergencyCallAnswered = false;
    private static BroadcastReceiver callAnsweredReceiver;
    private static Handler emergencyHandler = new Handler();

    public static void triggerEmergency(Context context) {
        // Register receiver for call answered
        if (callAnsweredReceiver == null) {
            callAnsweredReceiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent intent) {
                    Log.d(TAG, "CALL_ANSWERED broadcast received. Setting flag and clearing handler.");
                    emergencyCallAnswered = true;
                    emergencyHandler.removeCallbacksAndMessages(null);
                    Toast.makeText(ctx, "Emergency call answered. Stopping all further calls.", Toast.LENGTH_LONG).show();
                }
            };
            context.getApplicationContext().registerReceiver(
                callAnsweredReceiver,
                new android.content.IntentFilter("com.mvp.sarah.CALL_ANSWERED"),
                Context.RECEIVER_NOT_EXPORTED
            );
        }
        emergencyCallAnswered = false;
        Log.d(TAG, "Starting emergency call sequence. Flag reset. Contacts: " + EmergencyContactsManager.getContacts(context).size());
        List<EmergencyContactsManager.Contact> contacts = EmergencyContactsManager.getContacts(context);
        if (contacts.isEmpty()) {
            Toast.makeText(context, "No emergency contacts set!", Toast.LENGTH_LONG).show();
            return;
        }
        SharedPreferences prefs = context.getSharedPreferences("SaraEmergencyPrefs", Context.MODE_PRIVATE);
        String message = prefs.getString("emergency_message", "I'm in trouble. Please help me! My location: {location_link}");
        FusedLocationProviderClient locationClient = LocationServices.getFusedLocationProviderClient(context);
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(context, "Location permission required for emergency message!", Toast.LENGTH_LONG).show();
            sendMessagesAndCalls(context, contacts, message.replace("{location_link}", "Location unavailable"));
            return;
        }
        locationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                String locationLink = "https://maps.google.com/?q=" + location.getLatitude() + "," + location.getLongitude();
                String msg = message.replace("{location_link}", locationLink);
                sendMessagesAndCalls(context, contacts, msg);
            } else {
                // Request a fresh location if last location is null
                LocationRequest locationRequest = LocationRequest.create()
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                        .setNumUpdates(1)
                        .setInterval(1000);
                locationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                    @Override
                    public void onLocationResult(LocationResult locationResult) {
                        Location freshLocation = locationResult != null ? locationResult.getLastLocation() : null;
                        String locationLink = "Location unavailable";
                        if (freshLocation != null) {
                            locationLink = "https://maps.google.com/?q=" + freshLocation.getLatitude() + "," + freshLocation.getLongitude();
                        }
                        String msg = message.replace("{location_link}", locationLink);
                        sendMessagesAndCalls(context, contacts, msg);
                        // Remove updates after first result
                        locationClient.removeLocationUpdates(this);
                    }
                }, null);
            }
        });
        // Show the full-screen stop overlay
        Intent stopIntent = new Intent(context, EmergencyStopActivity.class);
        stopIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        context.startActivity(stopIntent);
    }

    private static void sendMessagesAndCalls(Context context, List<EmergencyContactsManager.Contact> contacts, String message) {
        // Send SMS to all contacts
        for (EmergencyContactsManager.Contact c : contacts) {
            try {
                SmsManager sms = SmsManager.getDefault();
                sms.sendTextMessage(c.phone, null, message, null, null);
                Toast.makeText(context, "SMS sent to " + c.name, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(context, "Failed to send SMS to " + c.name, Toast.LENGTH_SHORT).show();
            }
        }
        // Start calling contacts in order, with delay between each
        callContactsInOrder(context, contacts, 0);
    }

    private static void callContactsInOrder(Context context, List<EmergencyContactsManager.Contact> contacts, int index) {
        callContactsInOrder(context, contacts, index, 1);
    }

    // New overloaded method to track attempts
    private static void callContactsInOrder(Context context, List<EmergencyContactsManager.Contact> contacts, int index, int attempt) {
        Log.d(TAG, "callContactsInOrder: index=" + index + ", attempt=" + attempt + ", flag=" + emergencyCallAnswered);
        if (emergencyCallAnswered) {
            Log.d(TAG, "Sequence stopped: emergencyCallAnswered is true (before call). No further calls.");
            Toast.makeText(context, "Emergency call answered. Stopping sequence.", Toast.LENGTH_LONG).show();
            return;
        }
        if (index >= contacts.size()) {
            Log.d(TAG, "All emergency contacts called. Sequence complete.");
            Toast.makeText(context, "All emergency contacts called.", Toast.LENGTH_LONG).show();
            return;
        }
        // Defensive check before starting a new call
        if (emergencyCallAnswered) {
            Log.d(TAG, "Sequence stopped: emergencyCallAnswered is true (defensive before call). No further calls.");
            Toast.makeText(context, "Emergency call answered. Stopping sequence.", Toast.LENGTH_LONG).show();
            return;
        }
        EmergencyContactsManager.Contact c = contacts.get(index);
        String attemptMsg = attempt == 1 ? "Calling " : "Retrying ";
        Log.d(TAG, attemptMsg + c.name + " (" + c.phone + ")");
        Toast.makeText(context, attemptMsg + c.name, Toast.LENGTH_SHORT).show();
        Intent callIntent = new Intent(Intent.ACTION_CALL);
        callIntent.setData(Uri.parse("tel:" + c.phone));
        callIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(callIntent);
            Log.d(TAG, "Started call intent for: " + c.phone);
        } catch (Exception e) {
            Log.e(TAG, "Failed to call " + c.name + ": " + e.getMessage());
            Toast.makeText(context, "Failed to call " + c.name, Toast.LENGTH_SHORT).show();
        }
        emergencyHandler.postDelayed(() -> {
            Log.d(TAG, "Handler triggered for index=" + index + ", attempt=" + attempt + ", flag=" + emergencyCallAnswered);
            if (emergencyCallAnswered) {
                Log.d(TAG, "Sequence stopped: emergencyCallAnswered is true (in handler). No further calls.");
                Toast.makeText(context, "Emergency call answered. Stopping sequence.", Toast.LENGTH_LONG).show();
                return;
            }
            if (attempt < 2) {
                Log.d(TAG, "Retrying same contact: " + c.name);
                callContactsInOrder(context, contacts, index, attempt + 1);
            } else {
                Log.d(TAG, "Moving to next contact. Current: " + c.name + ", Next index: " + (index + 1));
                callContactsInOrder(context, contacts, index + 1, 1);
            }
        }, 30000);
    }

    // Call this to stop the emergency sequence from anywhere (e.g., EmergencyStopActivity)
    public static void stopEmergencySequence(Context context) {
        Log.d(TAG, "stopEmergencySequence called. Setting flag and clearing handler.");
        emergencyCallAnswered = true;
        emergencyHandler.removeCallbacksAndMessages(null);
        Toast.makeText(context, "Emergency calling stopped.", Toast.LENGTH_LONG).show();
    }
} 