package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class ScreenshotHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "take a screenshot",
        "screenshot",
        "capture screen",
        "screen capture",
        "take screenshot",
        "capture a screenshot",
        "screen shot"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        for (String cmd : COMMANDS) {
            if (lower.contains(cmd)) return true;
        }
        return false;
    }

    @Override
    public void handle(Context context, String command) {
        // Send broadcast to trigger screenshot via quick tile
        Intent intent = new Intent("com.mvp.sarah.ACTION_TRIGGER_QUICK_TILE");
        intent.putExtra("tile_keyword", "Screenshot");
        context.sendBroadcast(intent);
        FeedbackProvider.speakAndToast(context, "Taking a screenshot");
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 