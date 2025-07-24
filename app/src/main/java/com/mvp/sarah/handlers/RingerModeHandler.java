package com.mvp.sarah.handlers;

import android.content.Context;
import android.media.AudioManager;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class RingerModeHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "mute my phone",
            "set phone to silent",
            "vibrate my phone",
            "set phone to vibrate",
            "unmute my phone",
            "set phone to ring"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("mute") || command.contains("silent") || command.contains("vibrate") || command.contains("ring");
    }

    @Override
    public void handle(Context context, String command) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);

        if (command.contains("mute") || command.contains("silent")) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
            FeedbackProvider.speakAndToast(context, "Phone is now silent.");
        } else if (command.contains("vibrate")) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_VIBRATE);
            FeedbackProvider.speakAndToast(context, "Phone is now on vibrate.");
        } else if (command.contains("unmute") || command.contains("ring")) {
            audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
            FeedbackProvider.speakAndToast(context, "Phone is now set to ring.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 