package com.mvp.sara.handlers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;
import android.widget.Toast;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Calendar;
import java.util.Arrays;
import java.util.List;

public class ReminderHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "remind me to [task] in [number] minutes",
            "remind me to [task] in [number] hours",
            "remind me to [task] at [time]"
    );

    @Override
    public boolean canHandle(String command) {
        return command.startsWith("remind me to ");
    }

    @Override
    public void handle(Context context, String command) {
        // Support: 'remind me to [task] in [number] minutes/hours' and 'remind me to [task] at [HH:mm]'
        Pattern inPattern = Pattern.compile("remind me to (.+) in (\\d+) (minutes|hours)");
        Pattern atPattern = Pattern.compile("remind me to (.+) at (\\d{1,2}):(\\d{2})");
        Matcher inMatcher = inPattern.matcher(command);
        Matcher atMatcher = atPattern.matcher(command);

        if (inMatcher.matches()) {
            String task = inMatcher.group(1);
            int amount = Integer.parseInt(inMatcher.group(2));
            String unit = inMatcher.group(3);
            long duration = unit.equals("hours") ? amount * 60 * 60 * 1000 : amount * 60 * 1000;
            long triggerAtMillis = SystemClock.elapsedRealtime() + duration;
            scheduleReminder(context, task, triggerAtMillis);
            FeedbackProvider.speakAndToast(context, "Reminder set for " + amount + " " + unit + ": " + task);
        } else if (atMatcher.matches()) {
            String task = atMatcher.group(1);
            int hour = Integer.parseInt(atMatcher.group(2));
            int minute = Integer.parseInt(atMatcher.group(3));
            Calendar now = Calendar.getInstance();
            Calendar target = (Calendar) now.clone();
            target.set(Calendar.HOUR_OF_DAY, hour);
            target.set(Calendar.MINUTE, minute);
            target.set(Calendar.SECOND, 0);
            target.set(Calendar.MILLISECOND, 0);
            if (target.before(now)) {
                target.add(Calendar.DAY_OF_MONTH, 1); // Next day
            }
            long triggerAtMillis = SystemClock.elapsedRealtime() + (target.getTimeInMillis() - now.getTimeInMillis());
            scheduleReminder(context, task, triggerAtMillis);
            FeedbackProvider.speakAndToast(context, "Reminder set for " + String.format("%02d:%02d", hour, minute) + ": " + task);
        } else {
            FeedbackProvider.speakAndToast(context, "Say: 'remind me to [task] in [number] minutes/hours' or 'at [time]'", Toast.LENGTH_LONG);
        }
    }

    private void scheduleReminder(Context context, String task, long triggerAtMillis) {
        Intent intent = new Intent(context, ReminderReceiver.class);
        intent.putExtra("task", task);
        PendingIntent pi = PendingIntent.getBroadcast(context, (int) System.currentTimeMillis(), intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAtMillis, pi);
    }

    // ReminderReceiver to show a toast when the alarm triggers
    public static class ReminderReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String task = intent.getStringExtra("task");
            FeedbackProvider.init(context); // Ensure TTS is ready
            FeedbackProvider.speakAndToast(context, "Reminder: " + task, Toast.LENGTH_LONG);
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 