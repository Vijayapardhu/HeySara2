package com.mvp.sara.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class PlayMusicHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        return command.contains("play music") || command.contains("start music");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Playing music.");
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_APP_MUSIC);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        } else {
            FeedbackProvider.speakAndToast(context, "I couldn't find a music app to open.");
        }
    }
    
    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("play music", "start music");
    }
} 