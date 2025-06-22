package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ClickLabelHandler implements CommandHandler, CommandRegistry.SuggestionProvider {

    public static final String ACTION_CLICK_LABEL = "com.mvp.sara.ACTION_CLICK_LABEL";
    public static final String EXTRA_LABEL = "com.mvp.sara.EXTRA_LABEL";

    private static final List<String> COMMANDS = Arrays.asList(
            "click on",
            "tap on",
            "click",
            "like",
            "like this reel",
            "switch camera",
            "flip camera",
            "scroll down",
            "scroll up"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        for (String prefix : COMMANDS) {
            if (lowerCmd.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void handle(Context context, String command) {
        String labelToClick = "";
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        
        android.util.Log.d("ClickLabelHandler", "Handling command: '" + command + "' (lowercase: '" + lowerCmd + "')");

        if (lowerCmd.equals("like") || lowerCmd.equals("like this reel")) {
            labelToClick = "like";
            android.util.Log.d("ClickLabelHandler", "Recognized like command, setting labelToClick to: " + labelToClick);
        } else if (lowerCmd.equals("switch camera") || lowerCmd.equals("flip camera")) {
            labelToClick = "switch_camera";
            android.util.Log.d("ClickLabelHandler", "Recognized camera switch command, setting labelToClick to: " + labelToClick);
        } else if (lowerCmd.equals("scroll down")) {
            labelToClick = "scroll";
        } else if (lowerCmd.equals("scroll up")) {
            labelToClick = "scroll_up";
        } else {
            for (String prefix : COMMANDS) {
                if (lowerCmd.startsWith(prefix + " ")) {
                    labelToClick = command.substring(prefix.length()).trim();
                    break;
                }
            }
        }

        if (labelToClick.isEmpty()) {
            android.util.Log.w("ClickLabelHandler", "No label to click found for command: " + command);
            FeedbackProvider.speakAndToast(context, "Sorry, I didn't catch what you want me to click.");
            return;
        }

        android.util.Log.d("ClickLabelHandler", "Sending broadcast with label: " + labelToClick);
        FeedbackProvider.speakAndToast(context, "Okay, " + lowerCmd);

        Intent intent = new Intent(ACTION_CLICK_LABEL);
        intent.putExtra(EXTRA_LABEL, labelToClick);
        context.sendBroadcast(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
                "click on [label]",
                "tap on [button name]",
                "like",
                "switch camera",
                "flip camera",
                "scroll down",
                "scroll up");
    }
} 