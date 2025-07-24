package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CameraAnalysisActivity;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CameraTranslateHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "translate this with the camera",
            "camera translate to french"
    );

    private static final Pattern LANG_PATTERN = Pattern.compile("to (\\w+)", Pattern.CASE_INSENSITIVE);

    @Override
    public boolean canHandle(String command) {
        return command.contains("camera translate") || (command.contains("translate") && command.contains("camera"));
    }

    @Override
    public void handle(Context context, String command) {
        Matcher matcher = LANG_PATTERN.matcher(command);
        String targetLang = "en"; // Default to Spanish
        if (matcher.find()) {
            targetLang = matcher.group(1);
        }

        FeedbackProvider.speakAndToast(context, "Okay, opening the camera to translate.");
        Intent intent = new Intent(context, CameraAnalysisActivity.class);
        intent.putExtra("mode", "translate");
        intent.putExtra("target_lang", targetLang);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 