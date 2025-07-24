package com.mvp.sarah.handlers;

import android.content.Context;
import android.media.AudioManager;
import android.view.KeyEvent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class MusicControlHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "pause music",
            "resume music",
            "play music",
            "next song",
            "previous song"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("music") || command.contains("song");
    }

    @Override
    public void handle(Context context, String command) {
        int keyCode = -1;
        String feedback = "";

        if (command.contains("pause")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_PAUSE;
            feedback = "Pausing.";
        } else if (command.contains("resume") || command.contains("play")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_PLAY;
            feedback = "Resuming.";
        } else if (command.contains("next")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_NEXT;
            feedback = "Next song.";
        } else if (command.contains("previous")) {
            keyCode = KeyEvent.KEYCODE_MEDIA_PREVIOUS;
            feedback = "Previous song.";
        }

        if (keyCode != -1) {
            AudioManager am = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            KeyEvent downEvent = new KeyEvent(KeyEvent.ACTION_DOWN, keyCode);
            am.dispatchMediaKeyEvent(downEvent);
            KeyEvent upEvent = new KeyEvent(KeyEvent.ACTION_UP, keyCode);
            am.dispatchMediaKeyEvent(upEvent);
            FeedbackProvider.speakAndToast(context, feedback);
        } else {
            FeedbackProvider.speakAndToast(context, "I can play, pause, and skip songs. What would you like to do?");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 