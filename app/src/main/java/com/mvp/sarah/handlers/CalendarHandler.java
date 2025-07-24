package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.provider.CalendarContract;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class CalendarHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
            "add event to my calendar",
            "create a calendar event"
    );

    @Override
    public boolean canHandle(String command) {
        return command.contains("calendar") && (command.contains("add") || command.contains("create"));
    }

    @Override
    public void handle(Context context, String command) {
        // Remove trigger words
        String details = command.replace("add event", "")
                .replace("create event", "")
                .replace("add event to my calendar", "")
                .replace("create a calendar event", "")
                .replace("to my calendar", "")
                .replace("calendar", "")
                .trim();
        String title = details;
        long startTimeMillis = -1;
        boolean allDay = false;
        String dateTimeStr = null;

        // Try to extract date and time (robust)
        // Examples: "meeting with John tomorrow at 3pm", "doctor appointment on June 5th at 2pm", "call mom at 15:30", "lunch next Monday"
        Calendar calendar = Calendar.getInstance();
        boolean foundTime = false;
        boolean foundDate = false;
        int hour = 0, minute = 0;
        // Try to find time (e.g., 3pm, 15:30, 3:30pm)
        Pattern timePattern = Pattern.compile("(at )?(\\d{1,2})(:(\\d{2}))? ?([ap]m)?", Pattern.CASE_INSENSITIVE);
        Matcher timeMatcher = timePattern.matcher(details);
        if (timeMatcher.find()) {
            foundTime = true;
            hour = Integer.parseInt(timeMatcher.group(2));
            minute = (timeMatcher.group(4) != null) ? Integer.parseInt(timeMatcher.group(4)) : 0;
            String ampm = timeMatcher.group(5);
            if (ampm != null && ampm.equalsIgnoreCase("pm") && hour < 12) hour += 12;
            if (ampm != null && ampm.equalsIgnoreCase("am") && hour == 12) hour = 0;
            calendar.set(Calendar.HOUR_OF_DAY, hour);
            calendar.set(Calendar.MINUTE, minute);
            calendar.set(Calendar.SECOND, 0);
            // Remove time from title
            title = details.substring(0, timeMatcher.start()).trim();
        }
        // Try to find date (e.g., tomorrow, on June 5th, next Monday)
        if (details.toLowerCase().contains("tomorrow")) {
            calendar.add(Calendar.DAY_OF_YEAR, 1);
            foundDate = true;
            title = title.replaceAll("(?i)tomorrow", "").trim();
        } else {
            // on June 5th, on 5 June, on 2024-06-05, next Monday
            Pattern datePattern = Pattern.compile("on ([a-zA-Z]+ \\d{1,2}(st|nd|rd|th)?|\\d{1,2} [a-zA-Z]+|\\d{4}-\\d{2}-\\d{2})", Pattern.CASE_INSENSITIVE);
            Matcher dateMatcher = datePattern.matcher(details);
            if (dateMatcher.find()) {
                String dateStr = dateMatcher.group(1).replaceAll("(st|nd|rd|th)", "");
                String[] formats = {"MMMM d yyyy", "d MMMM yyyy", "yyyy-MM-dd"};
                boolean parsed = false;
                for (String fmt : formats) {
                    try {
                        Date parsedDate = new SimpleDateFormat(fmt).parse(dateStr + " " + calendar.get(Calendar.YEAR));
                        calendar.setTime(parsedDate);
                        foundDate = true;
                        parsed = true;
                        break;
                    } catch (ParseException ignored) {}
                }
                if (!parsed) {
                    // fallback: ignore date
                }
                title = title.replace(dateMatcher.group(0), "").trim();
            } else if (details.toLowerCase().contains("next monday")) {
                int today = calendar.get(Calendar.DAY_OF_WEEK);
                int daysUntilMonday = (Calendar.MONDAY - today + 7) % 7;
                if (daysUntilMonday == 0) daysUntilMonday = 7;
                calendar.add(Calendar.DAY_OF_YEAR, daysUntilMonday);
                foundDate = true;
                title = title.replaceAll("(?i)next monday", "").trim();
            }
        }
        // If no time found, make it all-day event
        if (!foundTime) {
            allDay = true;
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
        }
        startTimeMillis = calendar.getTimeInMillis();
        if (title.isEmpty()) {
            FeedbackProvider.speakAndToast(context, "Please provide a title for the event.");
            return;
        }
        // Confirm event details with user
        String confirmMsg = "Create event: '" + title + "'";
        if (foundDate || foundTime) {
            dateTimeStr = new SimpleDateFormat(allDay ? "EEE, MMM d (all day)" : "EEE, MMM d 'at' h:mm a").format(calendar.getTime());
            confirmMsg += " on " + dateTimeStr;
        } else {
            confirmMsg += " today (all day)";
        }
        FeedbackProvider.speakAndToast(context, confirmMsg);
        Intent intent = new Intent(Intent.ACTION_INSERT)
                .setData(CalendarContract.Events.CONTENT_URI)
                .putExtra(CalendarContract.Events.TITLE, title);
        if (startTimeMillis != -1) {
            intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, startTimeMillis);
            if (allDay) intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
        }
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Opening calendar to create your event.");
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Sorry, I couldn't open the calendar app.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 