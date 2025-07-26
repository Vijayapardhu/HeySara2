package com.mvp.sarah;

import androidx.fragment.app.FragmentActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;

public class EmergencyStopActivity extends FragmentActivity {
    private SeekBar slider;
    private boolean stopped = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Fullscreen, no status bar
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_emergency_stop);

        slider = findViewById(R.id.slider_stop);
        TextView message = findViewById(R.id.text_emergency_message);
        message.setText("Emergency services are being called...\nSlide to stop calling");

        slider.setMax(100);
        slider.setProgress(0);
        slider.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress >= 100 && !stopped) {
                    stopped = true;
                    EmergencyActions.stopEmergencySequence(EmergencyStopActivity.this);
                    Toast.makeText(EmergencyStopActivity.this, "Emergency calling stopped.", Toast.LENGTH_LONG).show();
                    // Close after a short delay
                    new Handler().postDelayed(() -> finish(), 500);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // Prevent back button
            }
        });
    }

    // Prevent touches outside the slider
    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        View v = findViewById(R.id.slider_stop);
        if (v != null) {
            int[] location = new int[2];
            v.getLocationOnScreen(location);
            int x = (int) ev.getRawX();
            int y = (int) ev.getRawY();
            if (x >= location[0] && x <= location[0] + v.getWidth() && y >= location[1] && y <= location[1] + v.getHeight()) {
                return super.dispatchTouchEvent(ev);
            } else {
                // Ignore touches outside the slider
                return true;
            }
        }
        return super.dispatchTouchEvent(ev);
    }
} 