package com.mvp.sarah.handlers;

import android.content.Context;

import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class JokeHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "tell me a joke",
            "make me laugh",
            "another one"
    );

    private static final String[] JOKES = {
            "Why don't scientists trust atoms? Because they make up everything!",
            "Why did the scarecrow win an award? Because he was outstanding in his field!",
            "Why don't skeletons fight each other? They don't have the guts.",
            "What do you call fake spaghetti? An Impasta!",
            "I would tell you a joke about construction, but I'm still working on it."
    };

    private final Random random = new Random();

    @Override
    public boolean canHandle(String command) {
        return command.contains("joke") || command.contains("laugh");
    }

    @Override
    public void handle(Context context, String command) {
        String joke = JOKES[random.nextInt(JOKES.length)];
        FeedbackProvider.speakAndToast(context, joke);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 