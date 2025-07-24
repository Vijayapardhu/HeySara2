package com.mvp.sarah;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.EditText;

public class TestTimerDialogActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final EditText input = new EditText(this);
        input.setHint("Enter test duration in minutes");

        new AlertDialog.Builder(this)
            .setTitle("Set Test Timer")
            .setMessage("How many minutes is your test?")
            .setView(input)
            .setCancelable(false)
            .setPositiveButton("OK", (dialog, which) -> {
                String value = input.getText().toString();
                int minutes = 0;
                try { minutes = Integer.parseInt(value); } catch (Exception ignored) {}
                Intent result = new Intent("com.mvp.sarah.TEST_TIMER_SET");
                result.putExtra("minutes", minutes);
                sendBroadcast(result);
                finish();
            })
            .show();
    }
} 