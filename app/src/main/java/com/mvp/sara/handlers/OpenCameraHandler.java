package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.util.Log;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class OpenCameraHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String TAG = "OpenCameraHandler";
    private static String currentCameraId = "0"; // Default to back camera
    
    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        return lowerCmd.contains("camera") || 
               lowerCmd.contains("switch camera") || 
               lowerCmd.contains("front camera") || 
               lowerCmd.contains("back camera");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        
        if (lowerCmd.contains("switch camera") || lowerCmd.contains("front camera") || lowerCmd.contains("back camera")) {
            switchCamera(context);
        } else if (lowerCmd.contains("camera")) {
            openCamera(context);
        }
    }
    
    private void openCamera(Context context) {
        Log.d(TAG, "openCamera called");
        FeedbackProvider.speakAndToast(context, "Opening camera.");
        
        try {
            // Try multiple camera intents to ensure compatibility
            Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            
            Log.d(TAG, "Starting camera activity with intent: " + intent.getAction());
            context.startActivity(intent);
            Log.d(TAG, "Camera activity started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
            // Fallback to default camera app
            try {
                Intent fallbackIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallbackIntent);
                Log.d(TAG, "Fallback camera activity started");
            } catch (Exception fallbackError) {
                Log.e(TAG, "Fallback camera also failed: " + fallbackError.getMessage());
                FeedbackProvider.speakAndToast(context, "Could not open camera app.");
            }
        }
    }
    
    private void switchCamera(Context context) {
        Log.d(TAG, "switchCamera called");
        try {
            CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
            String[] cameraIds = cameraManager.getCameraIdList();
            
            Log.d(TAG, "Available camera IDs: " + java.util.Arrays.toString(cameraIds));
            Log.d(TAG, "Current camera ID: " + currentCameraId);
            
            if (cameraIds.length < 2) {
                Log.w(TAG, "Only one camera available, cannot switch");
                FeedbackProvider.speakAndToast(context, "Only one camera is available on this device.");
                return;
            }
            
            // Simple approach: if we have 2 cameras, switch between them
            String newCameraId = currentCameraId.equals("0") ? "1" : "0";
            
            // Verify the new camera exists
            boolean cameraExists = false;
            for (String cameraId : cameraIds) {
                if (cameraId.equals(newCameraId)) {
                    cameraExists = true;
                    break;
                }
            }
            
            if (cameraExists) {
                currentCameraId = newCameraId;
                String cameraType = currentCameraId.equals("1") ? "front" : "back";
                Log.d(TAG, "Successfully switched to " + cameraType + " camera (ID: " + currentCameraId + ")");
                FeedbackProvider.speakAndToast(context, "Switched to " + cameraType + " camera.");
                
                // Open camera with the new camera ID
                try {
                    Intent intent = new Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    intent.putExtra("android.intent.extras.CAMERA_FACING", 
                        currentCameraId.equals("1") ? 1 : 0); // 1 for front, 0 for back
                    
                    Log.d(TAG, "Starting camera activity with facing: " + (currentCameraId.equals("1") ? "front" : "back"));
                    context.startActivity(intent);
                    Log.d(TAG, "Camera activity started successfully for " + cameraType + " camera");
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error starting camera activity: " + e.getMessage());
                    // Fallback to default camera app
                    try {
                        Intent fallbackIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        context.startActivity(fallbackIntent);
                        Log.d(TAG, "Fallback camera activity started");
                    } catch (Exception fallbackError) {
                        Log.e(TAG, "Fallback camera also failed: " + fallbackError.getMessage());
                        FeedbackProvider.speakAndToast(context, "Could not open camera app.");
                    }
                }
            } else {
                Log.w(TAG, "Camera ID " + newCameraId + " does not exist");
                FeedbackProvider.speakAndToast(context, "Camera not available.");
            }
            
        } catch (CameraAccessException e) {
            Log.e(TAG, "Camera access error: " + e.getMessage());
            FeedbackProvider.speakAndToast(context, "Camera access error: " + e.getMessage());
        } catch (Exception e) {
            Log.e(TAG, "Unexpected error in switchCamera: " + e.getMessage());
            FeedbackProvider.speakAndToast(context, "Error switching camera: " + e.getMessage());
        }
    }
    
    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "open camera", 
            "launch camera",
            "switch camera",
            "front camera",
            "back camera"
        );
    }
} 