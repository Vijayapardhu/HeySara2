package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CallAnswerHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    
    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        // Accept more variants and ignore punctuation
        lowerCmd = lowerCmd.replaceAll("[!?.]", "").trim();
        return lowerCmd.matches(".*\\b(answer|accept|pick up|yes|reject|decline|hang up|no|ignore|dismiss)\\b.*");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        lowerCmd = lowerCmd.replaceAll("[!?.]", "").trim();
        android.util.Log.d("CallAnswerHandler", "handle called with: '" + command + "' (lowercase: '" + lowerCmd + "')");

        String action = null;
        if (lowerCmd.matches(".*\\b(answer|accept|pick up|yes)\\b.*")) {
            action = "answer";
            android.util.Log.d("CallAnswerHandler", "Detected ANSWER command");
        } else if (lowerCmd.matches(".*\\b(reject|decline|hang up|no|ignore|dismiss)\\b.*")) {
            action = "reject";
            android.util.Log.d("CallAnswerHandler", "Detected REJECT command");
        }

        if (action != null) {
            android.util.Log.d("CallAnswerHandler", "Sending call command: " + action);
            Intent intent = new Intent("com.mvp.sarah.CALL_COMMAND");
            intent.putExtra("command", action);
            context.sendBroadcast(intent);
            FeedbackProvider.speakAndToast(context, action.equals("answer") ? "Answering call" : "Rejecting call");
        } else {
            android.util.Log.w("CallAnswerHandler", "No action to send for command: '" + command + "'");
            FeedbackProvider.speakAndToast(context, "Sorry, I didn't understand. Please say 'answer' or 'reject'.");
        }
    }
    
    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "answer call",
            "accept call", 
            "pick up call",
            "reject call",
            "decline call",
            "hang up call",
            "no"
        );
    }
} 