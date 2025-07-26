package com.mvp.sarah;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;
import java.util.List;
import java.util.Map;

public class AppUtils {
    private static final String TAG = "AppUtils";
    
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
    
    /**
     * Find camera app package name dynamically from installed apps
     * @param context Application context
     * @return Package name of the camera app, or null if not found
     */
    public static String findCameraAppPackage(Context context) {
        PackageManager pm = context.getPackageManager();
        Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        List<ResolveInfo> apps = pm.queryIntentActivities(mainIntent, 0);
        
        // Priority list of camera app keywords (most common first)
        String[] cameraKeywords = {
            "camera", "cam", "snap", "shoot"
        };
        
        // First, try to find apps with camera-related names
        for (ResolveInfo info : apps) {
            String label = info.loadLabel(pm).toString().toLowerCase();
            String packageName = info.activityInfo.packageName;
            
            // Check if the app name contains camera keywords
            for (String keyword : cameraKeywords) {
                if (label.contains(keyword)) {
                    Log.d(TAG, "Found camera app by name: " + label + " (" + packageName + ")");
                    return packageName;
                }
            }
        }
        
        // If no camera app found by name, try common camera package names
        String[] commonCameraPackages = {
            "com.motorola.camera3",           // Motorola
            "com.google.android.GoogleCamera", // Google Camera
            "com.sec.android.app.camera",     // Samsung
            "com.oneplus.camera",             // OnePlus
            "com.huawei.camera",              // Huawei
            "com.xiaomi.camera",              // Xiaomi
            "com.oppo.camera",                // OPPO
            "com.vivo.camera",                // Vivo
            "com.android.camera",             // Generic Android
            "com.android.camera2"             // Camera2 API
        };
        
        for (String packageName : commonCameraPackages) {
            try {
                pm.getPackageInfo(packageName, 0);
                Log.d(TAG, "Found camera app by package: " + packageName);
                return packageName;
            } catch (PackageManager.NameNotFoundException e) {
                // Package not found, continue to next
            }
        }
        
        Log.w(TAG, "No camera app found on device");
        return null;
    }
} 