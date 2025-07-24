package com.mvp.sarah;

import android.content.Context;

public interface CommandHandler {
    boolean canHandle(String command);
    void handle(Context context, String command);
} 