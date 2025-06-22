package com.mvp.sara.handlers;

import android.content.Context;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class CalendarHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    public boolean canHandle(String command) { return command.contains("calendar"); }
    public void handle(Context c, String cmd) { FeedbackProvider.speakAndToast(c, "I can't access your calendar yet, but this feature is coming soon!"); }
    public List<String> getSuggestions() { return Arrays.asList("what's on my calendar", "add event to calendar"); }
} 