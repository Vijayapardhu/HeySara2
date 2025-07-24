package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class OpenSettingsHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    
    private static final String TAG = "OpenSettingsHandler";
    private static final Map<String, String> settingsMap = new HashMap<>();
    
    static {
        // General settings
        settingsMap.put("settings", Settings.ACTION_SETTINGS);
        settingsMap.put("general settings", Settings.ACTION_SETTINGS);
        settingsMap.put("main settings", Settings.ACTION_SETTINGS);
        settingsMap.put("phone settings", Settings.ACTION_SETTINGS);
        settingsMap.put("device settings", Settings.ACTION_SETTINGS);
        
        // Display settings
        settingsMap.put("display settings", Settings.ACTION_DISPLAY_SETTINGS);
        settingsMap.put("display", Settings.ACTION_DISPLAY_SETTINGS);
        settingsMap.put("screen settings", Settings.ACTION_DISPLAY_SETTINGS);
        settingsMap.put("screen", Settings.ACTION_DISPLAY_SETTINGS);
        settingsMap.put("brightness settings", Settings.ACTION_DISPLAY_SETTINGS);
        settingsMap.put("brightness", Settings.ACTION_DISPLAY_SETTINGS);
        settingsMap.put("wallpaper settings", Settings.ACTION_DISPLAY_SETTINGS);
        settingsMap.put("wallpaper", Settings.ACTION_DISPLAY_SETTINGS);
        settingsMap.put("theme settings", Settings.ACTION_DISPLAY_SETTINGS);
        settingsMap.put("theme", Settings.ACTION_DISPLAY_SETTINGS);
        
        // Developer settings
        settingsMap.put("developer settings", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        settingsMap.put("developer options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        settingsMap.put("developer", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        settingsMap.put("dev settings", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        settingsMap.put("dev options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        settingsMap.put("development settings", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        settingsMap.put("development options", Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS);
        
        // Network settings
        settingsMap.put("wifi settings", Settings.ACTION_WIFI_SETTINGS);
        settingsMap.put("wifi", Settings.ACTION_WIFI_SETTINGS);
        settingsMap.put("wi-fi settings", Settings.ACTION_WIFI_SETTINGS);
        settingsMap.put("wi-fi", Settings.ACTION_WIFI_SETTINGS);
        settingsMap.put("wireless settings", Settings.ACTION_WIFI_SETTINGS);
        settingsMap.put("wireless", Settings.ACTION_WIFI_SETTINGS);
        settingsMap.put("bluetooth settings", Settings.ACTION_BLUETOOTH_SETTINGS);
        settingsMap.put("bluetooth", Settings.ACTION_BLUETOOTH_SETTINGS);
        settingsMap.put("bt settings", Settings.ACTION_BLUETOOTH_SETTINGS);
        settingsMap.put("bt", Settings.ACTION_BLUETOOTH_SETTINGS);
        settingsMap.put("mobile data settings", Settings.ACTION_DATA_ROAMING_SETTINGS);
        settingsMap.put("mobile data", Settings.ACTION_DATA_ROAMING_SETTINGS);
        settingsMap.put("data settings", Settings.ACTION_DATA_ROAMING_SETTINGS);
        settingsMap.put("data", Settings.ACTION_DATA_ROAMING_SETTINGS);
        settingsMap.put("cellular settings", Settings.ACTION_DATA_ROAMING_SETTINGS);
        settingsMap.put("cellular", Settings.ACTION_DATA_ROAMING_SETTINGS);
        settingsMap.put("airplane mode settings", Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        settingsMap.put("airplane mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        settingsMap.put("aeroplane mode settings", Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        settingsMap.put("aeroplane mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        settingsMap.put("flight mode settings", Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        settingsMap.put("flight mode", Settings.ACTION_AIRPLANE_MODE_SETTINGS);
        settingsMap.put("hotspot settings", Settings.ACTION_WIFI_SETTINGS);
        settingsMap.put("hotspot", Settings.ACTION_WIFI_SETTINGS);
        settingsMap.put("tethering settings", Settings.ACTION_WIFI_SETTINGS);
        settingsMap.put("tethering", Settings.ACTION_WIFI_SETTINGS);
        
        // Sound settings
        settingsMap.put("sound settings", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("sound", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("audio settings", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("audio", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("volume settings", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("volume", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("ringtone settings", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("ringtone", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("vibration settings", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("vibration", Settings.ACTION_SOUND_SETTINGS);
        settingsMap.put("notification settings", Settings.ACTION_SETTINGS);
        settingsMap.put("notifications", Settings.ACTION_SETTINGS);
        settingsMap.put("notification", Settings.ACTION_SETTINGS);
        settingsMap.put("do not disturb settings", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        settingsMap.put("do not disturb", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        settingsMap.put("dnd settings", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        settingsMap.put("dnd", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        settingsMap.put("silent mode settings", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        settingsMap.put("silent mode", Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
        
        // Security settings
        settingsMap.put("security settings", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("security", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("lock screen settings", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("lock screen", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("lockscreen settings", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("lockscreen", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("fingerprint settings", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("fingerprint", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("face unlock settings", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("face unlock", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("face id settings", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("face id", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("pattern settings", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("pattern", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("pin settings", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("pin", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("password settings", Settings.ACTION_SECURITY_SETTINGS);
        settingsMap.put("password", Settings.ACTION_SECURITY_SETTINGS);
        
        // Storage settings
        settingsMap.put("storage settings", Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        settingsMap.put("storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        settingsMap.put("memory settings", Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        settingsMap.put("memory", Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        settingsMap.put("internal storage settings", Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        settingsMap.put("internal storage", Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        settingsMap.put("sd card settings", Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        settingsMap.put("sd card", Settings.ACTION_INTERNAL_STORAGE_SETTINGS);
        
        // Battery settings
        settingsMap.put("battery settings", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        settingsMap.put("battery", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        settingsMap.put("battery optimization settings", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        settingsMap.put("battery optimization", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        settingsMap.put("power settings", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        settingsMap.put("power", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        settingsMap.put("power saving settings", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        settingsMap.put("power saving", Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
        
        // App settings
        settingsMap.put("app settings", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("apps settings", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("applications settings", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("apps", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("applications", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("app", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("application", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("installed apps settings", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("installed apps", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("manage apps settings", Settings.ACTION_APPLICATION_SETTINGS);
        settingsMap.put("manage apps", Settings.ACTION_APPLICATION_SETTINGS);
        
        // Language settings
        settingsMap.put("language settings", Settings.ACTION_LOCALE_SETTINGS);
        settingsMap.put("language", Settings.ACTION_LOCALE_SETTINGS);
        settingsMap.put("locale settings", Settings.ACTION_LOCALE_SETTINGS);
        settingsMap.put("locale", Settings.ACTION_LOCALE_SETTINGS);
        settingsMap.put("input settings", Settings.ACTION_LOCALE_SETTINGS);
        settingsMap.put("input", Settings.ACTION_LOCALE_SETTINGS);
        settingsMap.put("keyboard settings", Settings.ACTION_LOCALE_SETTINGS);
        settingsMap.put("keyboard", Settings.ACTION_LOCALE_SETTINGS);
        settingsMap.put("text settings", Settings.ACTION_LOCALE_SETTINGS);
        settingsMap.put("text", Settings.ACTION_LOCALE_SETTINGS);
        
        // Date and time settings
        settingsMap.put("date time settings", Settings.ACTION_DATE_SETTINGS);
        settingsMap.put("date time", Settings.ACTION_DATE_SETTINGS);
        settingsMap.put("date settings", Settings.ACTION_DATE_SETTINGS);
        settingsMap.put("time settings", Settings.ACTION_DATE_SETTINGS);
        settingsMap.put("date", Settings.ACTION_DATE_SETTINGS);
        settingsMap.put("time", Settings.ACTION_DATE_SETTINGS);
        settingsMap.put("clock settings", Settings.ACTION_DATE_SETTINGS);
        settingsMap.put("clock", Settings.ACTION_DATE_SETTINGS);
        settingsMap.put("calendar settings", Settings.ACTION_DATE_SETTINGS);
        settingsMap.put("calendar", Settings.ACTION_DATE_SETTINGS);
        
        // Accessibility settings
        settingsMap.put("accessibility settings", Settings.ACTION_ACCESSIBILITY_SETTINGS);
        settingsMap.put("accessibility", Settings.ACTION_ACCESSIBILITY_SETTINGS);
        settingsMap.put("access settings", Settings.ACTION_ACCESSIBILITY_SETTINGS);
        settingsMap.put("access", Settings.ACTION_ACCESSIBILITY_SETTINGS);
        
        // Privacy settings
        settingsMap.put("privacy settings", Settings.ACTION_PRIVACY_SETTINGS);
        settingsMap.put("privacy", Settings.ACTION_PRIVACY_SETTINGS);
        settingsMap.put("permissions settings", Settings.ACTION_PRIVACY_SETTINGS);
        settingsMap.put("permissions", Settings.ACTION_PRIVACY_SETTINGS);
        
        // Location settings
        settingsMap.put("location settings", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsMap.put("location", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsMap.put("gps settings", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsMap.put("gps", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsMap.put("location services settings", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsMap.put("location services", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsMap.put("location access settings", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        settingsMap.put("location access", Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        
        // System settings
        settingsMap.put("system settings", Settings.ACTION_SETTINGS);
        settingsMap.put("system", Settings.ACTION_SETTINGS);
        settingsMap.put("about phone settings", Settings.ACTION_DEVICE_INFO_SETTINGS);
        settingsMap.put("about phone", Settings.ACTION_DEVICE_INFO_SETTINGS);
        settingsMap.put("device info settings", Settings.ACTION_DEVICE_INFO_SETTINGS);
        settingsMap.put("device info", Settings.ACTION_DEVICE_INFO_SETTINGS);
        settingsMap.put("phone info settings", Settings.ACTION_DEVICE_INFO_SETTINGS);
        settingsMap.put("phone info", Settings.ACTION_DEVICE_INFO_SETTINGS);
        settingsMap.put("build number settings", Settings.ACTION_DEVICE_INFO_SETTINGS);
        settingsMap.put("build number", Settings.ACTION_DEVICE_INFO_SETTINGS);
        settingsMap.put("software info settings", Settings.ACTION_DEVICE_INFO_SETTINGS);
        settingsMap.put("software info", Settings.ACTION_DEVICE_INFO_SETTINGS);
    }
    
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        
        // Only handle commands that explicitly start with "open" and are about settings
        if (!lower.startsWith("open ")) {
            return false;
        }
        
        // Explicitly exclude ALL toggle commands - let other handlers handle these
        if (lower.contains("turn on") || lower.contains("turn off") || 
            lower.contains("enable") || lower.contains("disable") ||
            lower.contains("toggle") ||
            lower.contains("what's") || lower.contains("how much") ||
            lower.contains("battery level") || lower.contains("battery status") ||
            lower.contains("switch") || lower.contains("change")) {
            return false;
        }
        
        // Check if it's a settings-related command
        String settingName = extractSettingName(lower);
        
        // Only handle if it contains "settings" or matches our settings map
        return lower.contains("settings") || settingsMap.containsKey(settingName) || hasPartialMatch(lower);
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        String settingName = extractSettingName(lower);
        
        Log.d(TAG, "Handling command: '" + command + "', extracted setting: '" + settingName + "'");
        
        String action = settingsMap.get(settingName);
        String matchedSetting = settingName;
        
        if (action == null) {
            // Try to find a partial match
            for (Map.Entry<String, String> entry : settingsMap.entrySet()) {
                if (isPartialMatch(settingName, entry.getKey())) {
                    action = entry.getValue();
                    matchedSetting = entry.getKey();
                    Log.d(TAG, "Found partial match: '" + settingName + "' -> '" + matchedSetting + "'");
                    break;
                }
            }
        }
        
        if (action != null) {
            Log.d(TAG, "Opening settings: '" + matchedSetting + "' with action: " + action);
            FeedbackProvider.speakAndToast(context, "Opening " + matchedSetting + ".");
            Intent intent = new Intent(action);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
                Log.d(TAG, "Successfully opened settings");
            } catch (Exception e) {
                Log.e(TAG, "Failed to open settings: " + e.getMessage());
                FeedbackProvider.speakAndToast(context, "Could not open " + matchedSetting + ". Opening general settings instead.");
                // Fallback to general settings
                try {
                    Intent fallbackIntent = new Intent(Settings.ACTION_SETTINGS);
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    fallbackIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(fallbackIntent);
                } catch (Exception fallbackError) {
                    Log.e(TAG, "Failed to open fallback settings: " + fallbackError.getMessage());
                    FeedbackProvider.speakAndToast(context, "Could not open any settings.");
                }
            }
        } else {
            Log.w(TAG, "No matching settings found for: '" + settingName + "'");
            FeedbackProvider.speakAndToast(context, "Sorry, I don't know how to open that setting. Opening general settings instead.");
            try {
                Intent intent = new Intent(Settings.ACTION_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open general settings: " + e.getMessage());
                FeedbackProvider.speakAndToast(context, "Could not open settings.");
            }
        }
    }
    
    private String extractSettingName(String command) {
        // Remove "open" and "settings" from the command
        String result = command.replace("open ", "").replace(" settings", "").trim();
        return result;
    }
    
    private boolean hasPartialMatch(String command) {
        String settingName = extractSettingName(command);
        for (String key : settingsMap.keySet()) {
            if (isPartialMatch(settingName, key)) {
                return true;
            }
        }
        return false;
    }
    
    private boolean isPartialMatch(String input, String target) {
        if (input == null || target == null) return false;
        
        // Exact match
        if (input.equals(target)) return true;
        
        // Contains match (input contains target or target contains input)
        if (input.contains(target) || target.contains(input)) return true;
        
        // Word boundary match (e.g., "wifi" matches "wi-fi")
        String normalizedInput = input.replace("-", "").replace(" ", "");
        String normalizedTarget = target.replace("-", "").replace(" ", "");
        if (normalizedInput.equals(normalizedTarget)) return true;
        
        // Similar words (e.g., "bt" matches "bluetooth")
        if ((input.equals("bt") && target.contains("bluetooth")) ||
            (input.equals("dnd") && target.contains("disturb")) ||
            (input.equals("dev") && target.contains("developer")) ||
            (input.equals("audio") && target.contains("sound")) ||
            (input.equals("cellular") && target.contains("data")) ||
            (input.equals("flight") && target.contains("airplane")) ||
            (input.equals("aeroplane") && target.contains("airplane"))) {
            return true;
        }
        
        return false;
    }
    
    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "open settings",
            "open display settings", 
            "open wifi settings",
            "open bluetooth settings",
            "open developer settings",
            "open sound settings",
            "open security settings",
            "open battery settings",
            "open app settings",
            "open language settings",
            "open accessibility settings",
            "open notification settings",
            "open storage settings",
            "open location settings",
            "open privacy settings"
        );
    }
}
