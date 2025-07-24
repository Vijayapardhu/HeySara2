package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

public class OpenAppHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static Map<String, String> appLabelToPackage = null;

    private void ensureAppList(Context context) {
        if (appLabelToPackage == null) {
            appLabelToPackage = new HashMap<>();
            PackageManager pm = context.getPackageManager();
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
            for (ResolveInfo info : apps) {
                String label = info.loadLabel(pm).toString().toLowerCase(Locale.ROOT);
                String pkg = info.activityInfo.packageName;
                appLabelToPackage.put(label, pkg);
            }
        }
    }

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        
        // Don't handle settings-related commands - let OpenSettingsHandler handle them
        if (lower.contains("settings") || 
            lower.contains("options") ||
            lower.contains("developer") ||
            lower.contains("wifi") ||
            lower.contains("bluetooth") ||
            lower.contains("battery") ||
            lower.contains("display") ||
            lower.contains("sound") ||
            lower.contains("security") ||
            lower.contains("privacy") ||
            lower.contains("location") ||
            lower.contains("storage") ||
            lower.contains("language") ||
            lower.contains("accessibility") ||
            lower.contains("notification") ||
            lower.contains("do not disturb") ||
            lower.contains("dnd") ||
            lower.contains("airplane mode") ||
            lower.contains("flight mode") ||
            lower.contains("mobile data") ||
            lower.contains("cellular") ||
            lower.contains("hotspot") ||
            lower.contains("tethering") ||
            lower.contains("nfc") ||
            lower.contains("rotation") ||
            lower.contains("auto rotate") ||
            lower.contains("flashlight") ||
            lower.contains("torch") ||
            lower.contains("cast") ||
            lower.contains("screen mirror") ||
            lower.contains("night light") ||
            lower.contains("reading mode") ||
            lower.contains("battery saver") ||
            lower.contains("power saving")) {
            return false;
        }
        
        return lower.startsWith("open ") || lower.endsWith(" open") || lower.startsWith("app ");
    }

    @Override
    public void handle(Context context, String command) {
        ensureAppList(context);
        String appName = extractAppName(command);
        
        //android.util.Log.d("OpenAppHandler", "Command: '" + command + "', Extracted app name: '" + appName + "'");
        //android.util.Log.d("OpenAppHandler", "Available apps count: " + appLabelToPackage.size());
        
        String pkg = appLabelToPackage.get(appName);
        if (pkg != null) {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
                FeedbackProvider.speakAndToast(context, "Opening " + appName);
                return;
            }
        }
        
        // Fallback: try partial match
        for (Map.Entry<String, String> entry : appLabelToPackage.entrySet()) {
            if (entry.getKey().contains(appName)) {
                Intent intent = context.getPackageManager().getLaunchIntentForPackage(entry.getValue());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                    FeedbackProvider.speakAndToast(context, "Opening " + entry.getKey());
                    return;
                }
            }
        }
        
        // If no match found, show some suggestions
        android.util.Log.d("OpenAppHandler", "No app found for: '" + appName + "'");
        StringBuilder suggestions = new StringBuilder("Sorry, I couldn't find the app '" + appName + "'. ");
        suggestions.append("Available apps include: ");
        
        // Show first 5 available apps as suggestions
        int count = 0;
        for (String availableApp : appLabelToPackage.keySet()) {
            if (count < 5) {
                suggestions.append(availableApp).append(", ");
                count++;
            } else {
                break;
            }
        }
        if (count > 0) {
            suggestions.setLength(suggestions.length() - 2); // Remove last comma
        }
        
        FeedbackProvider.speakAndToast(context, suggestions.toString());
    }
    
    private String extractAppName(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        if (lower.startsWith("open ")) {
            return lower.replace("open ", "").trim();
        } else if (lower.endsWith(" open")) {
            return lower.replace(" open", "").trim();
        } else if (lower.startsWith("app ")) {
            return lower.replace("app ", "").trim();
        }
        return "";
    }
    
    @Override
    public List<String> getSuggestions() {
        if(appLabelToPackage == null) return Arrays.asList("open [app name]", "[app name] open", "app [app name]");
        return appLabelToPackage.keySet().stream()
            .flatMap(name -> Arrays.asList("open " + name, name + " open", "app " + name).stream())
            .collect(Collectors.toList());
    }
} 