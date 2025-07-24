package com.mvp.sarah.handlers;

import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import com.mvp.sarah.ClickAccessibilityService;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import androidx.core.app.ActivityCompat;

import java.util.Arrays;
import java.util.List;

public class BluetoothHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "turn on bluetooth",
            "turn off bluetooth",
            "enable bluetooth",
            "disable bluetooth"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        
        // Only handle toggle commands, not "open bluetooth settings"
        if (lowerCmd.startsWith("open ")) {
            return false;
        }
        
        return (lowerCmd.contains("turn on") || lowerCmd.contains("turn off") || 
                lowerCmd.contains("enable") || lowerCmd.contains("disable")) &&
               lowerCmd.contains("bluetooth");
    }

    @Override
    public void handle(Context context, String command) {
        // Use the advanced accessibility service method for Bluetooth toggle
        FeedbackProvider.speakAndToast(context, "Toggling Bluetooth using advanced automation.");
        // Send broadcast to trigger advanced Bluetooth toggle
        Intent bluetoothIntent = new Intent("com.mvp.sarah.ACTION_TOGGLE_BLUETOOTH_ADVANCED");
        context.sendBroadcast(bluetoothIntent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 