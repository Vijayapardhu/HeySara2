// WeatherHandler.java
package com.mvp.sara.handlers;

import android.content.Context;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class WeatherHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    public boolean canHandle(String command) { return command.contains("weather") || command.contains("rain"); }
    public void handle(Context c, String cmd) { FeedbackProvider.speakAndToast(c, "I can't check the weather yet, but I'm learning how!"); }
    public List<String> getSuggestions() { return Arrays.asList("what's the weather", "is it going to rain"); }
} 