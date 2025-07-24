// DeviceInfoHandler.java
package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.BatteryManager;
import android.os.Environment;
import android.os.StatFs;
import android.os.SystemClock;
import android.net.wifi.WifiManager;
import android.text.format.Formatter;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.io.File;
import java.util.Arrays;
import java.util.List;

public class DeviceInfoHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "what's my battery level",
            "how much battery is left",
            "how much storage is free",
            "what's my storage space",
            "what's my ip address",
            "what's the device uptime"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        
        // Only handle device info queries, not battery status
        if (lowerCmd.contains("battery level") || lowerCmd.contains("battery status") || 
            lowerCmd.contains("how much battery")) {
            return false;
        }
        
        return lowerCmd.contains("storage") || lowerCmd.contains("ip address") || 
               lowerCmd.contains("uptime") || lowerCmd.contains("device info");
    }

    @Override
    public void handle(Context context, String command) {
        if (command.contains("battery")) {
            getBatteryLevel(context);
        } else if (command.contains("storage")) {
            getStorageSpace(context);
        } else if (command.contains("ip address")) {
            getIpAddress(context);
        } else if (command.contains("uptime")) {
            getDeviceUptime(context);
        }
    }

    private void getBatteryLevel(Context context) {
        IntentFilter ifilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
        Intent batteryStatus = context.registerReceiver(null, ifilter);
        int level = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) : -1;
        int scale = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_SCALE, -1) : -1;
        float batteryPct = level * 100 / (float) scale;
        FeedbackProvider.speakAndToast(context, "Your battery is at " + (int) batteryPct + " percent.");
    }

    private void getDeviceUptime(Context context) {
        long uptimeMillis = SystemClock.elapsedRealtime();
        long hours = uptimeMillis / (3600 * 1000);
        long minutes = (uptimeMillis % (3600 * 1000)) / (60 * 1000);
        FeedbackProvider.speakAndToast(context, "The device has been running for " + hours + " hours and " + minutes + " minutes.");
    }

    private void getIpAddress(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
        String ipAddress = Formatter.formatIpAddress(wifiManager.getConnectionInfo().getIpAddress());
        if (ipAddress.equals("0.0.0.0")) {
            FeedbackProvider.speakAndToast(context, "You are not connected to a Wi-Fi network.");
        } else {
            FeedbackProvider.speakAndToast(context, "Your IP address is " + ipAddress);
        }
    }

    private void getStorageSpace(Context context) {
        File path = Environment.getDataDirectory();
        StatFs stat = new StatFs(path.getPath());
        long blockSize = stat.getBlockSizeLong();
        long availableBlocks = stat.getAvailableBlocksLong();
        long freeSpaceGB = (availableBlocks * blockSize) / (1024 * 1024 * 1024);
        FeedbackProvider.speakAndToast(context, "You have " + freeSpaceGB + " gigabytes of free storage.");
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 