package com.mvp.sarah.handlers;

import android.content.Context;
import android.provider.Settings;
import android.widget.Toast;
import com.mvp.sarah.CommandHandler;
import android.content.Intent;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class AirplaneModeHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "turn on airplane mode",
            "turn off airplane mode",
            "enable airplane mode",
            "disable airplane mode",
            "turn on aeroplane mode",
            "turn off aeroplane mode",
            "enable aeroplane mode",
            "disable aeroplane mode"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        
        // Only handle toggle commands, not "open airplane mode settings"
        if (lowerCmd.startsWith("open ")) {
            return false;
        }
        
        return (lowerCmd.contains("turn on") || lowerCmd.contains("turn off") || 
                lowerCmd.contains("enable") || lowerCmd.contains("disable")) &&
               (lowerCmd.contains("airplane mode") || lowerCmd.contains("aeroplane mode"));
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Turning On Airplane Mode ", Toast.LENGTH_LONG);
        Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 