package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OpenWebsiteHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "open google.com",
            "go to wikipedia.org"
    );

    // A simple regex to find something that looks like a domain name
    private static final Pattern URL_PATTERN = Pattern.compile(
            "([a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,}"
    );

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("open ") || command.startsWith("go to ") || command.endsWith("web");
    }

    @Override
    public void handle(Context context, String command) {
        // For 'open website lite', require the command to end with 'web'
        if (command.toLowerCase().contains("open website lite")) {
            if (!command.trim().toLowerCase().endsWith("web")) {
                FeedbackProvider.speakAndToast(context, "For open website lite, please end your command with 'web'. For example: open google web");
                return;
            }
        }
        Matcher matcher = URL_PATTERN.matcher(command);
        if (matcher.find()) {
            String url = matcher.group(0);
            if (!url.startsWith("http://") && !url.startsWith("https://")) {
                url = "https://" + url;
            }
            FeedbackProvider.speakAndToast(context, "Opening " + url);
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            browserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(browserIntent);
        } else {
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't find a valid website to open.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 