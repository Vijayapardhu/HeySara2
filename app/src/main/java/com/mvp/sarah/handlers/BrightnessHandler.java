package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Settings;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

import java.util.Arrays;
import java.util.List;

public class BrightnessHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase();
        return lowerCmd.contains("brightness") || 
               lowerCmd.contains("bright") ||
               lowerCmd.contains("dim") ||
               lowerCmd.contains("dark");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase();
        
        // Check for WRITE_SETTINGS permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(context)) {
                FeedbackProvider.speakAndToast(context, "I need permission to change screen brightness. Please enable it in settings.");
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
                return;
            }
        }

        try {
            int currentBrightness = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 128);
            int maxBrightness = 255;
            int oneStep = maxBrightness / 10; // 10% step

            if (lowerCmd.contains("increase") || lowerCmd.contains("up") || lowerCmd.contains("brighter")) {
                int newBrightness = Math.min(currentBrightness + oneStep, maxBrightness);
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, newBrightness);
                int percent = (newBrightness * 100) / maxBrightness;
                FeedbackProvider.speakAndToast(context, "Brightness increased to " + percent + "%");
            } else if (lowerCmd.contains("decrease") || lowerCmd.contains("down") || lowerCmd.contains("dim") || lowerCmd.contains("darker")) {
                int newBrightness = Math.max(currentBrightness - oneStep, 0);
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, newBrightness);
                int percent = (newBrightness * 100) / maxBrightness;
                FeedbackProvider.speakAndToast(context, "Brightness decreased to " + percent + "%");
            } else if (lowerCmd.contains("set") || lowerCmd.contains("to")) {
                // Improved: Extract percentage from command (handles 'percent', '%', and numbers before them)
                String[] words = command.split("\\s+");
                int percent = -1;
                for (int i = 0; i < words.length; i++) {
                    String word = words[i].replaceAll("[^0-9%]", "");
                    if (word.endsWith("%")) {
                        word = word.replace("%", "");
                    }
                    if (word.matches("\\d+")) {
                        // Check if next word is 'percent' or contains '%'
                        boolean isPercent = (i + 1 < words.length && (words[i + 1].toLowerCase().contains("percent") || words[i + 1].contains("%"))) || words[i].contains("%") || words[i].toLowerCase().contains("percent");
                        if (isPercent || percent == -1) {
                            percent = Integer.parseInt(word);
                            break;
                        }
                    }
                }
                // Fallback: look for any number if not found
                if (percent == -1) {
                    for (String word : words) {
                        if (word.matches("\\d+")) {
                            percent = Integer.parseInt(word);
                            break;
                        }
                    }
                }
                if (percent >= 0 && percent <= 100) {
                    int newBrightness = (maxBrightness * percent) / 100;
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, newBrightness);
                    FeedbackProvider.speakAndToast(context, "Brightness set to " + percent + "%");
                } else {
                    FeedbackProvider.speakAndToast(context, "Please specify a percentage between 0 and 100");
                }
            } else if (lowerCmd.contains("maximum") || lowerCmd.contains("max") || lowerCmd.contains("full") || lowerCmd.contains("100")) {
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, maxBrightness);
                FeedbackProvider.speakAndToast(context, "Brightness set to maximum");
            } else if (lowerCmd.contains("minimum") || lowerCmd.contains("lowest") || lowerCmd.contains("0")) {
                Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS, 1);
                FeedbackProvider.speakAndToast(context, "Brightness set to minimum");
            } else if (lowerCmd.contains("auto brightness")) {
                if (lowerCmd.contains("enable") || lowerCmd.contains("on")) {
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC);
                    FeedbackProvider.speakAndToast(context, "Auto-brightness enabled");
                } else if (lowerCmd.contains("disable") || lowerCmd.contains("off")) {
                    Settings.System.putInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    FeedbackProvider.speakAndToast(context, "Auto-brightness disabled");
                } else {
                    int mode = Settings.System.getInt(context.getContentResolver(), Settings.System.SCREEN_BRIGHTNESS_MODE, Settings.System.SCREEN_BRIGHTNESS_MODE_MANUAL);
                    String status = (mode == Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC) ? "enabled" : "disabled";
                    FeedbackProvider.speakAndToast(context, "Auto-brightness is currently " + status);
                }
                return;
            } else {
                // Default: show current brightness
                int percent = (currentBrightness * 100) / maxBrightness;
                FeedbackProvider.speakAndToast(context, "Current brightness is " + percent + "%");
            }
        } catch (Exception e) {
            android.util.Log.e("BrightnessHandler", "Error setting brightness: " + e.getMessage());
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't change the brightness. Please check permissions.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "set brightness to 50 percent",
            "increase brightness",
            "decrease brightness",
            "max brightness",
            "brightness minimum",
            "make it brighter",
            "make it darker",
            "enable auto brightness",
            "disable auto brightness"
        );
    }
} 