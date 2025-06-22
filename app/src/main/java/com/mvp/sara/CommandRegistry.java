package com.mvp.sara;

import android.content.Context;
import android.util.Log;
import android.widget.Toast;
import com.mvp.sara.handlers.CallAnswerHandler;
import java.util.ArrayList;
import java.util.List;

public class CommandRegistry {
    private static final List<CommandHandler> handlers = new ArrayList<>();
    private static FallbackHandler fallbackHandler = new FallbackHandler();

    public static void register(CommandHandler handler) {
        handlers.add(handler);
    }

    public static boolean handleCommand(Context context, String command) {
        String normalized = command.trim().toLowerCase();
        Log.d("CommandRegistry", "Processing command: '" + command + "' (normalized: '" + normalized + "')");
        
        // First, check if this is a call-related command and prioritize CallAnswerHandler
        if (isCallCommand(normalized)) {
            for (CommandHandler handler : handlers) {
                if (handler instanceof CallAnswerHandler) {
                    Log.d("CommandRegistry", "Found CallAnswerHandler, processing call command");
                    handler.handle(context, command);
                    return true;
                }
            }
        }
        
        // Process other commands normally
        for (CommandHandler handler : handlers) {
            if (handler.canHandle(normalized)) {
                Log.d("CommandRegistry", "Handler " + handler.getClass().getSimpleName() + " can handle command");
                handler.handle(context, command);
                return true;
            }
        }
        
        // Fallback: suggest similar commands
        Log.d("CommandRegistry", "No handler found for command: '" + command + "'");
        fallbackHandler.handle(context, command);
        return false;
    }
    
    private static boolean isCallCommand(String command) {
        return command.contains("answer") || 
               command.contains("reject") ||
               command.contains("decline") ||
               command.contains("accept") ||
               command.contains("pick up") ||
               command.contains("hang up");
    }

    // Fallback handler for suggestions
    public static class FallbackHandler implements CommandHandler {
        @Override
        public boolean canHandle(String command) {
            return true;
        }
        @Override
        public void handle(Context context, String command) {
            List<String> suggestions = new ArrayList<>();
            String normalized = command.trim().toLowerCase();
            for (CommandHandler handler : handlers) {
                if (handler instanceof SuggestionProvider) {
                    for (String pattern : ((SuggestionProvider) handler).getSuggestions()) {
                        if (pattern.contains(normalized) || normalized.contains(pattern)) {
                            suggestions.add(pattern);
                        }
                    }
                }
            }
            if (!suggestions.isEmpty()) {
                FeedbackProvider.speakAndToast(context, "Did you mean: " + suggestions.get(0), Toast.LENGTH_LONG);
            } else {
                FeedbackProvider.speakAndToast(context, "Sorry, I didn't understand that!");
            }
        }
    }

    // Interface for handlers to provide suggestions
    public interface SuggestionProvider {
        List<String> getSuggestions();
    }
} 