package com.mvp.sara.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class SaraSettingsHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String PREFS_NAME = "SaraSettings";
    private static final String KEY_INTERRUPT_MEDIA = "interrupt_media";
    
    @Override
    public boolean canHandle(String command) {
        return command.contains("sara settings") || 
               command.contains("voice settings") || 
               command.contains("interrupt media") ||
               command.contains("don't interrupt media") ||
               command.contains("allow media interruption") ||
               command.contains("disable media interruption");
    }

    @Override
    public void handle(Context context, String command) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        if (command.contains("interrupt media") || command.contains("allow media interruption")) {
            prefs.edit().putBoolean(KEY_INTERRUPT_MEDIA, true).apply();
            FeedbackProvider.speakAndToast(context, "Sara will now interrupt media playback when listening.");
        } else if (command.contains("don't interrupt media") || command.contains("disable media interruption")) {
            prefs.edit().putBoolean(KEY_INTERRUPT_MEDIA, false).apply();
            FeedbackProvider.speakAndToast(context, "Sara will not interrupt media playback. Say 'Hey Sara' when media is paused.");
        } else if (command.contains("sara settings") || command.contains("voice settings")) {
            boolean interruptMedia = prefs.getBoolean(KEY_INTERRUPT_MEDIA, false);
            String status = interruptMedia ? "enabled" : "disabled";
            FeedbackProvider.speakAndToast(context, "Media interruption is currently " + status + ". Say 'allow media interruption' or 'disable media interruption' to change.");
        }
    }
    
    public static boolean shouldInterruptMedia(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_INTERRUPT_MEDIA, false);
    }
    
    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "sara settings", 
            "voice settings", 
            "allow media interruption",
            "disable media interruption",
            "don't interrupt media"
        );
    }
} 