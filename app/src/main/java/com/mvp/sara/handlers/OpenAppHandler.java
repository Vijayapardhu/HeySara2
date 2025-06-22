package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
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
        return command.startsWith("open ");
    }

    @Override
    public void handle(Context context, String command) {
        ensureAppList(context);
        String appName = command.replace("open ", "").trim().toLowerCase(Locale.ROOT);
        String pkg = appLabelToPackage.get(appName);
        if (pkg != null) {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage(pkg);
            if (intent != null) {
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
                    context.startActivity(intent);
                    FeedbackProvider.speakAndToast(context, "Opening " + entry.getKey());
                    return;
                }
            }
        }
        FeedbackProvider.speakAndToast(context, "Sorry, I couldn't find the app " + appName);
    }
    
    @Override
    public List<String> getSuggestions() {
        if(appLabelToPackage == null) return Arrays.asList("open [app name]");
        return appLabelToPackage.keySet().stream().map(name -> "open " + name).collect(Collectors.toList());
    }
} 