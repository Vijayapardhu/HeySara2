package com.mvp.sarah.handlers;

import android.app.AlarmManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.provider.AlarmClock;
import android.text.TextUtils;
import android.widget.Toast;
import com.mvp.sarah.AlarmReceiver;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import org.json.JSONArray;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AlarmHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final String PREFS_NAME = "SaraAlarmPrefs";
    private static final String ALARMS_KEY = "saved_alarms";
    
    // Patterns for natural language processing
    private static final Pattern TIME_PATTERN = Pattern.compile(
        "(\\d{1,2})(:(\\d{2}))?(\\s*(am|pm))?|" +
        "(\\d{1,2})\\s*o'clock|" +
        "half past (\\d{1,2})|" +
        "quarter past (\\d{1,2})|" +
        "quarter to (\\d{1,2})"
    );
    
    private static final Pattern DAYS_PATTERN = Pattern.compile(
        "every\\s+(monday|tuesday|wednesday|thursday|friday|saturday|sunday|day|weekday|weekend)",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern LABEL_PATTERN = Pattern.compile(
        "called\\s+\"([^\"]+)\"|label\\s+\"([^\"]+)\"|named\\s+\"([^\"]+)\"",
        Pattern.CASE_INSENSITIVE
    );

    @Override
    public boolean canHandle(String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);
        // Only match if it's clearly about alarms or timers
        return lowerCmd.contains("alarm") ||
               lowerCmd.contains("wake me") ||
               lowerCmd.contains("remind me") ||
               lowerCmd.contains("cancel all alarms") ||
               lowerCmd.matches(".*set (an )?alarm.*") ||
               lowerCmd.matches(".*set (a )?timer.*");
    }

    @Override
    public void handle(Context context, String command) {
        String lowerCmd = command.toLowerCase(Locale.ROOT);

        if (lowerCmd.contains("snooze")) {
            snoozeAlarm(context);
        } else if (lowerCmd.startsWith("set") || lowerCmd.startsWith("create") || lowerCmd.contains("wake me")) {
            setAlarm(context, command);
        } else if (lowerCmd.startsWith("delete") || lowerCmd.startsWith("remove") || lowerCmd.contains("cancel")) {
            if (lowerCmd.contains("all")) {
                deleteAllAlarms(context);
            } else {
                deleteAlarm(context, command);
            }
        } else if (lowerCmd.startsWith("check") || lowerCmd.startsWith("what") || lowerCmd.startsWith("show") || lowerCmd.contains("list")) {
            checkAlarms(context);
        } else {
            FeedbackProvider.speakAndToast(context, 
                "I can help you with alarms. Try saying:\n" +
                "- Set an alarm for 7 am every weekday\n" +
                "- Set an alarm called \"Gym time\" for 6:30 am\n" +
                "- Show my alarms\n" +
                "- Delete the gym alarm\n" +
                "- Snooze alarm"
            );
        }
    }

    private void setAlarm(Context context, String command) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            if (!alarmManager.canScheduleExactAlarms()) {
                FeedbackProvider.speakAndToast(context, "I need permission to schedule exact alarms. Please grant it in the settings.", Toast.LENGTH_LONG);
                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
                return;
            }
        }

        try {
            // Extract time
            Matcher timeMatcher = TIME_PATTERN.matcher(command);
            if (!timeMatcher.find()) {
                FeedbackProvider.speakAndToast(context, "Please specify a time for the alarm.");
                return;
            }

            Calendar calendar = Calendar.getInstance();
            int hour = 0;
            int minute = 0;

            // Parse different time formats
            if (timeMatcher.group(1) != null) {
                // Standard time format (e.g., 7:30 am, 14:30)
                hour = Integer.parseInt(timeMatcher.group(1));
                minute = (timeMatcher.group(3) != null) ? Integer.parseInt(timeMatcher.group(3)) : 0;
                
                String lowerCmd = command.toLowerCase(Locale.ROOT);
                boolean isPm = lowerCmd.contains("pm") || lowerCmd.contains("evening") || lowerCmd.contains("afternoon");
                boolean isAm = lowerCmd.contains("am") || lowerCmd.contains("morning");

                // Only apply 12-hour logic if the hour is in the 1-12 range
                if (hour > 0 && hour < 13) {
                    if (isPm && hour < 12) {
                        hour += 12; // e.g., 3pm becomes 15
                    } else if (isAm && hour == 12) {
                        hour = 0; // 12am (midnight) becomes 0
                    }
                }
            } else if (timeMatcher.group(6) != null) {
                // O'clock format
                hour = Integer.parseInt(timeMatcher.group(6));
            } else if (timeMatcher.group(7) != null) {
                // Half past format
                hour = Integer.parseInt(timeMatcher.group(7));
                minute = 30;
            } else if (timeMatcher.group(8) != null) {
                // Quarter past format
                hour = Integer.parseInt(timeMatcher.group(8));
                minute = 15;
            } else if (timeMatcher.group(9) != null) {
                // Quarter to format
                hour = Integer.parseInt(timeMatcher.group(9)) - 1;
                minute = 45;
            }

                calendar.set(Calendar.HOUR_OF_DAY, hour);
                calendar.set(Calendar.MINUTE, minute);
                calendar.set(Calendar.SECOND, 0);

                // If the time is in the past, set it for the next day
                if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
                    calendar.add(Calendar.DAY_OF_YEAR, 1);
                }

            // Extract recurring days
            Matcher daysMatcher = DAYS_PATTERN.matcher(command);
            int[] recurringDays = null;
            if (daysMatcher.find()) {
                String dayPattern = daysMatcher.group(1).toLowerCase();
                recurringDays = parseRecurringDays(dayPattern);
            }

            // Extract label
            Matcher labelMatcher = LABEL_PATTERN.matcher(command);
            String label = null;
            if (labelMatcher.find()) {
                label = labelMatcher.group(1);
                if (label == null) label = labelMatcher.group(2);
                if (label == null) label = labelMatcher.group(3);
            }

            // Generate unique ID for the alarm
            String alarmId = UUID.randomUUID().toString();

            // Set the actual alarm first
            setSystemAlarm(context, alarmId, calendar.getTimeInMillis(), label, recurringDays);
            
            // Save alarm details only after system alarm is set successfully
            saveAlarmToPrefs(context, alarmId, calendar.getTimeInMillis(), label, recurringDays);

            // Prepare feedback message
                SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            StringBuilder feedback = new StringBuilder("OK, I've set an alarm for " + sdf.format(calendar.getTime()));
            
            if (label != null) {
                feedback.append(" labeled \"").append(label).append("\"");
            }
            
            if (recurringDays != null) {
                feedback.append(" recurring ").append(getRecurringDaysDescription(recurringDays));
            }

            FeedbackProvider.speakAndToast(context, feedback.toString());

            } catch (Exception e) {
            android.util.Log.e("AlarmHandler", "Error setting alarm", e);
                FeedbackProvider.speakAndToast(context, "Sorry, there was an error setting the alarm.");
            }
    }

    private void setSystemAlarm(Context context, String alarmId, long timeInMillis, String label, int[] recurringDays) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent alarmIntent = new Intent(context, AlarmReceiver.class);
        alarmIntent.putExtra("alarm_id", alarmId);
        alarmIntent.putExtra("alarm_label", label != null ? label : "Wake up!");
        if (recurringDays != null) {
            alarmIntent.putExtra("recurring_days", recurringDays);
        }

        int flags = android.app.PendingIntent.FLAG_UPDATE_CURRENT;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
        }

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            alarmIntent,
            flags
        );

        // For all alarms, use setExactAndAllowWhileIdle on modern Android.
        // The AlarmReceiver will handle rescheduling for recurring alarms.
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            );
        } else {
            alarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                timeInMillis,
                pendingIntent
            );
        }
    }

    private void saveAlarmToPrefs(Context context, String alarmId, long timeInMillis, String label, int[] recurringDays) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        // Use a Set to store alarm IDs for easy add/remove
        Set<String> alarmIds = new HashSet<>(prefs.getStringSet(ALARMS_KEY, new HashSet<>()));
        alarmIds.add(alarmId);
        
        SharedPreferences.Editor editor = prefs.edit();
        editor.putStringSet(ALARMS_KEY, alarmIds);

        // Store individual alarm details
        JSONObject alarmDetails = new JSONObject();
        try {
            alarmDetails.put("time", timeInMillis);
            alarmDetails.put("label", label);
            if (recurringDays != null) {
                alarmDetails.put("recurringDays", new JSONArray(recurringDays));
            }
        } catch (Exception e) {
            // handle exception
        }
        editor.putString("alarm_" + alarmId, alarmDetails.toString());
        editor.apply();
    }

    private void deleteAlarm(Context context, String command) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> alarmIds = prefs.getStringSet(ALARMS_KEY, new HashSet<>());
        if (alarmIds.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "You have no alarms to delete.");
            return;
        }

        String alarmToDeleteId = null;
        String alarmToDeleteLabel = null;

        // Find the alarm to delete by its label
        for (String id : alarmIds) {
            String alarmJson = prefs.getString("alarm_" + id, null);
            if (alarmJson != null) {
                try {
                    JSONObject details = new JSONObject(alarmJson);
                    String label = details.optString("label");
                    if (label != null && command.toLowerCase().contains(label.toLowerCase())) {
                        alarmToDeleteId = id;
                        alarmToDeleteLabel = label;
                        break;
                    }
                } catch (Exception e) {
                    // handle
                }
            }
        }
        
        if (alarmToDeleteId != null) {
            // Cancel the system alarm
            cancelSystemAlarm(context, alarmToDeleteId);
            
            // Remove from preferences
            alarmIds.remove(alarmToDeleteId);
            SharedPreferences.Editor editor = prefs.edit();
            editor.putStringSet(ALARMS_KEY, alarmIds);
            editor.remove("alarm_" + alarmToDeleteId);
            editor.apply();
            
            FeedbackProvider.speakAndToast(context, "OK, I've deleted the alarm for " + alarmToDeleteLabel);
        } else {
            FeedbackProvider.speakAndToast(context, "I couldn't find an alarm with that name. You can say 'show my alarms' to see them.");
        }
    }

    private void deleteAllAlarms(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        Set<String> alarmIds = new HashSet<>(prefs.getStringSet(ALARMS_KEY, new HashSet<>()));

        if (alarmIds.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "You don't have any alarms set.");
            return;
        }
        
        for (String id : alarmIds) {
            cancelSystemAlarm(context, id);
            prefs.edit().remove("alarm_" + id).apply();
        }
        
        prefs.edit().remove(ALARMS_KEY).apply();
        
        FeedbackProvider.speakAndToast(context, "All of your alarms have been removed.");
    }

    private void cancelSystemAlarm(Context context, String alarmId) {
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent intent = new Intent(context, AlarmReceiver.class);
        int flags = android.app.PendingIntent.FLAG_NO_CREATE;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            flags |= android.app.PendingIntent.FLAG_IMMUTABLE;
        }
        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
            context,
            alarmId.hashCode(),
            intent,
            flags
        );
        
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    private void checkAlarms(Context context) {
        try {
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            Set<String> alarmIds = prefs.getStringSet(ALARMS_KEY, new HashSet<>());

            if (alarmIds == null || alarmIds.isEmpty()) {
                FeedbackProvider.speakAndToast(context, "You don't have any alarms set.");
                return;
            }

            StringBuilder response = new StringBuilder("Your alarms:\n");
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());

            for (String id : alarmIds) {
                String alarmJson = prefs.getString("alarm_" + id, null);
                if (alarmJson != null) {
                    try {
                        org.json.JSONObject alarm = new org.json.JSONObject(alarmJson);
                        long time = alarm.getLong("time");
                        String label = alarm.optString("label", null);
                        org.json.JSONArray recurringDays = alarm.optJSONArray("recurringDays");

                        response.append("- ").append(sdf.format(new java.util.Date(time)));
                        if (label != null && !label.isEmpty()) {
                            response.append(" (").append(label).append(")");
                        }
                        if (recurringDays != null && recurringDays.length() > 0) {
                            int[] days = new int[recurringDays.length()];
                            for (int j = 0; j < recurringDays.length(); j++) {
                                days[j] = recurringDays.getInt(j);
                            }
                            response.append(" ").append(getRecurringDaysDescription(days));
                        }
                        response.append("\n");
                    } catch (Exception e) {
                        // skip malformed alarm
                    }
                }
            }

            FeedbackProvider.speakAndToast(context, response.toString().trim());
        } catch (Exception e) {
            android.util.Log.e("AlarmHandler", "Error checking alarms", e);
            FeedbackProvider.speakAndToast(context, "Sorry, there was an error checking your alarms.");
        }
    }

    private void snoozeAlarm(Context context) {
        // Get the most recently triggered alarm
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String lastAlarmId = prefs.getString("last_triggered_alarm", null);
        
        if (lastAlarmId == null) {
            FeedbackProvider.speakAndToast(context, "No active alarm to snooze.");
            return;
        }

        try {
            // Set a new alarm for 9 minutes from now
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 9);
            
            setSystemAlarm(context, lastAlarmId + "_snooze", calendar.getTimeInMillis(), "Snoozed Alarm", null);
            
            SimpleDateFormat sdf = new SimpleDateFormat("h:mm a", Locale.getDefault());
            FeedbackProvider.speakAndToast(context, "Alarm snoozed until " + sdf.format(calendar.getTime()));
        } catch (Exception e) {
            android.util.Log.e("AlarmHandler", "Error snoozing alarm", e);
            FeedbackProvider.speakAndToast(context, "Sorry, there was an error snoozing the alarm.");
        }
    }

    private int[] parseRecurringDays(String pattern) {
        switch (pattern) {
            case "day":
                return new int[]{
                    Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
                    Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY,
                    Calendar.SATURDAY
                };
            case "weekday":
                return new int[]{
                    Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
                    Calendar.THURSDAY, Calendar.FRIDAY
                };
            case "weekend":
                return new int[]{Calendar.SATURDAY, Calendar.SUNDAY};
            case "monday": return new int[]{Calendar.MONDAY};
            case "tuesday": return new int[]{Calendar.TUESDAY};
            case "wednesday": return new int[]{Calendar.WEDNESDAY};
            case "thursday": return new int[]{Calendar.THURSDAY};
            case "friday": return new int[]{Calendar.FRIDAY};
            case "saturday": return new int[]{Calendar.SATURDAY};
            case "sunday": return new int[]{Calendar.SUNDAY};
            default: return null;
        }
    }

    private String getRecurringDaysDescription(int[] days) {
        if (days == null || days.length == 0) return "";

        if (Arrays.equals(days, new int[]{
            Calendar.SUNDAY, Calendar.MONDAY, Calendar.TUESDAY,
            Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY,
            Calendar.SATURDAY
        })) {
            return "every day";
        }

        if (Arrays.equals(days, new int[]{
            Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY,
            Calendar.THURSDAY, Calendar.FRIDAY
        })) {
            return "every weekday";
        }

        if (Arrays.equals(days, new int[]{Calendar.SATURDAY, Calendar.SUNDAY})) {
            return "every weekend";
        }

        List<String> dayNames = new ArrayList<>();
        for (int day : days) {
            switch (day) {
                case Calendar.SUNDAY: dayNames.add("Sunday"); break;
                case Calendar.MONDAY: dayNames.add("Monday"); break;
                case Calendar.TUESDAY: dayNames.add("Tuesday"); break;
                case Calendar.WEDNESDAY: dayNames.add("Wednesday"); break;
                case Calendar.THURSDAY: dayNames.add("Thursday"); break;
                case Calendar.FRIDAY: dayNames.add("Friday"); break;
                case Calendar.SATURDAY: dayNames.add("Saturday"); break;
            }
        }

        return "every " + TextUtils.join(" and ", dayNames);
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
            "set an alarm for 7:30 am",
            "set an alarm for 18:00",
            "set an alarm for 6 in the evening",
            "set an alarm for 6 pm every weekday",
            "set an alarm called \"Gym time\" for 6:30 am",
            "check my alarms",
            "delete the gym alarm",
            "delete all alarms",
            "snooze alarm"
        );
    }
} 