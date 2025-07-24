package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class CurrentVersionHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "current version",
        "what version",
        "app version"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("current version") ||
               lower.contains("what version") ||
               lower.contains("app version");
    }

    @Override
    public void handle(Context context, String command) {
        try {
            PackageManager pm = context.getPackageManager();
            String pkg = context.getPackageName();
            PackageInfo info = pm.getPackageInfo(pkg, 0);
            String version = info.versionName;
            FeedbackProvider.speakAndToast(context, "Sara version " + version);
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Could not get version: " + e.getMessage());
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 