// DeviceInfoHandler.java
package com.mvp.sara.handlers;

import android.content.Context;
import android.os.Build;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class DeviceInfoHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    public boolean canHandle(String command) { return command.contains("device info") || command.contains("phone name") || command.contains("your name"); }
    public void handle(Context c, String cmd) {
        if(cmd.contains("your name")){
            FeedbackProvider.speakAndToast(c, "My name is Sara, your personal assistant.");
            return;
        }
        String deviceName = Build.MANUFACTURER + " " + Build.MODEL;
        FeedbackProvider.speakAndToast(c, "This is a " + deviceName);
    }
    public List<String> getSuggestions() { return Arrays.asList("what's my device name", "what is your name"); }
} 