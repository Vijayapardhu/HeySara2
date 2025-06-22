package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PaymentHandler implements CommandHandler, CommandRegistry.SuggestionProvider {

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        return lowerCmd.startsWith("pay ") || lowerCmd.startsWith("send money") || lowerCmd.startsWith("scan");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);

        // Handle "scan" commands
        if (lowerCmd.startsWith("scan")) {
            Uri uri = Uri.parse("upi://pay");
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            String feedback = "Opening UPI scanner";

            // Check if a specific app is mentioned
            if (lowerCmd.contains("using")) {
                try {
                    String appName = lowerCmd.substring(lowerCmd.lastIndexOf("using") + 5).trim();
                    String packageName = getPackageNameForApp(appName);
                    if (packageName != null) {
                        intent.setPackage(packageName);
                        feedback = "Opening scanner in " + appName;
                    } else {
                        feedback = "Couldn't find " + appName + ", opening default scanner.";
                    }
                } catch (Exception e) {
                    // Ignore parsing errors, just open default
                }
            }

            FeedbackProvider.speakAndToast(context, feedback);

            PackageManager pm = context.getPackageManager();
            if (intent.resolveActivity(pm) != null) {
                context.startActivity(intent);
            } else {
                FeedbackProvider.speakAndToast(context, "No UPI app is installed to handle scanning.");
            }
            return; // Done with scan command
        }

        // Existing logic for "pay [amount] to [recipient]"
        try {
            // 1. Extract details from the command
            String[] parts = lowerCmd.split(" to | using ");
            if (parts.length < 3) {
                FeedbackProvider.speakAndToast(context, "Please provide the amount, recipient, and payment app. For example: pay 500 to John using Google Pay.");
                return;
            }

            String amount = parts[0].replaceAll("[^\\d.]", "");
            String recipientName = parts[1].trim();
            String appName = parts[2].trim();

            // 2. Find the recipient's UPI ID from contacts
            String upiId = getUpiIdForContact(context, recipientName);
            if (upiId == null) {
                FeedbackProvider.speakAndToast(context, "Sorry, I couldn't find a UPI ID for " + recipientName);
                return;
            }

            // 3. Construct the UPI payment URI
            Uri uri = new Uri.Builder()
                .scheme("upi")
                .authority("pay")
                .appendQueryParameter("pa", upiId)
                .appendQueryParameter("pn", recipientName)
                .appendQueryParameter("am", amount)
                .appendQueryParameter("cu", "INR")
                .appendQueryParameter("tn", "Sent via Sara Voice Assistant")
                .build();

            // 4. Create the Intent and set the target package
            Intent intent = new Intent(Intent.ACTION_VIEW, uri);
            String packageName = getPackageNameForApp(appName);
            
            if (packageName == null) {
                FeedbackProvider.speakAndToast(context, "Sorry, I couldn't find the app: " + appName);
                return;
            }
            intent.setPackage(packageName);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            // 5. Check if the app is installed before launching
            PackageManager pm = context.getPackageManager();
            if (intent.resolveActivity(pm) != null) {
                FeedbackProvider.speakAndToast(context, "Opening " + appName + " to pay " + amount + " to " + recipientName);
                context.startActivity(intent);
            } else {
                FeedbackProvider.speakAndToast(context, "The app " + appName + " is not installed.");
            }
        } catch (Exception e) {
            android.util.Log.e("PaymentHandler", "Error processing payment command: " + e.getMessage());
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't process that payment command.");
        }
    }

    private String getUpiIdForContact(Context context, String name) {
        // For this to work, the UPI ID must be stored in the "Notes" field of the contact.
        String[] projection = { ContactsContract.CommonDataKinds.Note.NOTE };
        String selection = ContactsContract.Data.MIMETYPE + " = ? AND " + ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?";
        String[] selectionArgs = { ContactsContract.CommonDataKinds.Note.CONTENT_ITEM_TYPE, "%" + name + "%" };
        
        Cursor cursor = context.getContentResolver().query(
            ContactsContract.Data.CONTENT_URI, 
            projection, 
            selection, 
            selectionArgs, 
            null
        );

        if (cursor != null && cursor.moveToFirst()) {
            try {
                String note = cursor.getString(0);
                if (note != null && note.contains("@")) { // Basic check for a UPI ID format
                    return note.trim();
                }
            } finally {
                cursor.close();
            }
        }
        return null; // Return null if not found
    }

    private String getPackageNameForApp(String appName) {
        switch (appName.toLowerCase()) {
            case "google pay":
            case "gpay":
                return "com.google.android.apps.nbu.paisa.user";
            case "phonepe":
                return "com.phonepe.app";
            case "paytm":
                return "net.one97.paytm";
            case "navy":
                return "com.naviapp";
            default:
                return null;
        }
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "pay 500 to John using gpay",
            "send money 100 to Jane using phonepe",
            "scan and pay",
            "scan qr using paytm"
        );
    }
} 