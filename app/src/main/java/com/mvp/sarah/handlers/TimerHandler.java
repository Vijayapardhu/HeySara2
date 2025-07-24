package com.mvp.sarah.handlers;

import android.content.Context;
import android.media.MediaPlayer;
import android.os.CountDownTimer;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class TimerHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "set a timer",
            "timer for"
    );
    private static CountDownTimer activeTimer = null;

    @Override
    public boolean canHandle(String command) {
        return command.contains("timer");
    }

    @Override
    public void handle(Context context, String command) {
        if (activeTimer != null) {
            FeedbackProvider.speakAndToast(context, "A timer is already running.");
            return;
        }
        long millis = parseDuration(command);
        if (millis <= 0) {
            FeedbackProvider.speakAndToast(context, "Please specify a valid timer duration, like 'timer for 10 minutes'.");
            return;
        }
        FeedbackProvider.speakAndToast(context, "Timer set for " + formatDuration(millis) + ".");
        activeTimer = new CountDownTimer(millis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {}
            @Override
            public void onFinish() {
                FeedbackProvider.speakAndToast(context, "Time's up!");
                try {
                    MediaPlayer mp = MediaPlayer.create(context, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
                    if (mp != null) mp.start();
                } catch (Exception ignored) {}
                activeTimer = null;
            }
        };
        activeTimer.start();
    }

    private long parseDuration(String command) {
        Pattern p = Pattern.compile("(\\d+)\\s*(second|minute|hour|sec|min|hr)s?", Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(command);
        long millis = 0;
        while (m.find()) {
            int value = Integer.parseInt(m.group(1));
            String unit = m.group(2).toLowerCase();
            if (unit.startsWith("sec")) millis += value * 1000L;
            else if (unit.startsWith("min")) millis += value * 60_000L;
            else if (unit.startsWith("hour") || unit.startsWith("hr")) millis += value * 3_600_000L;
        }
        return millis;
    }

    private String formatDuration(long millis) {
        long seconds = millis / 1000 % 60;
        long minutes = millis / (60 * 1000) % 60;
        long hours = millis / (60 * 60 * 1000);
        StringBuilder sb = new StringBuilder();
        if (hours > 0) sb.append(hours).append(" hour").append(hours > 1 ? "s " : " ");
        if (minutes > 0) sb.append(minutes).append(" minute").append(minutes > 1 ? "s " : " ");
        if (seconds > 0) sb.append(seconds).append(" second").append(seconds > 1 ? "s" : "");
        return sb.toString().trim();
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 