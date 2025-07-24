package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import com.mvp.sarah.ClickAccessibilityService;

import java.util.Arrays;
import java.util.List;

public class WifiHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "turn on wifi",
            "turn off wifi",
            "enable wi-fi",
            "disable wi-fi",
            "turn on mobile data",
            "turn off mobile data",
            "enable mobile data",
            "disable mobile data",
            "turn on hotspot",
            "turn off hotspot",
            "enable hotspot",
            "disable hotspot"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        
        // Only handle toggle commands, not "open wifi settings"
        if (lowerCmd.startsWith("open ")) {
            return false;
        }
        
        return (lowerCmd.contains("turn on") || lowerCmd.contains("turn off") || 
                lowerCmd.contains("enable") || lowerCmd.contains("disable")) &&
               (lowerCmd.contains("wifi") || lowerCmd.contains("wi-fi") || 
                lowerCmd.contains("mobile data") || lowerCmd.contains("hotspot"));
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase();
        boolean isWifi = lowerCmd.contains("wifi") || lowerCmd.contains("wi-fi");
        boolean isMobileData = lowerCmd.contains("mobile data");
        boolean isHotspot = lowerCmd.contains("hotspot");
        
        // Use the advanced accessibility service methods for all toggles
        if (isWifi) {
            // Send broadcast to trigger advanced WiFi toggle
            Intent wifiIntent = new Intent("com.mvp.sarah.ACTION_TOGGLE_WIFI_ADVANCED");
            context.sendBroadcast(wifiIntent);
        } else if (isMobileData) {
            // Send broadcast to trigger advanced mobile data toggle
            Intent mobileDataIntent = new Intent("com.mvp.sarah.ACTION_TOGGLE_MOBILE_DATA_ADVANCED");
            context.sendBroadcast(mobileDataIntent);
        } else if (isHotspot) {
            // Send broadcast to trigger advanced hotspot toggle
            Intent hotspotIntent = new Intent("com.mvp.sarah.ACTION_TOGGLE_HOTSPOT_ADVANCED");
            context.sendBroadcast(hotspotIntent);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
}
