package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class RecordVideoHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "record video",
        "start video recording",
        "take a video"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("record video") ||
               lower.contains("start video recording") ||
               lower.contains("take a video");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Opening camera to record video.");
        try {
            Intent intent = new Intent(MediaStore.ACTION_VIDEO_CAPTURE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Error opening camera: " + e.getMessage());
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 