package com.mvp.sarah;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.SystemClock;
import android.util.Log;
import android.view.KeyEvent;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class TriggerDetectionService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "trigger_detection_channel";
    private static final int NOTIF_ID = 2001;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD_GRAVITY = 1.8F;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private long mShakeTimestamp;
    private int volumeDownCount = 0;
    private long lastVolumeDownTime = 0;
    private static final int VOLUME_PRESS_EMERGENCY_COUNT = 4;
    private static final long VOLUME_PRESS_WINDOW_MS = 2000;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sara Emergency & Shake Detection")
                .setContentText("Listening for shake or volume down presses...")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setOngoing(true)
                .build();
        startForeground(NOTIF_ID, notification);

        handler = new Handler();
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            if (accelerometer != null) {
                sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
            }
        }
        // Register for media button events for volume detection
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            audioManager.registerMediaButtonEventReceiver(
                new android.content.ComponentName(getPackageName(), TriggerDetectionService.class.getName()));
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (sensorManager != null) {
            sensorManager.unregisterListener(this);
        }
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
        float x = event.values[0];
        float y = event.values[1];
        float z = event.values[2];
        float gX = x / SensorManager.GRAVITY_EARTH;
        float gY = y / SensorManager.GRAVITY_EARTH;
        float gZ = z / SensorManager.GRAVITY_EARTH;
        float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
        if (gForce > SHAKE_THRESHOLD_GRAVITY) {
            final long now = SystemClock.elapsedRealtime();
            if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                return;
            }
            mShakeTimestamp = now;
            Log.d("TriggerDetectionService", "Shake detected! Starting SaraVoiceService.");
            Intent serviceIntent = new Intent(this, SaraVoiceService.class);
            serviceIntent.setAction("com.mvp.sarah.ACTION_START_COMMAND_LISTENING");
            startForegroundService(serviceIntent);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    // Volume button detection (requires accessibility or media session for global detection)
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    // To fully support global volume detection, you may need an accessibility service or listen for ACTION_MEDIA_BUTTON broadcasts.
    // For now, you can trigger emergency from MainActivity's onKeyDown as a fallback.

    // Helper for notification channel
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Sara Trigger Detection",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Detects shake and volume down for emergency");
            NotificationManager notificationManager = getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
} 