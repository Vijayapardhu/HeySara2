package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class BackToHomeHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "back to home",
        "go to home",
        "home screen"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("back to home") ||
               lower.contains("go to home") ||
               lower.contains("home screen");
    }

    @Override
    public void handle(Context context, String command) {
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_HOME);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
        FeedbackProvider.speakAndToast(context, "Returning to home screen.");
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 