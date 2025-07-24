package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TypeTextHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    public static final String ACTION_TYPE_TEXT = "com.mvp.sarah.ACTION_TYPE_TEXT";
    public static final String ACTION_NEXT_LINE = "com.mvp.sarah.ACTION_NEXT_LINE";
    public static final String ACTION_SELECT_ALL = "com.mvp.sarah.ACTION_SELECT_ALL";
    public static final String ACTION_COPY = "com.mvp.sarah.ACTION_COPY";
    public static final String ACTION_CUT = "com.mvp.sarah.ACTION_CUT";
    public static final String ACTION_PASTE = "com.mvp.sarah.ACTION_PASTE";
    public static final String EXTRA_TEXT = "com.mvp.sarah.EXTRA_TEXT";

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        return lowerCmd.startsWith("type ") ||
               lowerCmd.equals("next line") ||
               lowerCmd.equals("select all") ||
               lowerCmd.equals("copy") ||
               lowerCmd.equals("cut") ||
               lowerCmd.equals("paste");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        Intent intent = null;
        if (lowerCmd.startsWith("type ")) {
            String text = command.substring(5).trim();
            intent = new Intent(ACTION_TYPE_TEXT);
            intent.putExtra(EXTRA_TEXT, text);
            FeedbackProvider.speakAndToast(context, "Typing: " + text);
        } else if (lowerCmd.equals("next line")) {
            intent = new Intent(ACTION_NEXT_LINE);
            FeedbackProvider.speakAndToast(context, "Next line");
        } else if (lowerCmd.equals("select all")) {
            intent = new Intent(ACTION_SELECT_ALL);
            FeedbackProvider.speakAndToast(context, "Select all");
        } else if (lowerCmd.equals("copy")) {
            intent = new Intent(ACTION_COPY);
            FeedbackProvider.speakAndToast(context, "Copy");
        } else if (lowerCmd.equals("cut")) {
            intent = new Intent(ACTION_CUT);
            FeedbackProvider.speakAndToast(context, "Cut");
        } else if (lowerCmd.equals("paste")) {
            intent = new Intent(ACTION_PASTE);
            FeedbackProvider.speakAndToast(context, "Paste");
        }
        if (intent != null) {
            context.sendBroadcast(intent);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "type hello world",
            "next line",
            "select all",
            "copy",
            "cut",
            "paste"
        );
    }
} 