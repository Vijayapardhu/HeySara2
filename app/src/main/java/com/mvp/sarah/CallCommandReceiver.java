package com.mvp.sarah;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telecom.TelecomManager;
import android.os.Build;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import android.content.pm.PackageManager;

public class CallCommandReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && "com.mvp.sarah.CALL_COMMAND".equals(intent.getAction())) {
            String command = intent.getStringExtra("command");
            if (command == null) return;

            TelecomManager telecomManager = (TelecomManager) context.getSystemService(Context.TELECOM_SERVICE);

            if (ActivityCompat.checkSelfPermission(context, android.Manifest.permission.ANSWER_PHONE_CALLS) != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(context, "Missing permission to answer calls", Toast.LENGTH_SHORT).show();
                return;
            }

            if ("answer".equals(command)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    telecomManager.acceptRingingCall();
                    Toast.makeText(context, "Answering call...", Toast.LENGTH_SHORT).show();
                }
            } else if ("reject".equals(command)) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    telecomManager.endCall();
                    Toast.makeText(context, "Rejecting call...", Toast.LENGTH_SHORT).show();
                }
            }
        }
    }
} 