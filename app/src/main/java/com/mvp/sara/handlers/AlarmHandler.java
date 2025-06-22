package com.mvp.sara.handlers;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.provider.AlarmClock;
import com.mvp.sara.AlarmReceiver;
import com.mvp.sara.CommandHandler;
import com.mvp.sara.CommandRegistry;
import com.mvp.sara.FeedbackProvider;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlarmHandler implements CommandHandler, CommandRegistry.SuggestionProvider {

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        return lowerCmd.contains("alarm");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);

        if (lowerCmd.startsWith("set") || lowerCmd.startsWith("create")) {
            setAlarm(context, command);
        } else if (lowerCmd.startsWith("delete") || lowerCmd.startsWith("remove")) {
            deleteAlarm(context);
        } else if (lowerCmd.startsWith("check") || lowerCmd.startsWith("what") || lowerCmd.startsWith("show")) {
            checkNextAlarm(context);
        } else {
            FeedbackProvider.speakAndToast(context, "I can set, check, or delete alarms. For example: set an alarm for 7 am.");
        }
    }

    private void setAlarm(Context context, String command) {
        // Regex to find time patterns like "7:30 am", "8 pm", "9"
        Pattern pattern = Pattern.compile("(\\d{1,2})(:(\\d{2}))?(\\s*(am|pm))?");
        Matcher matcher = pattern.matcher(command);

        if (matcher.find()) {
            try {
                int hour = Integer.parseInt(matcher.group(1));
                int minute = (matcher.group(3) != null) ? Integer.parseInt(matcher.group(3)) : 0;
                String ampm = matcher.group(5);

                if (ampm != null && ampm.equalsIgnoreCase("pm") && hour < 12) {
                    hour += 12;
                }
                if (ampm != null && ampm.equalsIgnoreCase("am") && hour == 12) {
                    hour = 0; // Midnight case
                }

                Calendar calendar = Calendar.getInstance();
                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                // If the time is in the past, set it for the next day
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }

                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                Intent alarmIntent = new Intent(context, AlarmReceiver.class);
                alarmIntent.putExtra("alarm_message", "Time to wake up!");
                
                // Use a unique request code for each alarm to avoid overwriting
                int requestCode = (int) System.currentTimeMillis();
                android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(context, requestCode, alarmIntent, android.app.PendingIntent.FLAG_UPDATE_CURRENT | android.app.PendingIntent.FLAG_IMMUTABLE);
                
                // Set the alarm
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), pendingIntent);

                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
                FeedbackProvider.speakAndToast(context, "OK, I've set an alarm for " + sdf.format(calendar.getTime()));

            } catch (Exception e) {
                android.util.Log.e("AlarmHandler", "Error setting alarm in background", e);
                FeedbackProvider.speakAndToast(context, "Sorry, there was an error setting the alarm.");
            }
        } else {
            FeedbackProvider.speakAndToast(context, "Please specify a time for the alarm.");
        }
    }
    
    private void deleteAlarm(Context context) {
        Intent intent = new Intent(AlarmClock.ACTION_SHOW_ALARMS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Opening your alarms. You can delete one from there.");
        } else {
            FeedbackProvider.speakAndToast(context, "I couldn't find your clock app to show alarms.");
        }
    }

    private void checkNextAlarm(Context context) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        AlarmManager.AlarmClockInfo nextAlarm = alarmManager.getNextAlarmClock();

        if (nextAlarm != null) {
            long triggerTime = nextAlarm.getTriggerTime();
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a 'on' EEE, MMM d", Locale.getDefault());
            String formattedTime = sdf.format(triggerTime);
            FeedbackProvider.speakAndToast(context, "Your next alarm is set for " + formattedTime);
        } else {
            FeedbackProvider.speakAndToast(context, "You don't have any upcoming alarms set.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "set an alarm for 7:30 am",
            "check my next alarm",
            "delete an alarm"
        );
    }
} 