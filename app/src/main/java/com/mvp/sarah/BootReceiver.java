package com.mvp.sarah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import androidx.core.content.ContextCompat;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Intent shakeIntent = new Intent(context, ShakeDetectionService.class);
            ContextCompat.startForegroundService(context, shakeIntent);
        }
    }
} 