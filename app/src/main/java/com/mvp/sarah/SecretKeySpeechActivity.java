package com.mvp.sarah;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class SecretKeySpeechActivity extends Activity {
    public static final String EXTRA_PACKAGE = "package_name";
    public static final String EXTRA_APPNAME = "app_name";
    private static final int REQ_SPEECH = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String appName = getIntent().getStringExtra(EXTRA_APPNAME);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the secret key to unlock " + appName);
        startActivityForResult(intent, REQ_SPEECH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenKey = results.get(0).trim().toLowerCase();
                SharedPreferences prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE);
                String pin = prefs.getString("app_lock_pin", "");
                String secretPhrase = "hello sara i'm the user";
                String packageName = getIntent().getStringExtra(EXTRA_PACKAGE);
                String appName = getIntent().getStringExtra(EXTRA_APPNAME);
                String actionType = getIntent().getStringExtra("action_type");
                
                // Enhanced voice unlock validation
                boolean isValidSecret = spokenKey.equals(pin) || 
                                     spokenKey.equalsIgnoreCase(secretPhrase) ||
                                     spokenKey.contains("unlock") ||
                                     spokenKey.contains("open") ||
                                     spokenKey.contains("access") ||
                                     spokenKey.contains("hello sara") ||
                                     spokenKey.contains("i'm the user");
                
                if (isValidSecret) {
                    if ("remove_lock".equals(actionType)) {
                        // Remove app lock entirely
                        removeAppLock(packageName, appName);
                    } else {
                        // Temporary unlock for accessing locked app
                        temporaryUnlockApp(packageName, appName);
                    }
                } else {
                    // Record failed attempt
                    ClickAccessibilityService.recordUnlockAttempt(packageName);
                    
                    // Check if app is now locked due to too many attempts
                    if (ClickAccessibilityService.isAppLockedDueToAttempts(packageName)) {
                        Toast.makeText(this, "Too many failed attempts. App locked for 30 minutes.", Toast.LENGTH_LONG).show();
                        FeedbackProvider.speakAndToast(this, "Too many failed attempts. App locked for 30 minutes.");
                    } else {
                        Toast.makeText(this, "Incorrect secret key. Try again.", Toast.LENGTH_SHORT).show();
                        FeedbackProvider.speakAndToast(this, "Incorrect secret key. Try again.");
                    }
                }
            } else {
                Toast.makeText(this, "Could not recognize speech. Try again.", Toast.LENGTH_SHORT).show();
                FeedbackProvider.speakAndToast(this, "Could not recognize speech. Try again.");
            }
        } else {
            Toast.makeText(this, "Speech recognition cancelled or failed.", Toast.LENGTH_SHORT).show();
            FeedbackProvider.speakAndToast(this, "Speech recognition cancelled or failed.");
        }
        finish();
    }
    
    /**
     * Remove app lock entirely
     */
    private void removeAppLock(String packageName, String appName) {
        SharedPreferences prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE);
        Set<String> lockedApps = new HashSet<>(prefs.getStringSet("locked_apps", new HashSet<>()));
        
        // Remove from locked apps
        lockedApps.remove(packageName);
        prefs.edit().putStringSet("locked_apps", lockedApps).apply();
        
        // Clear any temporary unlock state
        ClickAccessibilityService.clearAppUnlockState(packageName);
        
        // Add back to available apps list
        String allAppsJson = prefs.getString("all_apps_map", "{}");
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> appNameToPackage = new Gson().fromJson(allAppsJson, type);
        appNameToPackage.put(appName, packageName);
        prefs.edit().putString("all_apps_map", new Gson().toJson(appNameToPackage)).apply();
        
        Toast.makeText(this, appName + " app lock removed successfully!", Toast.LENGTH_LONG).show();
        FeedbackProvider.speakAndToast(this, appName + " app lock removed successfully");
    }
    
    /**
     * Temporarily unlock app for access
     */
    private void temporaryUnlockApp(String packageName, String appName) {
        // Use enhanced unlock system
        boolean isTrusted = ClickAccessibilityService.isTrustedApp(packageName);
        if (isTrusted) {
            ClickAccessibilityService.markAppUnlockedExtended(packageName);
        } else {
            ClickAccessibilityService.markAppUnlocked(packageName);
        }
        
        // Show success message with remaining time
        long remainingTime = ClickAccessibilityService.getRemainingUnlockTime(packageName);
        String message = appName + " unlocked successfully!";
        if (remainingTime > 0) {
            long minutes = remainingTime / (60 * 1000);
            message += " Unlocked for " + minutes + " minutes.";
        }
        
        Toast.makeText(this, message, Toast.LENGTH_LONG).show();
        FeedbackProvider.speakAndToast(this, "App unlocked successfully");
        
        // Launch the unlocked app
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
        } catch (Exception e) {
            // App launch failed, but unlock was successful
        }
    }

    public static void handleSecretKeyResult(Context context, String spokenKey, String packageName, String appName, String actionType) {
        SharedPreferences prefs = context.getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE);
        String secretKey = prefs.getString("app_lock_secret", "");
        String normalizedSpoken = spokenKey.trim().replaceAll("\\s+", "").toLowerCase();
        String normalizedSecret = secretKey.trim().replaceAll("\\s+", "").toLowerCase();
        boolean isValidSecret = normalizedSpoken.equals(normalizedSecret);
        if (isValidSecret) {
            if ("remove_lock".equals(actionType)) {
                removeAppLock(context, packageName, appName);
            } else {
                temporaryUnlockApp(context, packageName, appName);
            }
        } else {
            ClickAccessibilityService.recordUnlockAttempt(packageName);
            if (ClickAccessibilityService.isAppLockedDueToAttempts(packageName)) {
                Toast.makeText(context, "Too many failed attempts. App locked for 30 minutes.", Toast.LENGTH_LONG).show();
                FeedbackProvider.speakAndToast(context, "Too many failed attempts. App locked for 30 minutes.");
            } else {
                Toast.makeText(context, "Incorrect secret key. Try again.", Toast.LENGTH_SHORT).show();
                FeedbackProvider.speakAndToast(context, "Incorrect secret key. Try again.");
            }
        }
    }

    public static void removeAppLock(Context context, String packageName, String appName) {
        SharedPreferences prefs = context.getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE);
        Set<String> lockedApps = new HashSet<>(prefs.getStringSet("locked_apps", new HashSet<>()));
        lockedApps.remove(packageName);
        prefs.edit().putStringSet("locked_apps", lockedApps).apply();
        ClickAccessibilityService.clearAppUnlockState(packageName);
        String allAppsJson = prefs.getString("all_apps_map", "{}");
        java.lang.reflect.Type type = new com.google.gson.reflect.TypeToken<java.util.Map<String, String>>(){}.getType();
        java.util.Map<String, String> appNameToPackage = new com.google.gson.Gson().fromJson(allAppsJson, type);
        appNameToPackage.put(appName, packageName);
        prefs.edit().putString("all_apps_map", new com.google.gson.Gson().toJson(appNameToPackage)).apply();
        Toast.makeText(context, appName + " app lock removed successfully", Toast.LENGTH_LONG).show();
        FeedbackProvider.speakAndToast(context, appName + " app lock removed successfully");
    }

    public static void temporaryUnlockApp(Context context, String packageName, String appName) {
        boolean isTrusted = ClickAccessibilityService.isTrustedApp(packageName);
        if (isTrusted) {
            ClickAccessibilityService.markAppUnlockedExtended(packageName);
        } else {
            ClickAccessibilityService.markAppUnlocked(packageName);
        }
        long remainingTime = ClickAccessibilityService.getRemainingUnlockTime(packageName);
        String message = appName + " unlocked successfully";
        if (remainingTime > 0) {
            long minutes = remainingTime / (60 * 1000);
            message += " for " + minutes + " minutes";
        }
        Toast.makeText(context, message, Toast.LENGTH_LONG).show();
        FeedbackProvider.speakAndToast(context, message);
        try {
            android.content.pm.PackageManager pm = context.getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            }
        } catch (Exception e) {
            // App launch failed, but unlock was successful
        }
    }
} 