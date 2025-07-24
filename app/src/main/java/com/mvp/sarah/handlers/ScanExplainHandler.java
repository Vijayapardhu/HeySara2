package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class ScanExplainHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("scan and explain") || lower.contains("read and explain text on screen");
    }

    @Override
    public void handle(Context context, String command) {
        Intent intent = new Intent("com.mvp.sarah.ACTION_SCAN_AND_EXPLAIN");
        context.sendBroadcast(intent);
        FeedbackProvider.speakAndToast(context, "Scanning and explaining the screen.");
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("scan and explain", "read and explain text on screen");
    }
} 