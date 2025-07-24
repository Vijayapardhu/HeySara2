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
import android.os.Build;
import android.os.IBinder;
import android.os.Vibrator;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;

public class ShakeDetectionService extends Service implements SensorEventListener {
    private static final String CHANNEL_ID = "shake_detection_channel";
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private static final float SHAKE_THRESHOLD = 12.0f;
    private long lastShakeTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();
        sensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
            sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
        createNotificationChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startForeground(1, buildNotification());
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
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            float x = event.values[0];
            float y = event.values[1];
            float z = event.values[2];
            float gX = x / SensorManager.GRAVITY_EARTH;
            float gY = y / SensorManager.GRAVITY_EARTH;
            float gZ = z / SensorManager.GRAVITY_EARTH;
            float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
            if (gForce > SHAKE_THRESHOLD) {
                long now = System.currentTimeMillis();
                if (now - lastShakeTime > 1000) { // 1 second debounce
                    lastShakeTime = now;
                    Log.d("ShakeDetectionService", "Shake detected!");
                    Toast.makeText(this, "Shake detected!", Toast.LENGTH_SHORT).show();
                    // Start SaraVoiceService with ACTION_START_COMMAND_LISTENING
                    Intent serviceIntent = new Intent(this, SaraVoiceService.class);
                    serviceIntent.setAction("com.mvp.sarah.ACTION_START_COMMAND_LISTENING");
                    Log.d("ShakeDetectionService", "Starting SaraVoiceService with ACTION_START_COMMAND_LISTENING");
                    ContextCompat.startForegroundService(this, serviceIntent);
                }
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "Shake Detection Service", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Shake Detection Active")
            .setContentText("Listening for shake gestures...")
            .setSmallIcon(R.drawable.ic_mic)
            .setOngoing(true)
            .build();
    }
} 