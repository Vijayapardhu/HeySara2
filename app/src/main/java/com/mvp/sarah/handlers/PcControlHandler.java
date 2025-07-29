package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.SharedPreferences;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.net.HttpURLConnection;
import java.net.URL;

public class PcControlHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        return command.startsWith("pc ") || command.startsWith("computer ");
    }

    @Override
    public void handle(Context context, String command) {
        String pcCommand = command.replaceFirst("^(pc|computer)\\s+", "");
        SharedPreferences prefs = context.getSharedPreferences("SaraSettingsPrefs", Context.MODE_PRIVATE);
        String pcIp = prefs.getString("pc_ip", "");
        if (pcIp.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please set your PC IP in Sara settings.");
            return;
        }
        new Thread(() -> {
            try {
                URL url = new URL("http://" + pcIp + ":5005/command");
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                String json = "{\"cmd\":\"" + pcCommand + "\"}";
                conn.getOutputStream().write(json.getBytes());
                int responseCode = conn.getResponseCode();
                if (responseCode != 200) {
                    FeedbackProvider.speakAndToast(context, "PC responded with error code: " + responseCode);
                }
            } catch (Exception e) {
                e.printStackTrace();
                FeedbackProvider.speakAndToast(context, "Failed to send command to PC. Please check your connection and try again.");
            }
        }).start();
        FeedbackProvider.speakAndToast(context, "Sent command to PC: " + pcCommand);
    }

    @Override
    public java.util.List<String> getSuggestions() {
        return java.util.Arrays.asList(
            "pc open notepad",
            "pc lock",
            "pc shutdown",
            "pc open browser",
            "pc open website github.com",
            "pc play music",
            "pc volume up",
            "pc volume down",
            "pc mute",
            "pc screenshot",
            "pc open explorer",
            "pc sleep"
        );
    }
} 