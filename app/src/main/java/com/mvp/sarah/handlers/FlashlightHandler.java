package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class FlashlightHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "turn on flashlight",
            "turn off flashlight",
            "enable torch",
            "strobe the flashlight"
    );

    private static Handler strobeHandler;
    private static Runnable strobeRunnable;
    private static boolean isStrobing = false;

    @Override
    public boolean canHandle(String command) {
        return command.contains("flashlight") || command.contains("torch");
    }

    @Override
    public void handle(Context context, String command) {
        if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH)) {
            FeedbackProvider.speakAndToast(context, "Flashlight is not available on this device.");
            return;
        }

        if (command.contains("strobe")) {
            toggleStrobe(context, true);
        } else {
            boolean enable = command.contains("on") || command.contains("enable");
            if (isStrobing) {
                toggleStrobe(context, false); // Stop strobe if it's running
            }
            toggleTorch(context, enable);
        }
    }

    private void toggleTorch(Context context, boolean enable) {
        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            cameraManager.setTorchMode(cameraId, enable);
            if (!isStrobing) { // Avoid double feedback
                FeedbackProvider.speakAndToast(context, "Flashlight " + (enable ? "on" : "off"));
            }
        } catch (CameraAccessException | ArrayIndexOutOfBoundsException e) {
            FeedbackProvider.speakAndToast(context, "Could not access the flashlight.");
        }
    }

    private void toggleStrobe(Context context, boolean start) {
        if (start && !isStrobing) {
            isStrobing = true;
            FeedbackProvider.speakAndToast(context, "Starting strobe effect.");
            strobeHandler = new Handler(Looper.getMainLooper());
            strobeRunnable = new Runnable() {
                private boolean torchState = false;
                @Override
                public void run() {
                    torchState = !torchState;
                    toggleTorch(context, torchState);
                    strobeHandler.postDelayed(this, 100); // 100ms interval for strobe
                }
            };
            strobeHandler.post(strobeRunnable);
        } else if (!start && isStrobing) {
            isStrobing = false;
            if (strobeHandler != null) {
                strobeHandler.removeCallbacks(strobeRunnable);
            }
            toggleTorch(context, false); // Ensure flashlight is turned off
            FeedbackProvider.speakAndToast(context, "Strobe off.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 