package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.database.Cursor;
import android.provider.ContactsContract;
import android.widget.Toast;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class CallContactHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "call [contact name]",
            "call [number]",
            "make a call to [contact name]",
            "make a call to [number]",
            "dial [contact name]",
            "dial [number]"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        return lowerCmd.startsWith("call ") ||
               lowerCmd.startsWith("make a call to ") ||
               lowerCmd.startsWith("dial ");
    }

    @Override
    public void handle(Context context, String command) {
        String contactOrNumber = "";
        String lowerCmd = command.toLowerCase();
        
        if (lowerCmd.startsWith("call ")) {
            contactOrNumber = command.substring(5).trim();
        } else if (lowerCmd.startsWith("make a call to ")) {
            contactOrNumber = command.substring(15).trim();
        } else if (lowerCmd.startsWith("dial ")) {
            contactOrNumber = command.substring(5).trim();
        }
        
        if (contactOrNumber.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify who to call");
            return;
        }
        
        // Check if it's a direct number
        if (contactOrNumber.matches("[\\d\\s\\+\\-\\(\\)]+")) {
            // Looks like a number - call directly
            String cleanNumber = contactOrNumber.replaceAll("[\\s\\-\\(\\)]", "");
            makeDirectCall(context, cleanNumber);
        } else {
            // Try to look up contact
            String number = getNumberForContact(context, contactOrNumber);
            if (number != null) {
                makeDirectCall(context, number);
            } else {
                FeedbackProvider.speakAndToast(context, "Contact not found: " + contactOrNumber);
            }
        }
    }
    
    private void makeDirectCall(Context context, String number) {
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Calling " + number);
        } catch (SecurityException e) {
            // Fallback to dialer if CALL_PHONE permission not granted
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.setData(Uri.parse("tel:" + number));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Opening dialer for " + number);
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
        return COMMANDS;
    }
} 