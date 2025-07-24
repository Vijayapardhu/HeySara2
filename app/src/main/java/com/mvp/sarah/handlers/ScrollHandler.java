package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class ScrollHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("scroll left") || lower.contains("scroll right");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase();
        String direction = lower.contains("left") ? "left" : "right";
        Intent intent = new Intent(direction.equals("left") ? "com.mvp.sarah.ACTION_SCROLL_LEFT" : "com.mvp.sarah.ACTION_SCROLL_RIGHT");
        context.sendBroadcast(intent);
        FeedbackProvider.speakAndToast(context, "Scrolling " + direction);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("scroll left", "scroll right");
    }
} 