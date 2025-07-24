package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class StopwatchHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "start stopwatch",
            "stop stopwatch",
            "reset stopwatch"
    );
    
    private boolean isRunning = false;
    private long startTime = 0;
    private long elapsedTime = 0;
    public static final String ACTION_STOPWATCH_UPDATE = "com.mvp.sarah.ACTION_STOPWATCH_UPDATE";
    private android.os.Handler handler = new android.os.Handler();
    private Runnable updateRunnable = null;

    @Override
    public boolean canHandle(String command) {
        return command.contains("stopwatch");
    }

    @Override
    public void handle(Context context, String command) {
        if (command.contains("start")) {
            if (isRunning) {
                FeedbackProvider.speakAndToast(context, "The stopwatch is already running.");
            } else {
                isRunning = true;
                startTime = SystemClock.elapsedRealtime() - elapsedTime;
                FeedbackProvider.speakAndToast(context, "Stopwatch started.");
                startLiveUpdates(context);
            }
        } else if (command.contains("stop")) {
            if (!isRunning) {
                FeedbackProvider.speakAndToast(context, "The stopwatch is not running.");
            } else {
                isRunning = false;
                elapsedTime = SystemClock.elapsedRealtime() - startTime;
                FeedbackProvider.speakAndToast(context, "Stopwatch stopped at " + formatElapsedTime(elapsedTime));
                stopLiveUpdates();
            }
        } else if (command.contains("reset")) {
            isRunning = false;
            startTime = 0;
            elapsedTime = 0;
            FeedbackProvider.speakAndToast(context, "Stopwatch reset.");
            stopLiveUpdates();
            sendStopwatchUpdate(context, "00:00:00");
        }
    }

    private String formatElapsedTime(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (60 * 1000)) % 60;
        long hours = (millis / (60 * 60 * 1000));
        return String.format(Locale.getDefault(), "%02d:%02d:%02d", hours, minutes, seconds);
    }

    private void startLiveUpdates(Context context) {
        stopLiveUpdates();
        updateRunnable = new Runnable() {
            @Override
            public void run() {
                if (isRunning) {
                    long now = SystemClock.elapsedRealtime();
                    long elapsed = now - startTime;
                    String time = formatElapsedTime(elapsed);
                    sendStopwatchUpdate(context, time);
                    handler.postDelayed(this, 1000);
                }
            }
        };
        handler.post(updateRunnable);
    }

    private void stopLiveUpdates() {
        if (updateRunnable != null) {
            handler.removeCallbacks(updateRunnable);
            updateRunnable = null;
        }
    }

    private void sendStopwatchUpdate(Context context, String time) {
        Intent intent = new Intent(ACTION_STOPWATCH_UPDATE);
        intent.putExtra("time", time);
        context.sendBroadcast(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 