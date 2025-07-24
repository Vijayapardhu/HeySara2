package com.mvp.sarah;

import android.app.Activity;
import android.app.NotificationManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import android.util.Log;
import com.mvp.sarah.utils.PermissionUtils;

public class PermissionsChecklistActivity extends Activity {
    private TextView deviceAdminStatus, accessibilityStatus, dndStatus, permissionsStatus;
    private Button btnDeviceAdmin, btnAccessibility, btnDnd, btnPermissions, btnContinue;
    private static final int REQ_PERMISSIONS = 1002;

    private static String[] getRequiredPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            return new String[] {
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.ANSWER_PHONE_CALLS,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
                android.Manifest.permission.ACCESS_BACKGROUND_LOCATION
            };
        } else {
            return new String[] {
                android.Manifest.permission.RECORD_AUDIO,
                android.Manifest.permission.CAMERA,
                android.Manifest.permission.READ_PHONE_STATE,
                android.Manifest.permission.ANSWER_PHONE_CALLS,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            };
        }
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_permissions_checklist);
        deviceAdminStatus = findViewById(R.id.device_admin_status);
        accessibilityStatus = findViewById(R.id.accessibility_status);
        dndStatus = findViewById(R.id.dnd_status);
        permissionsStatus = findViewById(R.id.permissions_status);
        btnDeviceAdmin = findViewById(R.id.btn_device_admin);
        btnAccessibility = findViewById(R.id.btn_accessibility);
        btnDnd = findViewById(R.id.btn_dnd);
        btnPermissions = findViewById(R.id.btn_permissions);
        btnContinue = findViewById(R.id.btn_continue_permissions);

        // Request all permissions at once on activity start
        ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQ_PERMISSIONS);

        btnDeviceAdmin.setOnClickListener(v -> {
            ComponentName adminComponent = new ComponentName(this, SaraDeviceAdminReceiver.class);
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin for Sara features.");
            startActivity(intent);
        });
        btnAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });
        btnDnd.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
            startActivity(intent);
        });
        btnPermissions.setOnClickListener(v -> {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQ_PERMISSIONS);
            // Request location permissions using PermissionUtils
            if (!PermissionUtils.hasLocationPermission(this)) {
                PermissionUtils.requestLocationPermission(this, REQ_PERMISSIONS);
            }
            if (!PermissionUtils.hasBackgroundLocationPermission(this)) {
                PermissionUtils.requestBackgroundLocationPermission(this, REQ_PERMISSIONS);
            }
        });
        btnContinue.setOnClickListener(v -> {
            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName adminComponent = new ComponentName(this, SaraDeviceAdminReceiver.class);
            boolean isAdmin = dpm.isAdminActive(adminComponent);
            boolean isAccessibility = isAccessibilityServiceEnabled(this, EnhancedAccessibilityService.class);
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            boolean isDnd = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || nm.isNotificationPolicyAccessGranted();
            boolean allGranted = true;
            for (String perm : getRequiredPermissions()) {
                if (ContextCompat.checkSelfPermission(this, perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            // Check location permissions using PermissionUtils
            if (!PermissionUtils.hasLocationPermission(this)) allGranted = false;
            if (!PermissionUtils.hasBackgroundLocationPermission(this)) allGranted = false;
            if (isAdmin && isAccessibility && isDnd && allGranted) {
                finish();
            } else {
                // Show a message to the user (Toast)
                android.widget.Toast.makeText(this, "Please enable all permissions and settings to continue.", android.widget.Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatuses();
    }

    private void updateStatuses() {
        // Device Admin
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = new ComponentName(this, SaraDeviceAdminReceiver.class);
        boolean isAdmin = dpm.isAdminActive(adminComponent);
        deviceAdminStatus.setText(isAdmin ? "Enabled" : "Not enabled");
        deviceAdminStatus.setTextColor(isAdmin ? 0xFF388E3C : 0xFFE53935);
        // Accessibility
        boolean isAccessibility = isAccessibilityServiceEnabled(this, EnhancedAccessibilityService.class);
        accessibilityStatus.setText(isAccessibility ? "Enabled" : "Not enabled");
        accessibilityStatus.setTextColor(isAccessibility ? 0xFF388E3C : 0xFFE53935);
        // DND
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        boolean isDnd = Build.VERSION.SDK_INT < Build.VERSION_CODES.M || nm.isNotificationPolicyAccessGranted();
        dndStatus.setText(isDnd ? "Enabled" : "Not enabled");
        dndStatus.setTextColor(isDnd ? 0xFF388E3C : 0xFFE53935);
        // Permissions
        boolean allGranted = true;
        for (String perm : getRequiredPermissions()) {
            if (ContextCompat.checkSelfPermission(this, perm) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                allGranted = false;
                break;
            }
        }
        // Check location permissions using PermissionUtils
        if (!PermissionUtils.hasLocationPermission(this)) allGranted = false;
        if (!PermissionUtils.hasBackgroundLocationPermission(this)) allGranted = false;
        permissionsStatus.setText(allGranted ? "All granted" : "Missing permissions");
        permissionsStatus.setTextColor(allGranted ? 0xFF388E3C : 0xFFE53935);
        btnContinue.setEnabled(isAdmin && isAccessibility && isDnd && allGranted);
        Log.d("PermissionsChecklist", "isAdmin=" + isAdmin + ", isAccessibility=" + isAccessibility + ", isDnd=" + isDnd + ", allGranted=" + allGranted);
    }

    private boolean isAccessibilityServiceEnabled(Context context, Class<?> service) {
        String prefString = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (prefString == null) return false;
        String serviceName = context.getPackageName() + "/" + service.getName();
        // Also check for flattened name (package/class)
        String flattened = new ComponentName(context, service).flattenToString();
        return prefString.toLowerCase().contains(serviceName.toLowerCase()) || prefString.toLowerCase().contains(flattened.toLowerCase());
    }
} 