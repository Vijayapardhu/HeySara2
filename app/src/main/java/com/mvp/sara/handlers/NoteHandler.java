package com.mvp.sara.handlers;

import android.content.Context;
import android.widget.Toast;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class NoteHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> notes = new ArrayList<>();
    private static final List<String> COMMANDS = Arrays.asList(
            "take a note [note content]",
            "add a note [note content]",
            "read my notes",
            "show my notes"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("note");
    }

    @Override
    public void handle(Context context, String command) {
        if (command.startsWith("take a note") || command.startsWith("add a note")) {
            String note = command.replace("take a note", "").replace("add a note", "").trim();
            if (!note.isEmpty()) {
                notes.add(note);
                FeedbackProvider.speakAndToast(context, "Note saved: " + note);
            } else {
                FeedbackProvider.speakAndToast(context, "What should the note say?");
            }
        } else if (command.contains("read my notes") || command.contains("show my notes")) {
            if (notes.isEmpty()) {
                FeedbackProvider.speakAndToast(context, "You have no notes.");
            } else {
                String allNotes = notes.stream().map(n -> (notes.indexOf(n) + 1) + ". " + n).collect(Collectors.joining(". "));
                FeedbackProvider.speakAndToast(context, "Here are your notes: " + allNotes, Toast.LENGTH_LONG);
            }
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 