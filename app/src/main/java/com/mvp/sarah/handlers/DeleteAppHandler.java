package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class DeleteAppHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.startsWith("delete ") || lower.startsWith("uninstall ");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        String appName = lower.replaceFirst("delete ", "").replaceFirst("uninstall ", "").trim();
        if (appName.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify the app name to delete.");
            return;
        }
        PackageManager pm = context.getPackageManager();
        List<ResolveInfo> packages = pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN, null).addCategory(Intent.CATEGORY_LAUNCHER), 0);
        String packageName = null;
        for (ResolveInfo info : packages) {
            String label = info.loadLabel(pm).toString().toLowerCase(Locale.ROOT);
            if (label.equals(appName) || label.contains(appName)) {
                packageName = info.activityInfo.packageName;
                break;
            }
        }
        if (packageName == null) {
            FeedbackProvider.speakAndToast(context, "App '" + appName + "' not found.");
            return;
        }
        Intent intent = new Intent(Intent.ACTION_DELETE);
        intent.setData(Uri.parse("package:" + packageName));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
        FeedbackProvider.speakAndToast(context, "Uninstalling " + appName);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("delete [app name]", "uninstall [app name]");
    }
} 