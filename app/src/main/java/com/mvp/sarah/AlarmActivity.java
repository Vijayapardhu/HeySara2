package com.mvp.sarah;

import android.animation.Animator;
import android.animation.AnimatorInflater;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.TextClock;

public class AlarmActivity extends Activity {
    private MediaPlayer mediaPlayer;
    private PowerManager.WakeLock wakeLock;
    private ValueAnimator backgroundAnimator;
    private Animator clockPulseAnimator;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private Vibrator vibrator;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_alarm);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        // Keep screen on and show above lock screen
        getWindow().addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        );

        // Get alarm details from intent
        String label = getIntent().getStringExtra("alarm_label");
        String alarmId = getIntent().getStringExtra("alarm_id");

        // Set alarm label
        TextView labelView = findViewById(R.id.alarm_label);
        labelView.setText(label != null ? label : "Sara Alarm");
        labelView.setAlpha(0f);
        labelView.animate().alpha(1f).setDuration(1000).start();

        // Setup TextClock
        TextClock timeView = findViewById(R.id.current_time);
        timeView.setAlpha(0f);
        timeView.animate().alpha(1f).setDuration(1000).start();
        
        // Start clock pulse animation
        clockPulseAnimator = AnimatorInflater.loadAnimator(this, R.animator.pulse);
        clockPulseAnimator.setTarget(timeView);
        clockPulseAnimator.start();

        // Setup buttons with animations
        ImageButton dismissButton = findViewById(R.id.dismiss_button);
        ImageButton snoozeButton = findViewById(R.id.snooze_button);

        // Animate buttons in from bottom
        float translationY = 300f;
        dismissButton.setTranslationY(translationY);
        snoozeButton.setTranslationY(translationY);
        dismissButton.setAlpha(0f);
        snoozeButton.setAlpha(0f);

        dismissButton.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1000)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();

        snoozeButton.animate()
            .translationY(0f)
            .alpha(1f)
            .setDuration(1000)
            .setInterpolator(new AccelerateDecelerateInterpolator())
            .start();

        // Setup button click listeners with animations
        dismissButton.setOnClickListener(v -> {
            v.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction(this::dismissAlarm)
                        .start();
                })
                .start();
        });

        snoozeButton.setOnClickListener(v -> {
            performHapticFeedback();
            v.animate()
                .scaleX(0.9f)
                .scaleY(0.9f)
                .setDuration(100)
                .withEndAction(() -> {
                    v.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(100)
                        .withEndAction(this::snoozeAlarm)
                        .start();
                })
                .start();
        });

       /*  // Acquire wake lock
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
            "Sara:AlarmWakeLock"
        );
        wakeLock.acquire(10*60*1000L); // 10 minutes timeout
*/
        // Start playing alarm sound
        startAlarmSound();

        // Cancel any existing notification
        NotificationManager notificationManager = 
            (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        if (alarmId != null) {
            notificationManager.cancel(alarmId.hashCode());
        }

        // Start background animation
        startBackgroundAnimation();
    }

    private void startBackgroundAnimation() {
        View rootView = findViewById(android.R.id.content);
        backgroundAnimator = ValueAnimator.ofFloat(0f, 1f);
        backgroundAnimator.setDuration(3000);
        backgroundAnimator.setRepeatCount(ValueAnimator.INFINITE);
        backgroundAnimator.setRepeatMode(ValueAnimator.REVERSE);
        backgroundAnimator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            int color = (int) new ArgbEvaluator().evaluate(
                value,
                getResources().getColor(R.color.alarm_background_start, null),
                getResources().getColor(R.color.alarm_background_end, null)
            );
            rootView.setBackgroundColor(color);
        });
        backgroundAnimator.start();
    }

    private void startAlarmSound() {
        // Request audio focus for alarm
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build())
                .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT);
        }

        try {
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }

            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, alarmSound);
            
            AudioAttributes attributes = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
            mediaPlayer.setAudioAttributes(attributes);
            
            mediaPlayer.setLooping(true);
            mediaPlayer.prepare();
            mediaPlayer.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void dismissAlarm() {
        // Abandon audio focus
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (audioManager != null && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            if (audioManager != null) {
                audioManager.abandonAudioFocus(null);
            }
        }

        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (backgroundAnimator != null) {
            backgroundAnimator.cancel();
        }
        if(clockPulseAnimator != null) {
            clockPulseAnimator.cancel();
        }

        // Animate out before finishing
        View rootView = findViewById(android.R.id.content);
        rootView.animate()
            .alpha(0f)
            .setDuration(500)
            .withEndAction(this::finish)
            .start();
    }

    private void snoozeAlarm() {
        // Create snooze intent similar to AlarmReceiver
        Intent snoozeIntent = new Intent(this, AlarmActionReceiver.class);
        snoozeIntent.setAction("SNOOZE_ALARM");
        String alarmId = getIntent().getStringExtra("alarm_id");
        snoozeIntent.putExtra("alarm_id", alarmId);
        sendBroadcast(snoozeIntent);
        
        dismissAlarm(); // This will stop the sound and close the activity
    }

    private void performHapticFeedback() {
        if (vibrator.hasVibrator()) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createOneShot(50, VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                //deprecated in API 26 
                vibrator.vibrate(50);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Ensure audio focus is abandoned
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            if (audioManager != null && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            if (audioManager != null) {
                audioManager.abandonAudioFocus(null);
            }
        }
        
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (backgroundAnimator != null) {
            backgroundAnimator.cancel();
        }
        if(clockPulseAnimator != null) {
            clockPulseAnimator.cancel();
        }
    }
} 