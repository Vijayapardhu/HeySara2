package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class TakePhotoHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String TAG = "TakePhotoHandler";
    
    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        Log.d(TAG, "canHandle called with command: '" + command + "' (lowercase: '" + lowerCmd + "')");
        
        boolean canHandle = lowerCmd.contains("take a photo") || 
               lowerCmd.contains("take a picture") ||
               lowerCmd.contains("take photo") ||
               lowerCmd.contains("take picture") ||
               lowerCmd.contains("click photo") ||
               lowerCmd.contains("click picture") ||
               lowerCmd.contains("capture photo") ||
               lowerCmd.contains("capture picture") ||
               lowerCmd.contains("snap photo") ||
               lowerCmd.contains("snap picture") ||
               lowerCmd.contains("open camera") ||
               lowerCmd.contains("launch camera") ||
               lowerCmd.contains("start camera");
               
        Log.d(TAG, "canHandle result: " + canHandle);
        return canHandle;
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        Log.d(TAG, "handle called with command: '" + command + "'");
        
        FeedbackProvider.speakAndToast(context, "Opening camera to take a photo.");
        
        try {
            // Send intent to accessibility service to handle camera and auto-capture
            Intent intent = new Intent("com.mvp.sarah.ACTION_TAKE_PHOTO_AUTO");
            context.sendBroadcast(intent);
            
            Log.d(TAG, "Sent auto-capture photo intent");
            
        } catch (Exception e) {
            Log.e(TAG, "Error sending auto-capture intent: " + e.getMessage());
            FeedbackProvider.speakAndToast(context, "Error taking photo: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "take a photo", 
            "take a picture",
            "take photo",
            "take picture",
            "click photo",
            "click picture",
            "capture photo",
            "capture picture",
            "snap photo",
            "snap picture",
            "open camera",
            "launch camera",
            "start camera"
        );
    }
} 