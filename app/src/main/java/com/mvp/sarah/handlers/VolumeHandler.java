package com.mvp.sarah.handlers;

import android.content.Context;
import android.media.AudioManager;
import android.widget.Toast;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class VolumeHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "set volume to [0-100] percent",
            "increase volume",
            "decrease volume",
            "mute volume",
            "unmute volume",
            "max volume"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("volume");
    }

    @Override
    public void handle(Context context, String command) {
        AudioManager audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        int max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int current = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        int oneStep = max / 15; // Approx 7% step

        if (command.startsWith("set volume to ")) {
            try {
                String volumeStr = command.replaceAll("\\D+", "");
                int volume = Integer.parseInt(volumeStr);
                int setVol = Math.max(0, Math.min(volume * max / 100, max));
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, setVol, AudioManager.FLAG_SHOW_UI);
                FeedbackProvider.speakAndToast(context, "Volume set to " + volume + "%");
            } catch (Exception e) {
                FeedbackProvider.speakAndToast(context, "Invalid volume value. Please say a number between 0 and 100.");
            }
        } else if (command.contains("increase")) {
            int newVol = Math.min(current + oneStep, max);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI);
            FeedbackProvider.speakAndToast(context, "Volume increased");
        } else if (command.contains("decrease")) {
            int newVol = Math.max(current - oneStep, 0);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVol, AudioManager.FLAG_SHOW_UI);
            FeedbackProvider.speakAndToast(context, "Volume decreased");
        } else if (command.contains("mute")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, AudioManager.FLAG_SHOW_UI);
            FeedbackProvider.speakAndToast(context, "Volume muted");
        } else if (command.contains("unmute")) {
            int targetVol = max / 2; // Unmute to 50%
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, Math.max(current, targetVol), AudioManager.FLAG_SHOW_UI);
            FeedbackProvider.speakAndToast(context, "Volume unmuted");
        } else if (command.contains("max")) {
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, max, AudioManager.FLAG_SHOW_UI);
            FeedbackProvider.speakAndToast(context, "Volume set to max");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 