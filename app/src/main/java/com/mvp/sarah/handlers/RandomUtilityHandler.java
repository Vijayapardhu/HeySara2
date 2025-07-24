package com.mvp.sarah.handlers;

import android.content.Context;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class RandomUtilityHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "roll a die",
            "flip a coin"
    );
    
    private final Random random = new Random();

    @Override
    public boolean canHandle(String command) {
        return command.contains("roll a die") || command.contains("flip a coin");
    }

    @Override
    public void handle(Context context, String command) {
        if (command.contains("roll a die")) {
            int result = random.nextInt(6) + 1;
            FeedbackProvider.speakAndToast(context, "You rolled a " + result);
        } else if (command.contains("flip a coin")) {
            String result = random.nextBoolean() ? "Heads" : "Tails";
            FeedbackProvider.speakAndToast(context, "It's " + result);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 