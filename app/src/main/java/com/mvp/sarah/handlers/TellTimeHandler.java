package com.mvp.sarah.handlers;

import android.content.Context;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class TellTimeHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "tell me the time",
        "what time is it",
        "current time"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("tell me the time") ||
               lower.contains("what time is it") ||
               lower.contains("current time");
    }

    @Override
    public void handle(Context context, String command) {
        String time = new SimpleDateFormat("hh:mm a", Locale.getDefault()).format(new Date());
        FeedbackProvider.speakAndToast(context, "The time is " + time);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 