package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import com.mvp.sarah.ClickAccessibilityService;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class LockUnlockAppHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String PREFS = "AppLockPrefs";
    private static final String KEY_LOCKED_APPS = "locked_apps";
    private static final String KEY_PIN = "app_lock_pin";
    private static final String KEY_PIN_SET = "app_lock_pin_set";
    private static final String DEFAULT_SECRET = "HI iam User";

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.startsWith("lock ")
            || lower.startsWith("unlock ")
            || lower.contains("change app lock")
            || lower.contains("manage app lock")
            || lower.contains("manage applock"); // Accept both with and without space
    }

    @Override
    public void handle(Context context, String command) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String lower = command.toLowerCase(Locale.ROOT);
        if (lower.contains("manage applock")) {
            FeedbackProvider.speakAndToast(context, "Opening app lock management.");
            Intent intent = new Intent(context, com.mvp.sarah.ManageAppLockActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            return;
        }
        if (lower.contains("change applock")) {
            FeedbackProvider.speakAndToast(context, "Opening app lock setup. Please set a new PIN.");
            com.mvp.sarah.AppLockActivity.launchSetup(context);
            return;
        }
        boolean isLock = lower.startsWith("lock ");
        boolean isUnlock = lower.startsWith("unlock ");
        String appName = lower;
        if (lower.startsWith("lock ")) {
            appName = lower.substring(5).trim();
        } else if (lower.startsWith("unlock ")) {
            appName = lower.substring(7).trim();
        }
        if (appName.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify the app name.");
            return;
        }
        Set<String> lockedApps = new HashSet<>(prefs.getStringSet(KEY_LOCKED_APPS, new HashSet<>()));
        
        // Enhanced app name matching with debugging
        String spokenName = appName.toLowerCase(Locale.ROOT);
        String foundPackage = null;
        String foundLabel = null;
        
        // First, check if it's already in locked apps (for unlock command)
        if (isUnlock) {
            for (String lockedPackage : lockedApps) {
                if (lockedPackage.toLowerCase().contains(spokenName)) {
                    foundPackage = lockedPackage;
                    foundLabel = appName; // Use the spoken name as label
                    break;
                }
            }
        }
        
        // If not found in locked apps, search in all apps map
        if (foundPackage == null) {
            String allAppsJson = prefs.getString("all_apps_map", "{}");
            Type type = new TypeToken<Map<String, String>>(){}.getType();
            Map<String, String> appNameToPackage = new Gson().fromJson(allAppsJson, type);
            
            // Debug: Log available apps
            System.out.println("Available apps: " + appNameToPackage.keySet());
            System.out.println("Looking for: " + spokenName);
            
            // Try exact match first
            for (Map.Entry<String, String> entry : appNameToPackage.entrySet()) {
                String appLabel = entry.getKey().toLowerCase(Locale.ROOT);
                if (appLabel.equals(spokenName) || appLabel.contains(spokenName) || spokenName.contains(appLabel)) {
                    foundPackage = entry.getValue();
                    foundLabel = entry.getKey();
                    System.out.println("Found app: " + foundLabel + " -> " + foundPackage);
                    break;
                }
            }
            
            // If still not found, try package name match
            if (foundPackage == null) {
                for (Map.Entry<String, String> entry : appNameToPackage.entrySet()) {
                    String packageName = entry.getValue().toLowerCase(Locale.ROOT);
                    if (packageName.contains(spokenName)) {
                        foundPackage = entry.getValue();
                        foundLabel = entry.getKey();
                        System.out.println("Found by package: " + foundLabel + " -> " + foundPackage);
                        break;
                    }
                }
            }
            
            // If still not found, try common app mappings
            if (foundPackage == null) {
                foundPackage = getCommonAppPackage(spokenName);
                if (foundPackage != null) {
                    foundLabel = appName;
                    System.out.println("Found by common mapping: " + foundLabel + " -> " + foundPackage);
                }
            }
        }
        if (isLock) {
            if (foundPackage != null) {
                lockedApps.add(foundPackage);
                prefs.edit().putStringSet(KEY_LOCKED_APPS, lockedApps).apply();
                
                // Update all_apps_map if needed
                String allAppsJson = prefs.getString("all_apps_map", "{}");
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                Map<String, String> appNameToPackage = new Gson().fromJson(allAppsJson, type);
                appNameToPackage.remove(foundLabel);
                prefs.edit().putString("all_apps_map", new Gson().toJson(appNameToPackage)).apply();
                
                FeedbackProvider.speakAndToast(context, (foundLabel != null ? foundLabel : appName) + " is now locked.");
            } else {
                FeedbackProvider.speakAndToast(context, "App not found: " + appName);
            }
            if (!prefs.getBoolean(KEY_PIN_SET, false)) {
                FeedbackProvider.speakAndToast(context, "Please set a 4-digit PIN for app lock.");
            }
        } else if (isUnlock) {
            if (foundPackage != null && lockedApps.contains(foundPackage)) {
                FeedbackProvider.speakAndToast(context, "Say the secret key to remove app lock for " + foundLabel);
                Intent intent = new Intent(context, com.mvp.sarah.SpeechUnlockActivity.class);
                intent.putExtra(com.mvp.sarah.SpeechUnlockActivity.EXTRA_PACKAGE, foundPackage);
                intent.putExtra(com.mvp.sarah.SpeechUnlockActivity.EXTRA_APPNAME, foundLabel != null ? foundLabel : appName);
                intent.putExtra(com.mvp.sarah.SpeechUnlockActivity.EXTRA_ACTION_TYPE, "remove_lock");
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } else {
                FeedbackProvider.speakAndToast(context, "App not found or not locked: " + appName);
            }
        }
    }

    /**
     * Get common app package names for popular apps
     */
    private String getCommonAppPackage(String appName) {
        switch (appName.toLowerCase()) {
            case "instagram":
            case "insta":
                return "com.instagram.android";
            case "facebook":
            case "fb":
                return "com.facebook.katana";
            case "whatsapp":
            case "wa":
                return "com.whatsapp";
            case "twitter":
            case "x":
                return "com.twitter.android";
            case "snapchat":
            case "snap":
                return "com.snapchat.android";
            case "youtube":
            case "yt":
                return "com.google.android.youtube";
            case "gmail":
            case "email":
                return "com.google.android.gm";
            case "maps":
            case "google maps":
                return "com.google.android.apps.maps";
            case "chrome":
            case "browser":
                return "com.android.chrome";
            case "spotify":
                return "com.spotify.music";
            case "netflix":
                return "com.netflix.mediaclient";
            case "amazon":
                return "com.amazon.mShop.android.shopping";
            case "telegram":
                return "org.telegram.messenger";
            case "discord":
                return "com.discord";
            case "tiktok":
                return "com.zhiliaoapp.musically";
            case "linkedin":
                return "com.linkedin.android";
            case "reddit":
                return "com.reddit.frontpage";
            case "pinterest":
                return "com.pinterest";
            case "uber":
                return "com.ubercab";
            case "lyft":
                return "me.lyft.android";
            case "zoom":
                return "us.zoom.videomeetings";
            case "teams":
            case "microsoft teams":
                return "com.microsoft.teams";
            case "skype":
                return "com.skype.raider";
            case "camera":
            case "photo":
                return "com.android.camera";
            case "gallery":
            case "photos":
                return "com.android.gallery3d";
            case "settings":
                return "com.android.settings";
            case "calculator":
                return "com.android.calculator2";
            case "clock":
                return "com.android.deskclock";
            case "calendar":
                return "com.google.android.calendar";
            case "contacts":
                return "com.android.contacts";
            case "phone":
                return "com.android.dialer";
            case "messages":
            case "sms":
                return "com.android.mms";
            default:
                return null;
        }
    }
    
    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("lock [app name]", "unlock [app name]");
    }
} 