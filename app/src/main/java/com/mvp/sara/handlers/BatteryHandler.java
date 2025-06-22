package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.widget.Toast;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class BatteryHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "what's my battery level",
            "battery status",
            "how much battery is left"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("battery");
    }

    @Override
    public void handle(Context context, String command) {
        IntentFilter filter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, filter);

        if (batteryStatus != null) {
            int level = batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            float batteryPct = level * 100 / (float) scale;
            FeedbackProvider.speakAndToast(context, "Battery is at " + (int) batteryPct + "%", Toast.LENGTH_LONG);
        } else {
            FeedbackProvider.speakAndToast(context, "Could not retrieve battery status");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 