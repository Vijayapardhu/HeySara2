package com.mvp.sarah;

import android.app.AlarmManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.widget.Toast;

import com.mvp.sarah.handlers.AlarmHandler;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

public class AlarmActionReceiver extends BroadcastReceiver {
    private static final String PREFS_NAME = "SaraAlarmPrefs";

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        String alarmId = intent.getStringExtra("alarm_id");
        if (action == null || alarmId == null) return;

        if (action.equals("SNOOZE_ALARM")) {
            snoozeAlarm(context, alarmId);
        } else if (action.equals("DISMISS_ALARM")) {
            dismissAlarm(context, alarmId);
        }
    }

    private void snoozeAlarm(Context context, String alarmId) {
        // Set a new alarm for 9 minutes from now
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 9);
        long snoozeTime = calendar.getTimeInMillis();

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.putExtra("alarm_id", alarmId + "_snooze");
        alarmIntent.putExtra("alarm_label", "Snoozed Alarm");
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                (alarmId + "_snooze").hashCode(),
                alarmIntent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE
        );
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
            );
        } else {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    snoozeTime,
                    pendingIntent
            );
        }
        SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
        Toast.makeText(context, "Alarm snoozed until " + sdf.format(calendar.getTime()), Toast.LENGTH_SHORT).show();
    }

    private void dismissAlarm(Context context, String alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                alarmId.hashCode(),
                intent,
                android.app.PendingIntent.FLAG_NO_CREATE | android.app.PendingIntent.FLAG_IMMUTABLE
        );
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
        Toast.makeText(context, "Alarm dismissed", Toast.LENGTH_SHORT).show();
    }
} 