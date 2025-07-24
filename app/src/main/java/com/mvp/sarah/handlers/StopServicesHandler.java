package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import com.mvp.sarah.SaraVoiceService;
import com.mvp.sarah.TriggerDetectionService;
import java.util.Arrays;
import java.util.List;

public class StopServicesHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "stop services",
        "stop sara",
        "turn off sara"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("stop services") ||
               lower.contains("stop sara") ||
               lower.contains("sara services stop")||
               lower.contains("turn off sara");
    }

    @Override
    public void handle(Context context, String command) {
        try {
            context.stopService(new Intent(context, SaraVoiceService.class));
            context.stopService(new Intent(context, TriggerDetectionService.class));
            FeedbackProvider.speakAndToast(context, "Sara services stopped.");
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Could not stop all services: " + e.getMessage());
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 