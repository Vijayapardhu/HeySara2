package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class OpenCameraHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String TAG = "OpenCameraHandler";
    private static String currentCameraId = "0"; // Default to back camera
    private static int switchButtonX = -1;
    private static int switchButtonY = -1;
    private static boolean learningSwitchButton = false;
    
    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        Log.d(TAG, "OpenCameraHandler.canHandle called with: '" + command + "' (lowercase: '" + lowerCmd + "')");
        
        boolean canHandle = lowerCmd.contains("switch camera") || 
               lowerCmd.contains("change camera") ||
               lowerCmd.contains("front camera") || 
               lowerCmd.contains("back camera") ||
               lowerCmd.contains("learn switch button") ||
               lowerCmd.contains("take photo") ||
               lowerCmd.contains("take picture") ||
               lowerCmd.contains("click photo") ||
               lowerCmd.contains("click picture") ||
               lowerCmd.contains("capture photo") ||
               lowerCmd.contains("capture picture") ||
               // Add more specific patterns to ensure we catch camera-related commands
               (lowerCmd.contains("camera") && (lowerCmd.contains("switch") || lowerCmd.contains("change") || lowerCmd.contains("flip")));
        
        Log.d(TAG, "OpenCameraHandler.canHandle result: " + canHandle);
        return canHandle;
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        Log.d(TAG, "Handling camera command: " + command);
        
        if (lowerCmd.contains("learn switch button")) {
            learningSwitchButton = true;
            FeedbackProvider.speakAndToast(context, "Touch the switch camera button in the camera app now.");
            return;
        }
        
        if (lowerCmd.contains("take photo") || lowerCmd.contains("take picture") || 
            lowerCmd.contains("click photo") || lowerCmd.contains("click picture") ||
            lowerCmd.contains("capture photo") || lowerCmd.contains("capture picture")) {
            // Open camera and take photo automatically
            takePhoto(context);
        } else if (lowerCmd.contains("switch camera") || lowerCmd.contains("change camera") || 
            lowerCmd.contains("front camera") || lowerCmd.contains("back camera")) {
            // Use accessibility service to find and click camera switch button
            Intent switchIntent = new Intent("com.mvp.sarah.ACTION_SWITCH_CAMERA");
            context.sendBroadcast(switchIntent);
            // FeedbackProvider.speakAndToast(context, "Looking for camera switch button..."); // Removed as requested
        } else if (lowerCmd.contains("camera")) {
            openCamera(context);
        }
    }
    
    private void takePhoto(Context context) {
        Log.d(TAG, "takePhoto called");
        FeedbackProvider.speakAndToast(context, "Opening camera and taking photo.");
        
        try {
            // Open the default camera app directly
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage("com.motorola.camera3"); // Default Motorola camera
            
            context.startActivity(intent);
            
            // Wait for camera to load, then trigger photo capture
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                Intent captureIntent = new Intent("com.mvp.sarah.ACTION_TAKE_PHOTO_AUTO");
                context.sendBroadcast(captureIntent);
            }, 3000); // Wait 3 seconds for camera to load
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera app: " + e.getMessage());
            // Fallback to generic camera intent
            try {
                Intent fallbackIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(fallbackIntent);
                
                // Wait for camera to load, then trigger photo capture
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    Intent captureIntent = new Intent("com.mvp.sarah.ACTION_TAKE_PHOTO_AUTO");
                    context.sendBroadcast(captureIntent);
                }, 3000);
                
            } catch (Exception fallbackError) {
                Log.e(TAG, "Fallback camera also failed: " + fallbackError.getMessage());
                FeedbackProvider.speakAndToast(context, "Could not open camera app.");
            }
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
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            Log.d(TAG, "Camera activity started successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error opening camera: " + e.getMessage());
            // Fallback to default camera app
            try {
                Intent fallbackIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                    Log.d(TAG, "Camera activity started successfully for " + cameraType + " camera");
                    
                } catch (Exception e) {
                    Log.e(TAG, "Error starting camera activity: " + e.getMessage());
                    // Fallback to default camera app
                    try {
                        Intent fallbackIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                        fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
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
                // FeedbackProvider.speakAndToast(context, "Camera not available."); // Removed as requested
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
            "change camera",
            "front camera",
            "back camera",
            "take photo",
            "take picture",
            "click photo",
            "click picture",
            "capture photo",
            "capture picture"
        );
    }

    // Add a method to be called from AccessibilityService to store the coordinates
    public static void setSwitchButtonCoordinates(int x, int y) {
        switchButtonX = x;
        switchButtonY = y;
        learningSwitchButton = false;
    }

    public static boolean isLearningSwitchButton() {
        return learningSwitchButton;
    }
} 