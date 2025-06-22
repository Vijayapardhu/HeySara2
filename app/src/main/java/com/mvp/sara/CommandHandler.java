package com.mvp.sara;

import android.content.Context;

public interface CommandHandler {
    boolean canHandle(String command);
    void handle(Context context, String command);
} 