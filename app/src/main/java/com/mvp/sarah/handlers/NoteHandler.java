package com.mvp.sarah.handlers;

import android.content.Context;
import android.widget.Toast;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;

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
        String lower = command.toLowerCase();
        return lower.contains("note") || lower.contains("take a note") || lower.contains("remember");
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
        } else if (command.toLowerCase().contains("delete all notes")) {
            notes.clear();
            FeedbackProvider.speakAndToast(context, "All notes deleted.");
        } else if (command.toLowerCase().matches(".*delete note( number)? \\d+.*")) {
            // Delete by number
            String[] words = command.split(" ");
            int num = -1;
            for (int i = 0; i < words.length; i++) {
                if (words[i].matches("\\d+")) {
                    num = Integer.parseInt(words[i]);
                    break;
                }
            }
            if (num > 0 && num <= notes.size()) {
                String removed = notes.remove(num - 1);
                FeedbackProvider.speakAndToast(context, "Deleted note " + num + ": " + removed);
            } else {
                FeedbackProvider.speakAndToast(context, "Note number not found.");
            }
        } else if (command.toLowerCase().contains("delete note")) {
            // Delete by content
            String content = command.toLowerCase().replace("delete note", "").trim();
            int idx = -1;
            for (int i = 0; i < notes.size(); i++) {
                if (notes.get(i).toLowerCase().contains(content)) {
                    idx = i;
                    break;
                }
            }
            if (idx != -1) {
                String removed = notes.remove(idx);
                FeedbackProvider.speakAndToast(context, "Deleted note: " + removed);
            } else {
                FeedbackProvider.speakAndToast(context, "No matching note found to delete.");
            }
        } else {
            FeedbackProvider.speakAndToast(context, "Note feature coming soon! (You said: " + command + ")");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 