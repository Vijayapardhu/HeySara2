package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CameraAnalysisActivity;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class ImageAnalysisHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "analyze this picture",
            "describe what you see",
            "what is this"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("analyze") || command.contains("describe") || command.contains("what is this");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Okay, opening the camera.");
        Intent intent = new Intent(context, CameraAnalysisActivity.class);
        intent.putExtra("mode", "analyze"); // To differentiate from translate mode later
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 