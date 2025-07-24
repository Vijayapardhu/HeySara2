package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class QuickSettingsDebugHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    
    private static final String TAG = "QuickSettingsDebug";
    
    private static final List<String> COMMANDS = Arrays.asList(
        "debug quick settings",
        "analyze quick settings",
        "show quick settings info",
        "test quick settings",
        "list quick settings tiles"
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        return lowerCmd.contains("debug") || 
               lowerCmd.contains("analyze") ||
               lowerCmd.contains("test") ||
               lowerCmd.contains("list") ||
               lowerCmd.contains("show");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase();
        Log.d(TAG, "Handling debug command: " + command);
        
        if (lowerCmd.contains("debug quick settings") || 
            lowerCmd.contains("analyze quick settings") ||
            lowerCmd.contains("test quick settings")) {
            debugQuickSettings(context);
            return;
        }
        
        if (lowerCmd.contains("list quick settings tiles") ||
            lowerCmd.contains("show quick settings info")) {
            listQuickSettingsTiles(context);
            return;
        }
        
        FeedbackProvider.speakAndToast(context, "Debug command not recognized");
    }
    
    private void debugQuickSettings(Context context) {
        Log.d(TAG, "Starting quick settings debug analysis");
        FeedbackProvider.speakAndToast(context, "Starting quick settings debug analysis");
        
        // Send debug intent to accessibility service
        Intent intent = new Intent("com.mvp.sarah.ACTION_DEBUG_QUICK_SETTINGS");
        context.sendBroadcast(intent);
    }
    
    private void listQuickSettingsTiles(Context context) {
        Log.d(TAG, "Listing quick settings tiles");
        FeedbackProvider.speakAndToast(context, "Analyzing available quick settings tiles");
        
        // Send list intent to accessibility service
        Intent intent = new Intent("com.mvp.sarah.ACTION_LIST_QUICK_SETTINGS");
        context.sendBroadcast(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 
 
 
 