package com.mvp.sarah;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;

public class AppUtils {
    public static String getPackageNameFromAppName(Context context, String appName) {
        SharedPreferences prefs = context.getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE);
        String allAppsJson = prefs.getString("all_apps_map", "{}");
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> appNameToPackage = new Gson().fromJson(allAppsJson, type);
        for (Map.Entry<String, String> entry : appNameToPackage.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(appName)) {
                return entry.getValue();
            }
        }
        return null;
    }
} 