package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class WhatsAppHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "whatsapp [contact/number] that [message]",
        "send whatsapp to [contact/number] that [message]",
        "wa [contact/number] that [message]"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        return lowerCmd.startsWith("whatsapp ") || 
               lowerCmd.startsWith("send whatsapp") ||
               lowerCmd.startsWith("wa ");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        String recipient = "";
        String message = "";
        
        // Parse different command formats
        if (lowerCmd.startsWith("whatsapp ") && lowerCmd.contains(" that ")) {
            // Format: "whatsapp [recipient] that [message]"
            String[] parts = command.split(" that ", 2);
            if (parts.length >= 2) {
                recipient = parts[0].substring(9).trim(); // Remove "whatsapp " prefix
                message = parts[1].trim();
            }
        } else if (lowerCmd.startsWith("wa ") && lowerCmd.contains(" that ")) {
            // Format: "wa [recipient] that [message]"
            String[] parts = command.split(" that ", 2);
            if (parts.length >= 2) {
                recipient = parts[0].substring(3).trim(); // Remove "wa " prefix
                message = parts[1].trim();
            }
        } else if (lowerCmd.startsWith("send whatsapp to ")) {
            String[] parts = command.split(" to | that ");
            if (parts.length >= 3) {
                recipient = parts[1].trim();
                message = parts[2].trim();
            }
        }
        
        if (recipient.isEmpty() || message.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify both a recipient and a message. For example: whatsapp John that hello there");
            return;
        }
        
        String number = recipient;
        String contactName = recipient;

        // Check if it's a phone number
        if (!recipient.matches("[\\d\\s\\+\\-\\(\\)]+")) {
            // Try to look up contact
            number = getNumberForContact(context, recipient);
            if (number == null) {
                FeedbackProvider.speakAndToast(context, "Contact not found: " + recipient + ". Please check the name or use a phone number.");
                return;
            }
        } else {
            // It's a number, clean it up
            number = recipient.replaceAll("[\\s\\-\\(\\)]", "");
            contactName = number; // Use number as name for display
        }
        
        // Confirm before sending
        String confirmation = "Sending WhatsApp message to " + contactName + ": " + message;
        FeedbackProvider.speakAndToast(context, confirmation);
        
        // Open WhatsApp with the message pre-filled
        openWhatsAppWithMessage(context, number, message, contactName);
    }
    
    private void openWhatsAppWithMessage(Context context, String number, String message, String contactName) {
        try {
            // Format the number for WhatsApp (remove any non-digit characters except +)
            String cleanNumber = number.replaceAll("[^\\d+]", "");
            
            // Create WhatsApp URL with pre-filled message
            String whatsappUrl = "https://wa.me/" + cleanNumber + "?text=" + Uri.encode(message);
            
            // Open WhatsApp
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse(whatsappUrl));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            
            // Send broadcast to accessibility service to automatically send the message
            Intent accessibilityIntent = new Intent("com.mvp.sarah.ACTION_SEND_WHATSAPP");
            accessibilityIntent.putExtra("contact_name", contactName);
            accessibilityIntent.putExtra("message", message);
            context.sendBroadcast(accessibilityIntent);
            
        } catch (Exception e) {
            android.util.Log.e("WhatsAppHandler", "Error opening WhatsApp: " + e.getMessage());
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't open WhatsApp. Please make sure WhatsApp is installed.");
        }
    }

    private String getNumberForContact(Context context, String name) {
        Cursor cursor = context.getContentResolver().query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                new String[]{ContactsContract.CommonDataKinds.Phone.NUMBER, ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME},
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME + " LIKE ?",
                new String[]{"%" + name + "%"},
                null
        );
        if (cursor != null) {
            try {
                if (cursor.moveToFirst()) {
                    return cursor.getString(cursor.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER));
                }
            } finally {
                cursor.close();
            }
        }
        return null;
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "whatsapp John that hello there",
            "wa Mom that I'll be home soon",
            "send whatsapp to Dad that happy birthday",
            "whatsapp 1234567890 that thanks for calling",
            "wa Alice that meeting at 3pm"
        );
    }
} 