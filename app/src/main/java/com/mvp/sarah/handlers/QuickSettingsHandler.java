package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class QuickSettingsHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    
    private static final String TAG = "QuickSettingsHandler";
    
    private static final List<String> COMMANDS = Arrays.asList(
        "toggle wifi",
        "turn on wifi",
        "turn off wifi",
        "toggle bluetooth",
        "turn on bluetooth", 
        "turn off bluetooth",
        "toggle mobile data",
        "turn on mobile data",
        "turn off mobile data",
        "toggle internet",
        "turn on internet",
        "turn off internet",
        "toggle hotspot",
        "turn on hotspot",
        "turn off hotspot",
        "toggle airplane mode",
        "turn on airplane mode",
        "turn off airplane mode",
        "toggle do not disturb",
        "turn on do not disturb",
        "turn off do not disturb",
        "toggle auto rotate",
        "turn on auto rotate",
        "turn off auto rotate",
        "toggle night mode",
        "turn on night mode",
        "turn off night mode",
        "toggle battery saver",
        "turn on battery saver",
        "turn off battery saver",
        "toggle location",
        "turn on location",
        "turn off location",
        "toggle flashlight",
        "turn on flashlight",
        "turn off flashlight",
        "toggle torch",
        "turn on torch",
        "turn off torch",
        "take screenshot",
        "capture screen",
        "open quick settings",
        "show quick settings",
        "open notification panel",
        "show notification panel",
        "toggle silent mode",
        "turn on silent mode",
        "turn off silent mode",
        "toggle vibration",
        "turn on vibration",
        "turn off vibration",
        "toggle cast",
        "turn on cast",
        "turn off cast",
        "toggle mobile hotspot",
        "turn on mobile hotspot",
        "turn off mobile hotspot",
        "toggle reading mode",
        "turn on reading mode",
        "turn off reading mode",
        "toggle gaming mode",
        "turn on gaming mode",
        "turn off gaming mode",
        "toggle performance mode",
        "turn on performance mode",
        "turn off performance mode",
        "toggle power saving",
        "turn on power saving",
        "turn off power saving",
        "toggle ultra power saving",
        "turn on ultra power saving",
        "turn off ultra power saving"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        
        // Handle quick settings related commands
        return lowerCmd.contains("quick settings") ||
               lowerCmd.contains("notification panel") ||
               lowerCmd.contains("toggle") ||
               (lowerCmd.contains("turn") && (lowerCmd.contains("on") || lowerCmd.contains("off"))) ||
               lowerCmd.contains("screenshot") ||
               lowerCmd.contains("screen capture") ||
               lowerCmd.contains("flashlight") ||
               lowerCmd.contains("torch") ||
               lowerCmd.contains("airplane mode") ||
               lowerCmd.contains("do not disturb") ||
               lowerCmd.contains("dnd") ||
               lowerCmd.contains("auto rotate") ||
               lowerCmd.contains("night mode") ||
               lowerCmd.contains("dark mode") ||
               lowerCmd.contains("battery saver") ||
               lowerCmd.contains("power saving") ||
               lowerCmd.contains("ultra power saving") ||
               lowerCmd.contains("location services") ||
               lowerCmd.contains("gps") ||
               lowerCmd.contains("silent mode") ||
               lowerCmd.contains("vibration") ||
               lowerCmd.contains("cast") ||
               lowerCmd.contains("mobile hotspot") ||
               lowerCmd.contains("reading mode") ||
               lowerCmd.contains("gaming mode") ||
               lowerCmd.contains("performance mode") ||
               lowerCmd.contains("internet") ||
               lowerCmd.contains("mobile internet");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase();
        Log.d(TAG, "Handling quick settings command: " + command);
        
        // Handle opening quick settings panel
        if (lowerCmd.contains("open quick settings") || 
            lowerCmd.contains("show quick settings") ||
            lowerCmd.contains("open notification panel") ||
            lowerCmd.contains("show notification panel")) {
            openQuickSettings(context);
            return;
        }
        
        // Handle screenshot
        if (lowerCmd.contains("screenshot") || lowerCmd.contains("screen capture")) {
            takeScreenshot(context);
            return;
        }
        
        // Handle flashlight/torch
        if (lowerCmd.contains("flashlight") || lowerCmd.contains("torch")) {
            handleFlashlight(context, lowerCmd);
            return;
        }
        
        // Handle airplane mode
        if (lowerCmd.contains("airplane mode")) {
            handleAirplaneMode(context, lowerCmd);
            return;
        }
        
        // Handle do not disturb
        if (lowerCmd.contains("do not disturb") || lowerCmd.contains("dnd")) {
            handleDoNotDisturb(context, lowerCmd);
            return;
        }
        
        // Handle auto rotate
        if (lowerCmd.contains("auto rotate")) {
            handleAutoRotate(context, lowerCmd);
            return;
        }
        
        // Handle night mode/dark mode
        if (lowerCmd.contains("night mode") || lowerCmd.contains("dark mode")) {
            handleNightMode(context, lowerCmd);
            return;
        }
        
        // Handle battery saver
        if (lowerCmd.contains("battery saver") || lowerCmd.contains("power saving")) {
            handleBatterySaver(context, lowerCmd);
            return;
        }
        
        // Handle location services
        if (lowerCmd.contains("location") || lowerCmd.contains("gps")) {
            handleLocationServices(context, lowerCmd);
            return;
        }
        
        // Handle silent mode
        if (lowerCmd.contains("silent mode")) {
            handleSilentMode(context, lowerCmd);
            return;
        }
        
        // Handle vibration
        if (lowerCmd.contains("vibration")) {
            handleVibration(context, lowerCmd);
            return;
        }
        
        // Handle cast
        if (lowerCmd.contains("cast")) {
            handleCast(context, lowerCmd);
            return;
        }
        
        // Handle mobile hotspot
        if (lowerCmd.contains("mobile hotspot")) {
            handleMobileHotspot(context, lowerCmd);
            return;
        }
        
        // Handle reading mode
        if (lowerCmd.contains("reading mode")) {
            handleReadingMode(context, lowerCmd);
            return;
        }
        
        // Handle gaming mode
        if (lowerCmd.contains("gaming mode")) {
            handleGamingMode(context, lowerCmd);
            return;
        }
        
        // Handle performance mode
        if (lowerCmd.contains("performance mode")) {
            handlePerformanceMode(context, lowerCmd);
            return;
        }
        
        // Handle ultra power saving
        if (lowerCmd.contains("ultra power saving")) {
            handleUltraPowerSaving(context, lowerCmd);
            return;
        }
        
        // Handle WiFi, Bluetooth, Mobile Data, Hotspot (these are handled by existing handlers)
        if (lowerCmd.contains("wifi") || lowerCmd.contains("bluetooth") || 
            lowerCmd.contains("hotspot")) {
            FeedbackProvider.speakAndToast(context, "Please use the specific commands for WiFi, Bluetooth, or Hotspot.");
            return;
        }
        
        // Handle internet/mobile data commands
        if (lowerCmd.contains("internet") || lowerCmd.contains("mobile internet") || lowerCmd.contains("mobile data")) {
            handleMobileData(context, lowerCmd);
            return;
        }
        
        // Default: try to trigger as a generic quick settings tile
        handleGenericQuickSettingsTile(context, command);
    }
    
    private void openQuickSettings(Context context) {
        Log.d(TAG, "Opening quick settings panel");
        // Use the global action to open quick settings
        Intent intent = new Intent("com.mvp.sarah.ACTION_TRIGGER_QUICK_TILE");
        intent.putExtra("tile_keyword", "open");
        context.sendBroadcast(intent);
        FeedbackProvider.speakAndToast(context, "Opening quick settings");
    }
    
    private void takeScreenshot(Context context) {
        Log.d(TAG, "Taking screenshot");
        Intent intent = new Intent("com.mvp.sarah.ACTION_TAKE_SCREENSHOT");
        context.sendBroadcast(intent);
        FeedbackProvider.speakAndToast(context, "Taking screenshot");
    }
    
    private void handleFlashlight(Context context, String command) {
        Log.d(TAG, "Handling flashlight command: " + command);
        String tileKeyword = "Flashlight";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Flashlight " + action);
    }
    
    private void handleAirplaneMode(Context context, String command) {
        Log.d(TAG, "Handling airplane mode command: " + command);
        String tileKeyword = "Airplane mode";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Airplane mode " + action);
    }
    
    private void handleDoNotDisturb(Context context, String command) {
        Log.d(TAG, "Handling do not disturb command: " + command);
        String tileKeyword = "Do not disturb";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Do not disturb " + action);
    }
    
    private void handleAutoRotate(Context context, String command) {
        Log.d(TAG, "Handling auto rotate command: " + command);
        String tileKeyword = "Auto-rotate";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Auto-rotate " + action);
    }
    
    private void handleNightMode(Context context, String command) {
        Log.d(TAG, "Handling night mode command: " + command);
        String tileKeyword = "Night mode";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Night mode " + action);
    }
    
    private void handleBatterySaver(Context context, String command) {
        Log.d(TAG, "Handling battery saver command: " + command);
        String tileKeyword = "Battery saver";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Battery saver " + action);
    }
    
    private void handleLocationServices(Context context, String command) {
        Log.d(TAG, "Handling location services command: " + command);
        String tileKeyword = "Location";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Location services " + action);
    }
    
    private void handleSilentMode(Context context, String command) {
        Log.d(TAG, "Handling silent mode command: " + command);
        String tileKeyword = "Silent mode";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Silent mode " + action);
    }
    
    private void handleVibration(Context context, String command) {
        Log.d(TAG, "Handling vibration command: " + command);
        String tileKeyword = "Vibration";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Vibration " + action);
    }
    
    private void handleCast(Context context, String command) {
        Log.d(TAG, "Handling cast command: " + command);
        String tileKeyword = "Cast";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Cast " + action);
    }
    
    private void handleMobileHotspot(Context context, String command) {
        Log.d(TAG, "Handling mobile hotspot command: " + command);
        String tileKeyword = "Mobile hotspot";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Mobile hotspot " + action);
    }
    
    private void handleReadingMode(Context context, String command) {
        Log.d(TAG, "Handling reading mode command: " + command);
        String tileKeyword = "Reading mode";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Reading mode " + action);
    }
    
    private void handleGamingMode(Context context, String command) {
        Log.d(TAG, "Handling gaming mode command: " + command);
        String tileKeyword = "Gaming mode";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Gaming mode " + action);
    }
    
    private void handlePerformanceMode(Context context, String command) {
        Log.d(TAG, "Handling performance mode command: " + command);
        String tileKeyword = "Performance mode";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Performance mode " + action);
    }
    
    private void handleUltraPowerSaving(Context context, String command) {
        Log.d(TAG, "Handling ultra power saving command: " + command);
        String tileKeyword = "Ultra power saving";
        triggerQuickSettingsTile(context, tileKeyword);
        String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
        FeedbackProvider.speakAndToast(context, "Ultra power saving " + action);
    }
    
    private void handleMobileData(Context context, String command) {
        Log.d(TAG, "Handling mobile data/internet command: " + command);
        // For internet commands, be very specific to avoid WiFi confusion
        String tileKeyword = "Internet"; // Use exact keyword that works on your device
        boolean shouldTurnOn = !command.contains("turn off") && !command.contains("off");
        triggerQuickSettingsTileWithState(context, tileKeyword, shouldTurnOn);
        String action = shouldTurnOn ? "turned on" : "turned off";
        FeedbackProvider.speakAndToast(context, "Mobile data " + action);
    }
    
    private void handleGenericQuickSettingsTile(Context context, String command) {
        Log.d(TAG, "Handling generic quick settings tile: " + command);
        // Extract the main keyword from the command
        String[] words = command.toLowerCase().split("\\s+");
        String tileKeyword = "";
        
        // Find the main feature keyword
        for (String word : words) {
            if (!word.equals("toggle") && !word.equals("turn") && 
                !word.equals("on") && !word.equals("off") && 
                !word.equals("mode") && !word.equals("the")) {
                tileKeyword = word.substring(0, 1).toUpperCase() + word.substring(1);
                break;
            }
        }
        
        if (!tileKeyword.isEmpty()) {
            Log.d(TAG, "Triggering quick settings tile: " + tileKeyword);
            triggerQuickSettingsTile(context, tileKeyword);
            String action = command.contains("turn off") || command.contains("off") ? "turned off" : "turned on";
            FeedbackProvider.speakAndToast(context, tileKeyword + " " + action);
        } else {
            Log.w(TAG, "Could not extract tile keyword from command: " + command);
            FeedbackProvider.speakAndToast(context, "I couldn't understand which quick setting you want to toggle.");
        }
    }
    
    private void triggerQuickSettingsTile(Context context, String tileKeyword) {
        Log.d(TAG, "Triggering quick settings tile: " + tileKeyword);
        Intent intent = new Intent("com.mvp.sarah.ACTION_TRIGGER_QUICK_TILE");
        intent.putExtra("tile_keyword", tileKeyword);
        context.sendBroadcast(intent);
    }
    
    private void triggerQuickSettingsTileWithState(Context context, String tileKeyword, boolean shouldTurnOn) {
        Log.d(TAG, "Triggering quick settings tile with state: " + tileKeyword + " (should turn " + (shouldTurnOn ? "ON" : "OFF") + ")");
        Intent intent = new Intent("com.mvp.sarah.ACTION_TRIGGER_QUICK_TILE_WITH_STATE");
        intent.putExtra("tile_keyword", tileKeyword);
        intent.putExtra("should_turn_on", shouldTurnOn);
        context.sendBroadcast(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 