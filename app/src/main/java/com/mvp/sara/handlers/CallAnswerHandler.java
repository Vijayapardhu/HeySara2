package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class CallAnswerHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    
    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        boolean canHandle = lowerCmd.contains("answer") || 
               lowerCmd.contains("reject") ||
               lowerCmd.contains("decline") ||
               lowerCmd.contains("accept") ||
               lowerCmd.contains("pick up") ||
               lowerCmd.contains("hang up") ||
               lowerCmd.equals("no");
        
        android.util.Log.d("CallAnswerHandler", "canHandle called with: '" + command + "' (lowercase: '" + lowerCmd + "') - result: " + canHandle);
        return canHandle;
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        android.util.Log.d("CallAnswerHandler", "handle called with: '" + command + "' (lowercase: '" + lowerCmd + "')");
        
        String action = "";
        
        if (lowerCmd.contains("answer") || lowerCmd.contains("accept") || lowerCmd.contains("pick up")) {
            action = "answer";
            android.util.Log.d("CallAnswerHandler", "Detected ANSWER command");
        } else if (lowerCmd.contains("reject") || lowerCmd.contains("decline") || lowerCmd.contains("hang up") || lowerCmd.equals("no")) {
            action = "reject";
            android.util.Log.d("CallAnswerHandler", "Detected REJECT command");
        } else {
            android.util.Log.w("CallAnswerHandler", "No action detected for command: '" + command + "'");
        }
        
        if (!action.isEmpty()) {
            android.util.Log.d("CallAnswerHandler", "Sending call command: " + action);
            
            // Send broadcast to CallDetectionService
            Intent intent = new Intent("com.mvp.sara.CALL_COMMAND");
            intent.putExtra("command", action);
            context.sendBroadcast(intent);
            
            FeedbackProvider.speakAndToast(context, action.equals("answer") ? "Answering call" : "Rejecting call");
        } else {
            android.util.Log.w("CallAnswerHandler", "No action to send for command: '" + command + "'");
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