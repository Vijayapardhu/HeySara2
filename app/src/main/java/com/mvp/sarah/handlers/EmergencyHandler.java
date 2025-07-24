package com.mvp.sarah.handlers;

import android.content.Context;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import com.mvp.sarah.EmergencyActions;
import java.util.Arrays;
import java.util.List;

public class EmergencyHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "i'm in trouble",
            "im in trouble",
            "help me"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase().replace("'", "").replace("\"", "").trim();
        return lowerCmd.contains("im in trouble") ||
               lowerCmd.contains("i am in trouble") ||
               lowerCmd.contains("help me") ||
               lowerCmd.contains("sos") ||
               lowerCmd.contains("save me") ||
               lowerCmd.matches(".*trouble.*");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Emergency mode activated!");
       // android.widget.Toast.makeText(context, "Emergency command triggered", android.widget.Toast.LENGTH_LONG).show();
        // Ensure EmergencyActions.triggerEmergency runs on main thread
        if (context instanceof android.app.Activity) {
            ((android.app.Activity) context).runOnUiThread(() -> EmergencyActions.triggerEmergency(context));
        } else {
            EmergencyActions.triggerEmergency(context);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 