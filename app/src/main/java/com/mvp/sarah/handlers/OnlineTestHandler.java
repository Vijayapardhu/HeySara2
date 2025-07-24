package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.FeedbackProvider;

public class OnlineTestHandler implements CommandHandler {
    @Override
    public boolean canHandle(String command) {
        if (command == null) return false;
        String lower = command.toLowerCase();
        return lower.contains("write online test") || lower.contains("write the online test");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Starting online test automation.");
        Intent intent = new Intent("com.mvp.sarah.ACTION_AUTOMATE_ONLINE_TEST");
        context.sendBroadcast(intent);
    }

    // Keep the static method for direct use if needed
    public static void handleVoiceCommand(Context context, String recognizedText) {
        if (recognizedText != null && recognizedText.toLowerCase().contains("write online test")) {
            FeedbackProvider.speakAndToast(context, "Starting online test automation.");
            Intent intent = new Intent("com.mvp.sarah.ACTION_AUTOMATE_ONLINE_TEST");
            context.sendBroadcast(intent);
        }
    }
}
