package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import com.mvp.sarah.ImageGenerationService;
import java.util.Arrays;
import java.util.List;

public class ImageGenerationHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        return command.toLowerCase().startsWith("generate image of ");
    }

    @Override
    public void handle(Context context, String command) {
        String prompt = command.substring("generate image of ".length()).trim();
        if (prompt.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify what image to generate.");
            return;
        }
        FeedbackProvider.speakAndToast(context, "Generating image of " + prompt);
        Intent intent = new Intent(context, ImageGenerationService.class);
        intent.putExtra("prompt", prompt);
        context.startService(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "generate image of a cat",
            "generate image of a robot holding a skateboard"
        );
    }
} 