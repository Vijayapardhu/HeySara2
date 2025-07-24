package com.mvp.sarah.handlers;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.DeviceAdmin;
import com.mvp.sarah.FeedbackProvider;
import com.mvp.sarah.SaraDeviceAdminReceiver;
import java.util.Arrays;
import java.util.List;

public class LockScreenHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "lock phone",
        "lock the phone",
        "screen off",
        "turn off screen"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("lock phone") ||
               lower.contains("lock the phone") ||
               lower.contains("screen off") ||
               lower.contains("turn off screen");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase();
        if (lower.contains("lock phone") || lower.contains("lock the phone")) {
            // Secure lock
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(context, SaraDeviceAdminReceiver.class);
            if (dpm.isAdminActive(admin)) {
                dpm.lockNow();
                FeedbackProvider.speakAndToast(context, "Locking phone and turning off screen.");
            } else {
                FeedbackProvider.speakAndToast(context, "Please enable device admin for Sara to lock your phone.");
                Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
                intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, admin);
                intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Sara needs device admin to lock your phone.");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
            }
        } else if (lower.contains("screen off") || lower.contains("turn off screen")) {
            // Non-secure screen off via accessibility
            Intent intent = new Intent("com.mvp.sarah.ACTION_SCREEN_OFF");
            context.sendBroadcast(intent);
            FeedbackProvider.speakAndToast(context, "Turning off screen.");
            // Fallback: try to simulate power button press (will only work on rooted devices)
            try {
                Runtime.getRuntime().exec(new String[]{"input", "keyevent", "26"});
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 