package com.mvp.sarah.handlers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.widget.EditText;
import com.mvp.sarah.CameraAnalysisActivity;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class TranslateModeHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        return command.toLowerCase().contains("translate mode");
    }

    @Override
    public void handle(Context context, String command) {
        String[] options = {"Camera Translation", "Text Translation"};
        new AlertDialog.Builder(context)
            .setTitle("Choose Translation Mode")
            .setItems(options, (dialog, which) -> {
                if (which == 0) {
                    // Camera translation
                    FeedbackProvider.speakAndToast(context, "Opening camera for translation.");
                    Intent intent = new Intent(context, CameraAnalysisActivity.class);
                    intent.putExtra("mode", "translate");
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                } else {
                    // Text translation
                    promptForText(context);
                }
            })
            .show();
    }

    private void promptForText(Context context) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(context)
            .setTitle("Enter text to translate")
            .setView(input)
            .setPositiveButton("Next", (dialog, which) -> {
                String text = input.getText().toString().trim();
                if (text.isEmpty()) {
                    FeedbackProvider.speakAndToast(context, "No text entered.");
                    return;
                }
                promptForLanguage(context, text);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void promptForLanguage(Context context, String text) {
        EditText input = new EditText(context);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        new AlertDialog.Builder(context)
            .setTitle("Enter target language (e.g., Hindi)")
            .setView(input)
            .setPositiveButton("Translate", (dialog, which) -> {
                String lang = input.getText().toString().trim();
                if (lang.isEmpty()) {
                    FeedbackProvider.speakAndToast(context, "No language entered.");
                    return;
                }
                // Use existing TranslateHandler logic if available, or implement translation here
                FeedbackProvider.speakAndToast(context, "Translating to " + lang + "...");
                // TODO: Call translation API or handler
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("translate mode");
    }
} 