package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class FindPhoneHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "find my phone",
            "where is my phone",
            "ring my phone",
            "make my phone ring"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        return lowerCmd.contains("find my phone") ||
               lowerCmd.contains("where is my phone") ||
               lowerCmd.contains("ring my phone") ||
               lowerCmd.contains("make my phone ring");
    }

    @Override
    public void handle(Context context, String command) {
        Intent intent = new Intent(context, com.mvp.sarah.FindPhoneActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
        FeedbackProvider.speakAndToast(context, "Ringing your phone at maximum volume! Tap anywhere or press a volume button to stop.");
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
}
