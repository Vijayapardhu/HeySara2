package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.telephony.SmsManager;
import android.widget.Toast;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import android.os.Handler;

public class SendSmsHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "send [contact/number] that [message]",
        "text [contact/number] [message]",
        "message [contact/number] [message]",
        "send sms to [contact/number] that [message]",
        "send a text to [contact/number] saying [message]"
    );

    // Conversational state for confirmation
    private static String pendingNumber = null;
    private static String pendingContactName = null;
    private static String pendingMessage = null;
    private static boolean awaitingConfirmation = false;

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        return lowerCmd.startsWith("send ") || 
               lowerCmd.startsWith("text ") ||
               lowerCmd.startsWith("message ") ||
               lowerCmd.startsWith("send sms") ||
               lowerCmd.startsWith("send a text");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        // If awaiting confirmation
        if (awaitingConfirmation && pendingNumber != null && pendingMessage != null) {
            if (lowerCmd.contains("yes") || lowerCmd.contains("send it") || lowerCmd.contains("confirm")) {
                sendSmsDirectly(context, pendingNumber, pendingMessage, pendingContactName);
                awaitingConfirmation = false;
                pendingNumber = null;
                pendingContactName = null;
                pendingMessage = null;
            } else {
                FeedbackProvider.speakAndToast(context, "Cancelled sending the message.");
                awaitingConfirmation = false;
                pendingNumber = null;
                pendingContactName = null;
                pendingMessage = null;
            }
            return;
        }
        String recipient = "";
        String message = "";
        
        // Parse different command formats - prioritize the new "send X that Y" format
        if (lowerCmd.startsWith("send ") && lowerCmd.contains(" that ")) {
            // New format: "send [recipient] that [message]"
            String[] parts = command.split(" that ", 2);
            if (parts.length >= 2) {
                recipient = parts[0].substring(5).trim(); // Remove "send " prefix
                message = parts[1].trim();
            }
        } else if (lowerCmd.startsWith("text ")) {
            // Format: "text [recipient] [message]"
            String[] parts = command.split(" ", 3);
            if (parts.length >= 3) {
                recipient = parts[1].trim();
                message = parts[2].trim();
            }
        } else if (lowerCmd.startsWith("message ")) {
            // Format: "message [recipient] [message]"
            String[] parts = command.split(" ", 3);
            if (parts.length >= 3) {
                recipient = parts[1].trim();
                message = parts[2].trim();
            }
        } else if (lowerCmd.startsWith("send sms to ")) {
            String[] parts = command.split(" to | that ");
            if (parts.length >= 3) {
                recipient = parts[1].trim();
                message = parts[2].trim();
            }
        } else if (lowerCmd.startsWith("send a text to ")) {
            String[] parts = command.split(" to | saying ");
            if (parts.length >= 3) {
                recipient = parts[1].trim();
                message = parts[2].trim();
            }
        }
        
        if (recipient.isEmpty() || message.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify both a recipient and a message. For example: send John that hello there");
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
        
        // Ask for confirmation before sending
        pendingNumber = number;
        pendingContactName = contactName;
        pendingMessage = message;
        awaitingConfirmation = true;
        String confirmation = "Do you want to send this message to " + contactName + ": " + message + "?";
        FeedbackProvider.speakAndToastWithCallback(context, confirmation, () -> {
            // Start speech recognition after TTS prompt
            Intent listenIntent = new Intent("com.mvp.sarah.ACTION_START_COMMAND_LISTENING");
            listenIntent.setPackage(context.getPackageName());
            context.startService(listenIntent);
        });
    }
    
    private void sendSmsDirectly(Context context, String number, String message, String contactName) {
        try {
            SmsManager smsManager = SmsManager.getDefault();
            
            // Split message if it's too long
            if (message.length() > 160) {
                List<String> parts = smsManager.divideMessage(message);
                for (String part : parts) {
                    smsManager.sendTextMessage(number, null, part, null, null);
                }
                FeedbackProvider.speakAndToast(context, "Long message sent to " + contactName + " in " + parts.size() + " parts");
            } else {
                smsManager.sendTextMessage(number, null, message, null, null);
                FeedbackProvider.speakAndToast(context, "Message sent to " + contactName);
            }
        } catch (Exception e) {
            android.util.Log.e("SendSmsHandler", "Error sending SMS: " + e.getMessage());
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't send the message. Please check your SMS permissions.");
            
            // Fallback: open SMS app
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("smsto:" + number));
            intent.putExtra("sms_body", message);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
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
            "send John that hello there",
            "send Mom that I'll be home soon",
            "send 1234567890 that thanks for calling",
            "send Dad that happy birthday",
            "send Alice that meeting at 3pm"
        );
    }
} 