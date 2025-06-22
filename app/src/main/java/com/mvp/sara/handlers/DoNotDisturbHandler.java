package com.mvp.sara.handlers;

import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.widget.Toast;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class DoNotDisturbHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "turn on do not disturb",
            "turn off do not disturb",
            "enable dnd",
            "disable dnd"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("do not disturb") || command.contains("dnd");
    }

    @Override
    public void handle(Context context, String command) {
        NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !nm.isNotificationPolicyAccessGranted()) {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
            String appName = context.getApplicationInfo().loadLabel(context.getPackageManager()).toString();
            FeedbackProvider.speakAndToast(context, "Grant Do Not Disturb access to '" + appName + "' and try again", Toast.LENGTH_LONG);
            return;
        }
        boolean enable = command.contains("on") || command.contains("enable");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (enable) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY);
            } else {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL);
            }
            FeedbackProvider.speakAndToast(context, "Do Not Disturb " + (enable ? "enabled" : "disabled"));
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 