package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class ReadNewNotificationHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("read out the new notification") || lower.contains("read latest notification");
    }

    @Override
    public void handle(Context context, String command) {
        Intent intent = new Intent("com.mvp.sarah.ACTION_READ_NEW_NOTIFICATION");
        context.sendBroadcast(intent);
        FeedbackProvider.speakAndToast(context, "Reading the latest notification.");
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("read out the new notification", "read latest notification");
    }
} 