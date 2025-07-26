package com.mvp.sarah;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.slider.Slider;
import android.widget.TextView;
import android.app.AlertDialog;
import android.widget.EditText;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.Executor;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.LinearLayout;
import android.content.Context;

public class ManageAppLockActivity extends AppCompatActivity {
    private RecyclerView recyclerAllApps;
    private AppLockAdapter adapter;
    private MaterialButton btnChangePassword;
    private Slider sliderLockDuration;
    private TextView labelLockDuration, emptyAllApps;
    private int lockDuration = 5;
    private EditText editSearch;
    private List<AppLockItem> allAppsFullList;
    private EditText editPin;
    private Button btnSavePin;

    private static final String PREFS = "AppLockPrefs";
    private static final String KEY_LOCKED_APPS = "locked_apps";
    private static final String KEY_PIN = "pin";
    private static final String KEY_PIN_SET = "pin_set";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_app_lock);

        // Use the beautiful card layout views
        editPin = findViewById(R.id.edit_pin);
        btnSavePin = findViewById(R.id.btn_save_pin);

        // Load current PIN if set
        SharedPreferences prefs = getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE);
        String pin = prefs.getString("app_lock_secret", "");
        editPin.setText(pin);

        btnSavePin.setOnClickListener(v -> {
            String newPin = editPin.getText().toString().trim();
            if (newPin.isEmpty()) {
                Toast.makeText(this, "Please enter a PIN or secret key", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString("app_lock_secret", newPin).apply();
            Toast.makeText(this, "App lock PIN/secret key saved!", Toast.LENGTH_SHORT).show();
        });

        recyclerAllApps = findViewById(R.id.recycler_all_apps);
        btnChangePassword = findViewById(R.id.btn_change_password);
        sliderLockDuration = findViewById(R.id.slider_lock_duration);
        labelLockDuration = findViewById(R.id.label_lock_duration);
        emptyAllApps = findViewById(R.id.empty_all_apps);
        recyclerAllApps.setLayoutManager(new LinearLayoutManager(this));
        editSearch = findViewById(R.id.edit_search);
        // Load all apps and lock state
        allAppsFullList = loadAllApps();
        adapter = new AppLockAdapter(new ArrayList<>(allAppsFullList), this::onToggleLock);
        recyclerAllApps.setAdapter(adapter);
        emptyAllApps.setVisibility(allAppsFullList.isEmpty() ? TextView.VISIBLE : TextView.GONE);
        // Search/filter logic
        editSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterApps(s.toString());
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });
        // Lock duration slider
        sliderLockDuration.setValue(lockDuration);
        labelLockDuration.setText(lockDuration + " minutes");
        sliderLockDuration.addOnChangeListener((slider, value, fromUser) -> {
            lockDuration = (int) value;
            labelLockDuration.setText(lockDuration + " minutes");
            // Save lock duration to preferences
            SharedPreferences lockPrefs = getSharedPreferences("app_lock_settings", Context.MODE_PRIVATE);
            lockPrefs.edit().putInt("lock_duration", lockDuration).apply();
        });
        // Change password
        btnChangePassword.setOnClickListener(v -> showChangePasswordDialog());
    }

    private List<AppLockItem> loadAllApps() {
        List<AppLockItem> list = new ArrayList<>();
        PackageManager pm = getPackageManager();
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> lockedApps = prefs.getStringSet(KEY_LOCKED_APPS, new HashSet<>());
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app : apps) {
            // Skip system apps
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            String appName = pm.getApplicationLabel(app).toString();
            boolean locked = lockedApps.contains(app.packageName);
            list.add(new AppLockItem(app.packageName, appName, locked));
        }
        Collections.sort(list, (a, b) -> a.appName.compareToIgnoreCase(b.appName));
        return list;
    }

    private void onToggleLock(AppLockItem item, boolean locked) {
        if (!locked) { // User is trying to unlock
            Executor executor = ContextCompat.getMainExecutor(this);
            BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                    runOnUiThread(() -> actuallyToggleLock(item, locked));
                }
                @Override
                public void onAuthenticationError(int errorCode, CharSequence errString) {
                    runOnUiThread(() -> Toast.makeText(ManageAppLockActivity.this, "Biometric authentication failed", Toast.LENGTH_SHORT).show());
                }
            });
            BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                    .setTitle("Confirm your identity")
                    .setSubtitle("Authenticate to unlock " + item.appName)
                    .setNegativeButtonText("Cancel")
                    .build();
            biometricPrompt.authenticate(promptInfo);
        } else {
            actuallyToggleLock(item, locked);
        }
    }

    private void actuallyToggleLock(AppLockItem item, boolean locked) {
        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
        Set<String> lockedApps = new HashSet<>(prefs.getStringSet(KEY_LOCKED_APPS, new HashSet<>()));
        if (locked) {
            lockedApps.add(item.packageName);
        } else {
            lockedApps.remove(item.packageName);
        }
        prefs.edit().putStringSet(KEY_LOCKED_APPS, lockedApps).apply();
        item.locked = locked;
        Toast.makeText(this, (locked ? "Locked " : "Unlocked ") + item.appName, Toast.LENGTH_SHORT).show();
        adapter.notifyDataSetChanged();
    }

    private void showChangePasswordDialog() {
        Executor executor = ContextCompat.getMainExecutor(this);
        BiometricPrompt biometricPrompt = new BiometricPrompt(this, executor, new BiometricPrompt.AuthenticationCallback() {
            @Override
            public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult result) {
                runOnUiThread(() -> showPasswordInputDialog());
            }
            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                runOnUiThread(() -> Toast.makeText(ManageAppLockActivity.this, "Biometric authentication failed", Toast.LENGTH_SHORT).show());
            }
        });
        BiometricPrompt.PromptInfo promptInfo = new BiometricPrompt.PromptInfo.Builder()
                .setTitle("Confirm your identity")
                .setSubtitle("Authenticate to change app lock password")
                .setNegativeButtonText("Cancel")
                .build();
        biometricPrompt.authenticate(promptInfo);
    }

    private void showPasswordInputDialog() {
        EditText input = new EditText(this);
        input.setHint("Enter new 4-digit PIN");
        input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER | android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        new AlertDialog.Builder(this)
                .setTitle("Change App Lock Password")
                .setView(input)
                .setPositiveButton("Save", (dialog, which) -> {
                    String newPin = input.getText().toString().trim();
                    if (newPin.length() == 4 && newPin.matches("\\d{4}")) {
                        SharedPreferences prefs = getSharedPreferences(PREFS, MODE_PRIVATE);
                        prefs.edit()
                            .putString(KEY_PIN, newPin)
                            .putBoolean(KEY_PIN_SET, true)
                            .apply();
                        Toast.makeText(this, "Password changed!", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "PIN must be exactly 4 digits", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void filterApps(String query) {
        List<AppLockItem> filtered = new ArrayList<>();
        for (AppLockItem item : allAppsFullList) {
            if (item.appName.toLowerCase().contains(query.toLowerCase()) || item.packageName.toLowerCase().contains(query.toLowerCase())) {
                filtered.add(item);
            }
        }
        adapter.updateList(filtered);
        emptyAllApps.setVisibility(filtered.isEmpty() ? TextView.VISIBLE : TextView.GONE);
    }

    // Data class for app items
    public static class AppLockItem {
        public String packageName;
        public String appName;
        public boolean locked;
        public AppLockItem(String pkg, String name, boolean locked) {
            this.packageName = pkg;
            this.appName = name;
            this.locked = locked;
        }
    }
} 