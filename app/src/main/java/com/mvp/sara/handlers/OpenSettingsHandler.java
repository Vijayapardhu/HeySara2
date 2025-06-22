package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class OpenSettingsHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        return command.contains("settings") || command.contains("open settings");
    }

    @Override
    public void handle(Context context, String command) {
        String action = Settings.ACTION_SETTINGS;
        if(command.contains("wifi")) action = Settings.Panel.ACTION_WIFI;
        if(command.contains("bluetooth")) action = Settings.ACTION_BLUETOOTH_SETTINGS;
        // Add more specific settings here
        
        FeedbackProvider.speakAndToast(context, "Opening settings.");
        Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }
    
    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("open settings", "settings");
    }
}
