package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.util.Log;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
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
               lowerCmd.contains("snap picture");
               
        Log.d(TAG, "canHandle result: " + canHandle);
        return canHandle;
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        Log.d(TAG, "handle called with command: '" + command + "'");
        
        FeedbackProvider.speakAndToast(context, "Opening camera to take a photo.");
        
        try {
            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            Log.d(TAG, "Starting camera activity with intent: " + intent.getAction());
            context.startActivity(intent);
            Log.d(TAG, "Camera activity started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error starting camera activity: " + e.getMessage());
            FeedbackProvider.speakAndToast(context, "Error opening camera: " + e.getMessage());
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
            "snap picture"
        );
    }
} 