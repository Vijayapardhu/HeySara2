package com.mvp.sarah;

import android.Manifest;
import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.view.KeyEvent;
import android.content.pm.ApplicationInfo;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.mvp.sarah.handlers.FileShareHelper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AlertDialog;
import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Environment;
import android.database.Cursor;
import android.widget.Toast;

import java.util.ArrayList;
import androidx.annotation.NonNull;
import android.content.Context;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import java.util.concurrent.TimeUnit;
import android.os.Build;
import android.provider.Settings;
import android.content.Intent;
import android.net.Uri;
import android.app.Activity;

public class MainActivity extends AppCompatActivity {

    private Button btnEnableAccessibility;
    private FileShareHelper fileShareHelper;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    private Uri lastFileUri;
    private String lastFileName;
    private boolean isFileSharingFlow = false;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private SensorEventListener shakeListener;
    private static final float SHAKE_THRESHOLD_GRAVITY = 2.7F;
    private static final int SHAKE_SLOP_TIME_MS = 500;
    private long mShakeTimestamp;
    private static final int VOLUME_PRESS_EMERGENCY_COUNT = 4;
    private static final long VOLUME_PRESS_WINDOW_MS = 2000;
    private int volumePressCount = 0;
    private long lastVolumePressTime = 0;

    private long downloadId = -1;
    private boolean isInitialized = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        SharedPreferences prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE);
        boolean isPinSet = prefs.getBoolean("app_lock_pin_set", false);
        if (!isPinSet) {
            Intent intent = new Intent(this, AppLockActivity.class);
            intent.putExtra("setup", true);
            startActivity(intent);
            finish();
            return;
        }

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            startActivity(new Intent(this, AuthActivity.class));
            finish();
            return;
        }

        // Handle ACTION_ASSIST intent if launched as assistant
        if (Intent.ACTION_ASSIST.equals(getIntent().getAction())) {
            Intent serviceIntent = new Intent(this, SaraVoiceService.class);
            serviceIntent.setAction("com.mvp.sarah.ACTION_START_COMMAND_LISTENING");
            ContextCompat.startForegroundService(this, serviceIntent);
            finish();
            return;
        }

        // Request all necessary permissions at launch (before anything else)
        String[] permissions = new String[] {
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.CAMERA,
                Manifest.permission.READ_PHONE_STATE,
                Manifest.permission.ANSWER_PHONE_CALLS,
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.WRITE_SETTINGS,
                Manifest.permission.SYSTEM_ALERT_WINDOW,
                Manifest.permission.FOREGROUND_SERVICE,
                Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
                Manifest.permission.CALL_PHONE,
                Manifest.permission.READ_CONTACTS,
                Manifest.permission.SEND_SMS,
                Manifest.permission.READ_SMS,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
                Manifest.permission.READ_EXTERNAL_STORAGE
        };
        ArrayList<String> permissionsToRequest = new ArrayList<>();
        for (String perm : permissions) {
            if (ContextCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(perm);
            }
        }
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsToRequest.toArray(new String[0]), 100);
        }

        setContentView(R.layout.activity_main);
        Log.d("MainActivity", "MainActivity onCreate called");

        // Onboarding: Show Picovoice onboarding if not set
        SharedPreferences saraPrefs = getSharedPreferences("SaraSettingsPrefs", Context.MODE_PRIVATE);
        String accessKey = saraPrefs.getString("picovoice_access_key", "");
        String ppnPath = saraPrefs.getString("porcupine_ppn_path", "");
        
        // Only show onboarding if we haven't set it up yet
        if (accessKey.isEmpty() || ppnPath.isEmpty()) {
            Intent onboardingIntent = new Intent(this, SaraSettingsActivity.class);
            onboardingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(onboardingIntent);
            // Don't finish here, let the user complete onboarding
            return;
        }

        // Check and request overlay permission
        if (!Settings.canDrawOverlays(this)) {
            Toast.makeText(this, "Please grant overlay permission for Sara to work over other apps", Toast.LENGTH_LONG).show();
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(Uri.parse("package:" + getPackageName()));
            startActivity(intent);
        }

        // Check and request write settings permission for brightness control
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.System.canWrite(this)) {
                Toast.makeText(this, "Please grant write settings permission for Sara to control brightness", Toast.LENGTH_LONG).show();
                Intent intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + getPackageName()));
                startActivity(intent);
            }
        }

        // Initialize buttons and views
        btnEnableAccessibility = findViewById(R.id.btn_enable_accessibility);
        Button btnCallSara = findViewById(R.id.btn_call_sara);
        Button btnSettings = findViewById(R.id.btn_settings);
        Button btnHelp = findViewById(R.id.btn_help);
        Button btnAbout = findViewById(R.id.btn_about);
        View statusIndicator = findViewById(R.id.status_indicator);
        TextView statusText = findViewById(R.id.status_text);

        // Register all command handlers
        CommandRegistry.init(this);

        // Main activation button
        btnEnableAccessibility.setOnClickListener(v -> {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            startActivity(intent);
        });

        // Call Sara button
        btnCallSara.setOnClickListener(v -> {
            Intent serviceIntent = new Intent(this, SaraVoiceService.class);
            serviceIntent.setAction("com.mvp.sarah.ACTION_START_COMMAND_LISTENING");
            ContextCompat.startForegroundService(this, serviceIntent);
            Toast.makeText(this, "Calling Sara...", Toast.LENGTH_SHORT).show();
        });

        // Settings button
        btnSettings.setOnClickListener(v -> {
            Intent intent = new Intent(this, SaraSettingsActivity.class);
            startActivity(intent);
        });

        // Help button
        btnHelp.setOnClickListener(v -> {
            showHelpDialog();
        });

        // About button
        btnAbout.setOnClickListener(v -> {
            showAboutDialog();
        });

        // File sharing setup
        fileShareHelper = new FileShareHelper();
        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d("FileShare", "File picker result received");
                Toast.makeText(this, "File picker result received", Toast.LENGTH_SHORT).show();
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    isFileSharingFlow = true;
                    fileShareHelper.handleFilePickerResult(this, result.getData());
                    ArrayList<Uri> uris = fileShareHelper.getFileUris();
                    ArrayList<String> names = fileShareHelper.getFileNames();
                    Log.d("FileShare", "URIs: " + uris + ", Names: " + names);
                    if (uris != null && names != null && uris.size() == names.size() && !uris.isEmpty()) {
                        Intent intent = new Intent(this, FileShareActivity.class);
                        ArrayList<String> uriStrings = new ArrayList<>();
                        for (Uri uri : uris) uriStrings.add(uri.toString());
                        intent.putStringArrayListExtra("uris", uriStrings);
                        intent.putStringArrayListExtra("names", names);
                        Log.d("FileShare", "Starting FileShareActivity");
                        Toast.makeText(this, "Starting FileShareActivity", Toast.LENGTH_SHORT).show();
                        startActivity(intent);
                    }
                } else {
                    isFileSharingFlow = false;
                }
            }
        );

        // Only auto-finish if not handling a share file intent or file sharing flow
        boolean isShareFileIntent = getIntent().getBooleanExtra("share_file", false);
        if (!isShareFileIntent && !isFileSharingFlow) {
            if (isAccessibilityServiceEnabled()) {
                btnEnableAccessibility.setVisibility(View.GONE);
                startAssistantService();
            } else {
                btnEnableAccessibility.setVisibility(View.VISIBLE);
            }
        }

        // Shake to activate setup
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        if (sensorManager != null) {
            accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        }
        shakeListener = new SensorEventListener() {
            @Override
            public void onSensorChanged(SensorEvent event) {
                if (event.sensor.getType() != Sensor.TYPE_ACCELEROMETER) return;
                float x = event.values[0];
                float y = event.values[1];
                float z = event.values[2];
                float gX = x / SensorManager.GRAVITY_EARTH;
                float gY = y / SensorManager.GRAVITY_EARTH;
                float gZ = z / SensorManager.GRAVITY_EARTH;
                float gForce = (float) Math.sqrt(gX * gX + gY * gY + gZ * gZ);
                if (gForce > SHAKE_THRESHOLD_GRAVITY) {
                    final long now = System.currentTimeMillis();
                    if (mShakeTimestamp + SHAKE_SLOP_TIME_MS > now) {
                        return;
                    }
                    mShakeTimestamp = now;
                    Toast.makeText(MainActivity.this, "Shake detected! Activating Sara...", Toast.LENGTH_SHORT).show();
                    startAssistantService();
                }
            }
            @Override
            public void onAccuracyChanged(Sensor sensor, int accuracy) {}
        };

        // Start TriggerDetectionService for background shake detection
        Intent triggerServiceIntent = new Intent(this, TriggerDetectionService.class);
        ContextCompat.startForegroundService(this, triggerServiceIntent);

        // Remove all code that starts or references ShakeDetectionService

        // Scan and store all apps mapping on first launch
        if (!prefs.getBoolean("all_apps_scanned", false)) {
            scanAndStoreAllApps(this);
            prefs.edit().putBoolean("all_apps_scanned", true).apply();
        }

        // Schedule periodic update check
        PeriodicWorkRequest updateWork = new PeriodicWorkRequest.Builder(
            UpdateCheckWorker.class,
            12, TimeUnit.HOURS)
            .build();
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "update_check",
            ExistingPeriodicWorkPolicy.KEEP,
            updateWork);
        
    }

    private void scanAndStoreAllApps(Context context) {
        PackageManager pm = context.getPackageManager();
        java.util.List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        Map<String, String> appNameToPackage = new HashMap<>();
        for (ApplicationInfo app : apps) {
            String appName = pm.getApplicationLabel(app).toString();
            appNameToPackage.put(appName, app.packageName);
        }
        SharedPreferences prefs = context.getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("all_apps_map", new Gson().toJson(appNameToPackage));
        editor.apply();
    }

    @Override
    protected void onStart() {
        super.onStart();
        checkForAppUpdate();
    }

    private void checkForAppUpdate() {
        FirebaseFirestore.getInstance().collection("app_update").document("latest").get()
            .addOnSuccessListener(document -> {
                if (document.exists() && document.contains("version_code") && document.contains("apk_url")) {
                    long latestVersion = document.getLong("version_code");
                    int currentVersion = getCurrentAppVersionCode();
                    if (currentVersion < latestVersion) {
                        String apkUrl = document.getString("apk_url");
                        promptForUpdate(apkUrl);
                    }
                }
            });
    }

    private int getCurrentAppVersionCode() {
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            return 0;
        }
    }

    private void promptForUpdate(String apkUrl) {
        Toast.makeText(this, "Downloading update in background...", Toast.LENGTH_SHORT).show();
        silentDownloadAndPromptInstall(apkUrl);
    }

    private void silentDownloadAndPromptInstall(String apkUrl) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
        request.setTitle("Sara Update");
        request.setDescription("Downloading update...");
        request.setDestinationInExternalFilesDir(this, Environment.DIRECTORY_DOWNLOADS, "Sara-latest.apk");
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setAllowedOverMetered(true);
        request.setAllowedOverRoaming(true);

        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        downloadId = manager.enqueue(request);

        registerReceiver(new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                long id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                if (id == downloadId) {
                    DownloadManager.Query query = new DownloadManager.Query();
                    query.setFilterById(downloadId);
                    Cursor cursor = manager.query(query);
                    if (cursor.moveToFirst()) {
                        String uriString = cursor.getString(cursor.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                        installApk(Uri.parse(uriString));
                    }
                    cursor.close();
                }
            }
        }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    private void installApk(Uri apkUri) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(apkUri, "application/vnd.android.package-archive");
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(intent);
    }

    private void startAssistantService() {
        try {
            Log.d("MainActivity", "Starting assistant service and finishing activity.");
            Intent serviceIntent = new Intent(this, SaraVoiceService.class);
            serviceIntent.setAction("com.mvp.sarah.ACTION_START_COMMAND_LISTENING");
            startForegroundService(serviceIntent);
            finish();
        } catch (Exception e) {
            Log.e("MainActivity", "Error starting assistant service: " + e.getMessage());
            Toast.makeText(this, "Error starting Sara service. Please try again.", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        // Update status indicator and text
        View statusIndicator = findViewById(R.id.status_indicator);
        android.widget.TextView statusText = findViewById(R.id.status_text);
        Button btnCallSara = findViewById(R.id.btn_call_sara);
        
        if (isAccessibilityServiceEnabled()) {
            // Service is enabled
            if (statusIndicator != null) {
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_online);
            }
            if (statusText != null) {
                statusText.setText("Sara is active and listening");
            }
            if (btnCallSara != null) {
                btnCallSara.setVisibility(View.VISIBLE);
            }
            if (isInitialized && btnEnableAccessibility != null) {
                btnEnableAccessibility.setVisibility(View.GONE);
            }
            startAssistantService();
        } else {
            // Service is not enabled
            if (statusIndicator != null) {
                statusIndicator.setBackgroundResource(R.drawable.status_indicator_offline);
            }
            if (statusText != null) {
                statusText.setText("Sara needs to be activated");
            }
            if (btnCallSara != null) {
                btnCallSara.setVisibility(View.GONE);
            }
            if (btnEnableAccessibility != null) {
                btnEnableAccessibility.setVisibility(View.VISIBLE);
            }
        }
        
        handleShareFileIntent(getIntent());
        boolean isShareFileIntent = getIntent().getBooleanExtra("share_file", false);
        if (!isShareFileIntent && !isFileSharingFlow) {
            if (isAccessibilityServiceEnabled()) {
                btnEnableAccessibility.setVisibility(View.GONE);
                startAssistantService();
            } else {
                btnEnableAccessibility.setVisibility(View.VISIBLE);
            }
        }
        if (sensorManager != null && accelerometer != null && shakeListener != null) {
            sensorManager.registerListener(shakeListener, accelerometer, SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister shake listener
        if (sensorManager != null && shakeListener != null) {
            sensorManager.unregisterListener(shakeListener);
        }
    }

    private boolean isAccessibilityServiceEnabled() {
        String serviceId = getPackageName() + "/" + ClickAccessibilityService.class.getCanonicalName();
        TextUtils.SimpleStringSplitter splitter = new TextUtils.SimpleStringSplitter(':');
        String settingValue = Settings.Secure.getString(getContentResolver(), Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        if (settingValue != null) {
            splitter.setString(settingValue);
            while (splitter.hasNext()) {
                if (splitter.next().equalsIgnoreCase(serviceId)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (fileShareHelper != null) fileShareHelper.stopServer();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // RECORD_AUDIO permission granted
                android.util.Log.d("MainActivity", "RECORD_AUDIO permission granted");
            } else {
                // Permission denied, show a message or disable voice features
                android.widget.Toast.makeText(this, "Microphone permission is required for Sara to work", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 2) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // FOREGROUND_SERVICE_MICROPHONE permission granted
                android.util.Log.d("MainActivity", "FOREGROUND_SERVICE_MICROPHONE permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Foreground service permission is required for Sara to work in background", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 3) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // CAMERA permission granted
                android.util.Log.d("MainActivity", "CAMERA permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Camera permission is required for Sara to work", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 4) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_PHONE_STATE permission granted
                android.util.Log.d("MainActivity", "READ_PHONE_STATE permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Phone state permission is required for Sara to work", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 5) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // ANSWER_PHONE_CALLS permission granted
                android.util.Log.d("MainActivity", "ANSWER_PHONE_CALLS permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Answer phone calls permission is required for Sara to work", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 6) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_CONTACTS permission granted
                android.util.Log.d("MainActivity", "READ_CONTACTS permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Contacts permission is required for Sara to announce caller names", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 7) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // CALL_PHONE permission granted
                android.util.Log.d("MainActivity", "CALL_PHONE permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Call phone permission is required for Sara to make calls", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 8) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // SEND_SMS permission granted
                android.util.Log.d("MainActivity", "SEND_SMS permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Send SMS permission is required for Sara to send messages", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 9) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // READ_SMS permission granted
                android.util.Log.d("MainActivity", "READ_SMS permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Read SMS permission is required for Sara to read messages", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 10) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Location permission granted
                android.util.Log.d("MainActivity", "Location permission granted");
            } else {
                // Permission denied, show a message
                android.widget.Toast.makeText(this, "Location permission is required to find out where you are", android.widget.Toast.LENGTH_LONG).show();
            }
        } else if (requestCode == 100) {
            for (int i = 0; i < permissions.length; i++) {
                if (permissions[i].equals(Manifest.permission.CALL_PHONE) && grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                    // Show dialog guiding user to app settings
                    new android.app.AlertDialog.Builder(this)
                        .setTitle("Direct Calling Permission Needed")
                        .setMessage("To enable direct calling, please grant the 'Phone' permission in app settings.")
                        .setPositiveButton("Open Settings", (dialog, which) -> {
                            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(Uri.parse("package:" + getPackageName()));
                            startActivity(intent);
                        })
                        .setNegativeButton("Cancel", null)
                        .show();
                }
            }
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        handleShareFileIntent(intent);
        if (Intent.ACTION_ASSIST.equals(intent.getAction())) {
            // Start the voice service and overlay
            Intent serviceIntent = new Intent(this, SaraVoiceService.class);
            serviceIntent.setAction("com.mvp.sarah.ACTION_START_COMMAND_LISTENING");
            androidx.core.content.ContextCompat.startForegroundService(this, serviceIntent);
            finish(); // Close MainActivity so only overlay is visible
        }
    }

    private void startFileShareService() {
        ArrayList<Uri> uris = fileShareHelper.getFileUris();
        ArrayList<String> names = fileShareHelper.getFileNames();
        if (uris.isEmpty() || names.isEmpty() || uris.size() != names.size()) {
            Toast.makeText(this, "No files to share.", Toast.LENGTH_LONG).show();
            return;
        }
        ArrayList<String> uriStrings = new ArrayList<>();
        for (Uri uri : uris) uriStrings.add(uri.toString());
        Intent serviceIntent = new Intent(this, FileShareService.class);
        serviceIntent.setAction(FileShareService.ACTION_START);
        serviceIntent.putStringArrayListExtra(FileShareService.EXTRA_URIS, uriStrings);
        serviceIntent.putStringArrayListExtra(FileShareService.EXTRA_NAMES, names);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        // Optionally show a QR code in the UI, or just rely on the notification
        Toast.makeText(this, "File sharing started in background. Check notification.", Toast.LENGTH_LONG).show();
    }

    private void handleShareFileIntent(Intent intent) {
        if (intent != null && intent.getBooleanExtra("share_file", false)) {
            // Reset the flag so it doesn't trigger again
            intent.removeExtra("share_file");
            isFileSharingFlow = true;
            fileShareHelper.pickFiles(this, filePickerLauncher);
        }
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(this)
            .setTitle("💡 How to Use Sara")
            .setMessage("🎤 Wake Word: Say 'Hey Sara' to activate\n\n" +
                       "📞 Voice Commands:\n" +
                       "• 'Call [contact name]' - Make a call\n" +
                       "• 'Send message to [contact]' - Send SMS\n" +
                       "• 'Open [app name]' - Launch apps\n" +
                       "• 'Lock [app name]' - Secure apps\n" +
                       "• 'What's the weather?' - Get weather info\n" +
                       "• 'Play music' - Control music\n" +
                       "• 'Take a screenshot' - Capture screen\n\n" +
                       "🔒 Security: Sara can lock/unlock apps with voice\n" +
                       "📱 Accessibility: Works with screen off\n" +
                       "🌐 Internet: Requires connection for some features")
            .setPositiveButton("Got it!", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .show();
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(this)
            .setTitle("ℹ️ About Hey Sara")
            .setMessage("Hey Sara - Your Personal Voice Assistant\n\n" +
                       "Version: 1.0\n" +
                       "Developer: Sara Team\n\n" +
                       "Features:\n" +
                       "• Voice-controlled assistant\n" +
                       "• App security & locking\n" +
                       "• Call & message management\n" +
                       "• Music & media control\n" +
                       "• System settings control\n" +
                       "• Emergency features\n\n" +
                       "Made with ❤️ for accessibility")
            .setPositiveButton("Close", null)
            .setIcon(android.R.drawable.ic_dialog_info)
            .show();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            long now = System.currentTimeMillis();
            if (now - lastVolumePressTime > VOLUME_PRESS_WINDOW_MS) {
                volumePressCount = 1;
            } else {
                volumePressCount++;
            }
            lastVolumePressTime = now;
            if (volumePressCount >= VOLUME_PRESS_EMERGENCY_COUNT) {
                volumePressCount = 0;
                com.mvp.sarah.EmergencyActions.triggerEmergency(this);
                return true;
            }
        }
        return super.onKeyDown(keyCode, event);
    }
}
