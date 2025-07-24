package com.mvp.sarah;

import android.app.AlarmManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import androidx.core.app.NotificationCompat;

import java.util.Calendar;

public class AlarmReceiver extends BroadcastReceiver {

    private static final String CHANNEL_ID = "sara_alarm_channel";
    private static final String PREFS_NAME = "SaraAlarmPrefs";
    private static final int SNOOZE_MINUTES = 9;

    @Override
    public void onReceive(Context context, Intent intent) {
        // Create a notification channel (required for Android 8.0+)
        createNotificationChannel(context);

        // Close any open assistant UI
        Intent closeIntent = new Intent(SaraVoiceService.ACTION_CLOSE_ASSISTANT_UI)
            .setPackage(context.getPackageName());
        context.sendBroadcast(closeIntent);
        
        // Get alarm details from the intent
        String alarmId = intent.getStringExtra("alarm_id");
        String label = intent.getStringExtra("alarm_label");
        int[] recurringDays = intent.getIntArrayExtra("recurring_days");

        // Store the alarm ID for snooze functionality
        if (alarmId != null) {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString("last_triggered_alarm", alarmId).apply();
        }

        // Launch AlarmActivity
        Intent alarmActivityIntent = new Intent(context, AlarmActivity.class);
        alarmActivityIntent.putExtra("alarm_id", alarmId);
        alarmActivityIntent.putExtra("alarm_label", label);
        alarmActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(alarmActivityIntent);

        // Create notification actions
        Intent snoozeIntent = new Intent(context, AlarmActionReceiver.class);
        snoozeIntent.setAction("SNOOZE_ALARM");
        snoozeIntent.putExtra("alarm_id", alarmId);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
            context, 
            (alarmId + "_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Intent dismissIntent = new Intent(context, AlarmActionReceiver.class);
        dismissIntent.setAction("DISMISS_ALARM");
        dismissIntent.putExtra("alarm_id", alarmId);
        PendingIntent dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            (alarmId + "_dismiss").hashCode(),
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Create an intent to open the MainActivity when the notification is tapped
        Intent mainIntent = new Intent(context, MainActivity.class);
        mainIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
            context, 
            0, 
            mainIntent, 
            PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build the notification (as backup in case activity doesn't show)
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_assistant)
                .setContentTitle(label != null ? label : "Sara Alarm")
                .setContentText("Time to wake up!")
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(contentIntent, true)
                .setAutoCancel(true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)
                .addAction(android.R.drawable.ic_popup_reminder, "Snooze", snoozePendingIntent)
                .setOngoing(true) // Make the notification persistent
                .setTimeoutAfter(60 * 60 * 1000); // Auto-dismiss after 1 hour if not handled

        // Show the notification
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        int notificationId = alarmId != null ? alarmId.hashCode() : (int) System.currentTimeMillis();
        notificationManager.notify(notificationId, builder.build());

        // If this is a recurring alarm, schedule the next occurrence
        if (recurringDays != null && recurringDays.length > 0) {
            scheduleNextRecurringAlarm(context, alarmId, label, recurringDays);
        }
    }

    private void scheduleNextRecurringAlarm(Context context, String alarmId, String label, int[] recurringDays) {
        Calendar now = Calendar.getInstance();
        Calendar nextAlarm = Calendar.getInstance();
        nextAlarm.add(Calendar.DAY_OF_YEAR, 1); // Start checking from tomorrow
        nextAlarm.set(Calendar.SECOND, 0);
        nextAlarm.set(Calendar.MILLISECOND, 0);

        // Find the next occurrence
        boolean found = false;
        for (int i = 0; i < 7 && !found; i++) {
            for (int day : recurringDays) {
                if (nextAlarm.get(Calendar.DAY_OF_WEEK) == day) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                nextAlarm.add(Calendar.DAY_OF_YEAR, 1);
            }
        }

        if (found) {
            // Schedule the next alarm
            AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.putExtra("alarm_id", alarmId);
            intent.putExtra("alarm_label", label);
            intent.putExtra("recurring_days", recurringDays);

            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.hashCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarm.getTimeInMillis(),
                    pendingIntent
                );
            } else {
                alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    nextAlarm.getTimeInMillis(),
                    pendingIntent
                );
            }
        }
    }

    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            CharSequence name = "Sara Alarms";
            String description = "Channel for alarms set by Sara";
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, name, NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription(description);
            channel.enableVibration(true);
            channel.setVibrationPattern(new long[]{0, 500, 250, 500}); // Vibration pattern
            channel.setBypassDnd(true); // Bypass Do Not Disturb
            channel.setSound(null, null); // We'll handle sound through AlarmActivity

            NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
            notificationManager.createNotificationChannel(channel);
        }
    }
} 