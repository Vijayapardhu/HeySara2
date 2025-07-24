package com.mvp.sarah.handlers;

import android.content.Context;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class HelpHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "what can you do",
            "help"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCaseCommand = command.toLowerCase();
        return lowerCaseCommand.equals("what can you do") || lowerCaseCommand.equals("help");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "I can do many things, like set alarms, get weather info, tell jokes, and much more. How can I help you right now?");
        // A more advanced version would list commands from the CommandRegistry.
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 