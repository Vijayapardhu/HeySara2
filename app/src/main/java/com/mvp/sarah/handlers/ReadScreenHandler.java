package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class ReadScreenHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    // This is the action our Accessibility Service will listen for.
    public static final String ACTION_READ_SCREEN = "com.mvp.sarah.ACTION_READ_SCREEN";

    private static final List<String> COMMANDS = Arrays.asList(
            "read the screen",
            "read this page"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("read screen") || command.contains("read the screen") || command.contains("read this page");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Reading screen.");
        Intent intent = new Intent(ACTION_READ_SCREEN);
        context.sendBroadcast(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 