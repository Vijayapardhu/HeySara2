package com.mvp.sarah;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

public class SaraWidgetProvider extends AppWidgetProvider {
    private static final String ACTION_TOGGLE_SERVICE = "com.mvp.sarah.ACTION_TOGGLE_SERVICE";

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            updateWidget(context, appWidgetManager, appWidgetId, SaraVoiceService.isRunning());
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_TOGGLE_SERVICE.equals(intent.getAction())) {
            boolean running = SaraVoiceService.isRunning();
            Intent serviceIntent = new Intent(context, SaraVoiceService.class);
            if (running) {
                context.stopService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
            // Update all widgets
            AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
            int[] appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, SaraWidgetProvider.class));
            for (int appWidgetId : appWidgetIds) {
                updateWidget(context, appWidgetManager, appWidgetId, !running);
            }
        }
    }

    public static void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, boolean isRunning) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sara_widget);
        views.setTextViewText(R.id.widget_status, isRunning ? "Service running" : "Service stopped");
        views.setTextViewText(R.id.btn_service_toggle, isRunning ? "Stop Sara" : "Start Sara");
        int btnColor = isRunning ? context.getResources().getColor(R.color.gray) : context.getResources().getColor(R.color.orange);
        views.setInt(R.id.btn_service_toggle, "setBackgroundColor", btnColor);
        Intent toggleIntent = new Intent(context, SaraWidgetProvider.class);
        toggleIntent.setAction(ACTION_TOGGLE_SERVICE);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(context, 0, toggleIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.btn_service_toggle, pendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
    }
} 