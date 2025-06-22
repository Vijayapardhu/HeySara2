package com.mvp.sara.handlers;

import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.widget.Toast;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class FlashlightHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "turn on flashlight",
            "turn off flashlight",
            "enable torch"
    );

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

        CameraManager cameraManager = (CameraManager) context.getSystemService(Context.CAMERA_SERVICE);
        try {
            String cameraId = cameraManager.getCameraIdList()[0];
            boolean enable = command.contains("on") || command.contains("enable");
            cameraManager.setTorchMode(cameraId, enable);
            FeedbackProvider.speakAndToast(context, "Flashlight " + (enable ? "on" : "off"));
        } catch (CameraAccessException | ArrayIndexOutOfBoundsException e) {
            FeedbackProvider.speakAndToast(context, "Could not access the flashlight.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 