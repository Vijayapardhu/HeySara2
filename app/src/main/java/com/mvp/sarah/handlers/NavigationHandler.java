package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.content.pm.PackageManager;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class NavigationHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "navigate to",
            "directions to",
            "take me to"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("navigate to") || command.contains("directions to") || command.contains("take me to");
    }

    @Override
    public void handle(Context context, String command) {
        String destination = command.replace("navigate to", "").replace("directions to", "").replace("take me to", "").trim();
        if (destination.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify a destination.");
            return;
        }
        String uri = "google.navigation:q=" + Uri.encode(destination);
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uri));
        intent.setPackage("com.google.android.apps.maps");
        PackageManager pm = context.getPackageManager();
        if (intent.resolveActivity(pm) != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Navigating to " + destination);
        } else {
            // Google Maps not installed, try generic geo intent
            Intent geoIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=" + Uri.encode(destination)));
            if (geoIntent.resolveActivity(pm) != null) {
                geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                geoIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(geoIntent);
                FeedbackProvider.speakAndToast(context, "Opening maps for " + destination);
            } else {
                // No maps app, open Play Store for Google Maps
                try {
                    Intent playStoreIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
                    playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    playStoreIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(playStoreIntent);
                    FeedbackProvider.speakAndToast(context, "Please install Google Maps to use navigation.");
                } catch (Exception e) {
                    FeedbackProvider.speakAndToast(context, "No maps app found and cannot open Play Store.");
                }
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 