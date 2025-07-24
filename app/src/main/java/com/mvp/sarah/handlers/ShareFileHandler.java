package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class ShareFileHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    public interface FileShareCallback {
        void onShareFileRequested();
    }

    private static FileShareCallback callback;

    public static void setCallback(FileShareCallback cb) {
        callback = cb;
    }

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.equals("share file") || lower.equals("share a file") || lower.equals("send file");
    }

    @Override
    public void handle(Context context, String command) {
        Intent intent = new Intent(context, com.mvp.sarah.MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        intent.putExtra("share_file", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        context.startActivity(intent);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "share file",
            "share a file",
            "send file"
        );
    }
} 