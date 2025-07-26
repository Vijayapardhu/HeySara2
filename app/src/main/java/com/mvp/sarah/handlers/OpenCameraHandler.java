package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraAccessException;
import android.util.Log;

import com.mvp.sarah.AppUtils;
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
               lowerCmd.contains("take selfie") ||
               lowerCmd.contains("selfie") ||
               // Check for timer-based photo commands
               (lowerCmd.contains("photo") && (lowerCmd.contains("in") || lowerCmd.contains("after") || lowerCmd.contains("timer"))) ||
               (lowerCmd.contains("selfie") && (lowerCmd.contains("in") || lowerCmd.contains("after") || lowerCmd.contains("timer"))) ||
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
        
        // Check for timer-based photo commands first
        if ((lowerCmd.contains("photo") || lowerCmd.contains("selfie")) && 
            (lowerCmd.contains("in") || lowerCmd.contains("after") || lowerCmd.contains("timer"))) {
            int seconds = extractSecondsFromCommand(command);
            if (seconds > 0) {
                if (lowerCmd.contains("selfie")) {
                    takeSelfieWithTimer(context, seconds);
                } else {
                    takePhotoWithTimer(context, seconds);
                }
                return;
            }
        }
        
        if (lowerCmd.contains("take selfie") || lowerCmd.contains("selfie")) {
            // Take selfie - open camera, switch to front camera, then take photo
            takeSelfie(context);
        } else if (lowerCmd.contains("take photo") || lowerCmd.contains("take picture") || 
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

        // Find camera app dynamically using shared utility
        String cameraPackage = AppUtils.findCameraAppPackage(context);
        if (cameraPackage != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(cameraPackage);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);

                // Wait for camera to load, then trigger photo capture
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    Intent captureIntent = new Intent("com.mvp.sarah.ACTION_TAKE_PHOTO_AUTO");
                    context.sendBroadcast(captureIntent);
                }, 3000); // Wait 3 seconds for camera to load

            } catch (Exception e) {
                Log.e(TAG, "Error opening camera app: " + cameraPackage, e);
                FeedbackProvider.speakAndToast(context, "Could not open camera app");
            }
        } else {
            FeedbackProvider.speakAndToast(context, "No camera app found on device");
        }
    }

    private void takeSelfie(Context context) {
        Log.d(TAG, "takeSelfie called");
        FeedbackProvider.speakAndToast(context, "Opening camera and taking selfie.");

        // Find camera app dynamically using shared utility
        String cameraPackage = AppUtils.findCameraAppPackage(context);
        if (cameraPackage != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(cameraPackage);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);

                // Wait for camera to load, then switch to front camera
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    // First switch to front camera
                    Intent switchIntent = new Intent("com.mvp.sarah.ACTION_SWITCH_CAMERA");
                    context.sendBroadcast(switchIntent);
                    
                    // Wait a bit more for camera switch to complete, then take photo
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Intent captureIntent = new Intent("com.mvp.sarah.ACTION_TAKE_PHOTO_AUTO");
                        context.sendBroadcast(captureIntent);
                    }, 2000); // Wait 2 seconds after switching camera
                    
                }, 3000); // Wait 3 seconds for camera to load

            } catch (Exception e) {
                Log.e(TAG, "Error opening camera app: " + cameraPackage, e);
                FeedbackProvider.speakAndToast(context, "Could not open camera app");
            }
        } else {
            FeedbackProvider.speakAndToast(context, "No camera app found on device");
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
            "capture picture",
            "take selfie",
            "selfie",
            "take photo in 5 seconds",
            "selfie after 10 seconds",
            "photo timer 3",
            "selfie in 7 seconds"
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
    
    /**
     * Extract number of seconds from timer-based commands
     * Examples: "take photo in 5 seconds" -> returns 5
     *          "selfie after 10 seconds" -> returns 10
     *          "photo timer 3" -> returns 3
     */
    private int extractSecondsFromCommand(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        
        // Look for patterns like "in X seconds", "after X seconds", "timer X"
        String[] patterns = {
            "in (\\d+) seconds?",
            "after (\\d+) seconds?",
            "timer (\\d+)",
            "in (\\d+) sec",
            "after (\\d+) sec",
            "(\\d+) seconds?",
            "(\\d+) sec"
        };
        
        for (String pattern : patterns) {
            java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
            java.util.regex.Matcher matcher = regex.matcher(lowerCmd);
            if (matcher.find()) {
                try {
                    int seconds = Integer.parseInt(matcher.group(1));
                    Log.d(TAG, "Extracted " + seconds + " seconds from command: " + command);
                    return seconds;
                } catch (NumberFormatException e) {
                    Log.w(TAG, "Failed to parse seconds from: " + matcher.group(1));
                }
            }
        }
        
        Log.w(TAG, "No seconds found in command: " + command);
        return 0;
    }

    private void takePhotoWithTimer(Context context, int seconds) {
        Log.d(TAG, "takePhotoWithTimer called with " + seconds + " seconds");
        FeedbackProvider.speakAndToast(context, "Taking photo in " + seconds + " seconds.");

        // Find camera app dynamically using shared utility
        String cameraPackage = AppUtils.findCameraAppPackage(context);
        if (cameraPackage != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(cameraPackage);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);

                // Wait for camera to load, then start countdown
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    startPhotoCountdown(context, seconds, false);
                }, 3000); // Wait 3 seconds for camera to load

            } catch (Exception e) {
                Log.e(TAG, "Error opening camera app: " + cameraPackage, e);
                FeedbackProvider.speakAndToast(context, "Could not open camera app");
            }
        } else {
            FeedbackProvider.speakAndToast(context, "No camera app found on device");
        }
    }

    private void takeSelfieWithTimer(Context context, int seconds) {
        Log.d(TAG, "takeSelfieWithTimer called with " + seconds + " seconds");
        FeedbackProvider.speakAndToast(context, "Taking selfie in " + seconds + " seconds.");

        // Find camera app dynamically using shared utility
        String cameraPackage = AppUtils.findCameraAppPackage(context);
        if (cameraPackage != null) {
            try {
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_LAUNCHER);
                intent.setPackage(cameraPackage);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);

                // Wait for camera to load, then switch to front camera and start countdown
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    // First switch to front camera
                    Intent switchIntent = new Intent("com.mvp.sarah.ACTION_SWITCH_CAMERA");
                    context.sendBroadcast(switchIntent);
                    
                    // Wait a bit more for camera switch to complete, then start countdown
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        startPhotoCountdown(context, seconds, true);
                    }, 2000); // Wait 2 seconds after switching camera
                    
                }, 3000); // Wait 3 seconds for camera to load

            } catch (Exception e) {
                Log.e(TAG, "Error opening camera app: " + cameraPackage, e);
                FeedbackProvider.speakAndToast(context, "Could not open camera app");
            }
        } else {
            FeedbackProvider.speakAndToast(context, "No camera app found on device");
        }
    }

    private void startPhotoCountdown(Context context, int totalSeconds, boolean isSelfie) {
        Log.d(TAG, "Starting photo countdown: " + totalSeconds + " seconds, selfie: " + isSelfie);
        
        // Start countdown from totalSeconds down to 1
        for (int i = totalSeconds; i > 0; i--) {
            final int currentSecond = i;
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                if (currentSecond > 3) {
                    // For longer countdowns, only announce at 5, 3, 2, 1
                    if (currentSecond == 5 || currentSecond <= 3) {
                        FeedbackProvider.speakAndToast(context, currentSecond + "");
                    }
                } else {
                    // For shorter countdowns, announce every second
                    FeedbackProvider.speakAndToast(context, currentSecond + "");
                }
            }, (totalSeconds - currentSecond) * 1000L);
        }
        
        // Take photo after countdown completes
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            FeedbackProvider.speakAndToast(context, "Taking " + (isSelfie ? "selfie" : "photo") + " now!");
            // Send a direct capture intent without reopening camera
            Intent captureIntent = new Intent("com.mvp.sarah.ACTION_TAKE_PHOTO_ONLY");
            context.sendBroadcast(captureIntent);
        }, totalSeconds * 1000L);
    }
} 