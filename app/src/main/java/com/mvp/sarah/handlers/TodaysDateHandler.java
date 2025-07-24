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

public class TodaysDateHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "today's date",
        "what is the date",
        "current date"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("today's date") ||
               lower.contains("what is the date") ||
               lower.contains("current date");
    }

    @Override
    public void handle(Context context, String command) {
        String date = new SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.getDefault()).format(new Date());
        FeedbackProvider.speakAndToast(context, "Today's date is " + date);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 