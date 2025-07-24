package com.mvp.sarah.handlers;

import android.content.Context;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class StopShareLocationHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("stop sharing my location");
    }

    @Override
    public void handle(Context context, String command) {
        ShareLocationHandler.stopSharing(context);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("stop sharing my location");
    }
} 