package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import com.mvp.sarah.EmergencyContactsActivity;
import java.util.Arrays;
import java.util.List;

public class SetupEmergencyHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "setup emergency",
            "emergency setup",
            "configure emergency",
            "emergency contacts",
            "add emergency contact"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        return lowerCmd.contains("setup emergency") ||
               lowerCmd.contains("emergency setup") ||
               lowerCmd.contains("configure emergency") ||
               lowerCmd.contains("emergency contacts") ||
               lowerCmd.contains("add emergency contact") ||
               lowerCmd.contains("manage emergency") ||
               lowerCmd.contains("manage emergency services") ||
               lowerCmd.contains("emergency services");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Opening emergency contacts setup.");
        Intent intent = new Intent(context, EmergencyContactsActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 