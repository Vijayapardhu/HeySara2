package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.telecom.TelecomManager;
import android.widget.Toast;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class VideoCallHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.startsWith("video call ") || lower.startsWith("start video call with ");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        String contact = lower.replaceFirst("video call ", "").replaceFirst("start video call with ", "").trim();
        if (contact.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please specify the contact to video call.");
            return;
        }
        // Try to launch a video call intent (using tel: URI with video)
        try {
            Intent intent = new Intent(Intent.ACTION_CALL);
            intent.setData(Uri.parse("tel:" + contact));
            intent.putExtra("android.telecom.extra.START_CALL_WITH_VIDEO_STATE", 3); // STATE_VIDEO_CALL
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Starting video call with " + contact);
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Could not start video call: " + e.getMessage());
        }
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("video call [contact]", "start video call with [contact]");
    }
} 