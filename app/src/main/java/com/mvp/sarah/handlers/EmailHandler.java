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

public class EmailHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "email john@example.com subject Hello body How are you?",
            "send an email to jane@example.com"
    );

    // Regex to capture address, subject, and body
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "email(?: to)? ([^ ]+) subject (.+) body (.+)", 
            Pattern.CASE_INSENSITIVE
    );

    // State for conversational flow
    private static String pendingEmailTo = null;
    private static String pendingEmailSubject = null;
    private static boolean awaitingSubject = false;
    private static boolean awaitingBody = false;

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("email") || command.startsWith("send an email");
    }

    @Override
    public void handle(Context context, String command) {
        Matcher matcher = EMAIL_PATTERN.matcher(command);
        if (matcher.find()) {
            String to = matcher.group(1);
            String subject = matcher.group(2);
            String body = matcher.group(3);
            sendEmail(context, to, subject, body);
            return;
        }

        // Step 1: If awaiting subject
        if (awaitingSubject && pendingEmailTo != null) {
            pendingEmailSubject = command.trim();
            awaitingSubject = false;
            awaitingBody = true;
            FeedbackProvider.speakAndToast(context, "What is the message?");
            return;
        }

        // Step 2: If awaiting body
        if (awaitingBody && pendingEmailTo != null && pendingEmailSubject != null) {
            String body = command.trim();
            sendEmail(context, pendingEmailTo, pendingEmailSubject, body);
            // Reset state
            pendingEmailTo = null;
            pendingEmailSubject = null;
            awaitingBody = false;
            return;
        }

        // Step 3: If only address is provided
        Pattern toPattern = Pattern.compile("(?:email|send an email to) ([^ ]+)", Pattern.CASE_INSENSITIVE);
        Matcher toMatcher = toPattern.matcher(command);
        if (toMatcher.find()) {
            pendingEmailTo = toMatcher.group(1);
            pendingEmailSubject = null;
            awaitingSubject = true;
            awaitingBody = false;
            FeedbackProvider.speakAndToast(context, "What is the subject?");
            return;
        }

        FeedbackProvider.speakAndToast(context, "Please say 'email [address] subject [subject] body [message]', or just 'email [address]' to start.");
    }

    private void sendEmail(Context context, String to, String subject, String body) {
        Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts("mailto", to, null));
        emailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
        emailIntent.putExtra(Intent.EXTRA_TEXT, body);
        emailIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(Intent.createChooser(emailIntent, "Send email..."));
        FeedbackProvider.speakAndToast(context, "Preparing your email to " + to + ".");
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 