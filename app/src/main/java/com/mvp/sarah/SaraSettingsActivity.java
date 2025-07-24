package com.mvp.sarah;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.mvp.sarah.CommandHandler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.util.Log;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.net.Uri;
import android.text.method.LinkMovementMethod;
import android.view.View;
import android.widget.TextView;

public class SaraSettingsActivity extends AppCompatActivity {
    private static final int PICK_PPN_FILE = 101;
    private static final String PREFS = "SaraSettingsPrefs";
    private static final String KEY_ACCESS_KEY = "picovoice_access_key";
    private static final String KEY_PPN_PATH = "porcupine_ppn_path";
    private static final String KEY_DISABLED_COMMANDS = "disabled_commands";

    private EditText editAccessKey;
    private Button btnSave;
    private LinearLayout commandsListLayout;
    private Set<String> disabledCommands = new HashSet<>();
    private EditText editUpdateUrl;
    private Button btnSaveUpdateUrl;
    private EditText editUpdateVersion;
    private String userRole = "user";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding_picovoice);

        // Add secret key field
        // editSecretKey = new EditText(this);
        // editSecretKey.setHint("Set Secret Key for Unlock");
        // LinearLayout rootLayout = findViewById(R.id.root_layout); // Assume root layout has this id
        // rootLayout.addView(editSecretKey, 0); // Add at the top

        editAccessKey = findViewById(R.id.edit_access_key);
        btnSave = findViewById(R.id.btn_save_picovoice);
        commandsListLayout = findViewById(R.id.commands_list_container);
        editUpdateUrl = findViewById(R.id.edit_update_url);
        btnSaveUpdateUrl = findViewById(R.id.btn_save_update_url);
        editUpdateVersion = findViewById(R.id.edit_update_version);

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String accessKey = prefs.getString(KEY_ACCESS_KEY, "");
        disabledCommands = prefs.getStringSet(KEY_DISABLED_COMMANDS, new HashSet<>());
        editAccessKey.setText(accessKey);

        // Load secret key if set
        // SharedPreferences appLockPrefs = getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE);
        // String secretKey = appLockPrefs.getString("app_lock_secret", "");
        // editSecretKey.setText(secretKey);

        // Add command toggles
        commandsListLayout.removeAllViews();
        List<CommandHandler> handlers = CommandRegistry.getAllHandlers();
        for (CommandHandler handler : handlers) {
            if (handler instanceof CommandRegistry.SuggestionProvider) {
                List<String> suggestions = ((CommandRegistry.SuggestionProvider) handler).getSuggestions();
                for (String command : suggestions) {
                    View row = getLayoutInflater().inflate(android.R.layout.simple_list_item_multiple_choice, null);
                    Switch toggle = new Switch(this);
                    toggle.setText(command);
                    toggle.setChecked(!disabledCommands.contains(command));
                    toggle.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        if (isChecked) {
                            disabledCommands.remove(command);
                        } else {
                            disabledCommands.add(command);
                        }
                    });
                    commandsListLayout.addView(toggle);
                }
            }
        }

        btnSave.setOnClickListener(v -> {
            String key = editAccessKey.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter your Picovoice Access Key", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Set the PPN path to the bundled asset
            String ppnPath = "keywords/sara_android.ppn";
            
            prefs.edit()
                .putString(KEY_ACCESS_KEY, key)
                .putString(KEY_PPN_PATH, ppnPath)
                .putStringSet(KEY_DISABLED_COMMANDS, disabledCommands)
                .apply();
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();

            // Save secret key
            // String secret = editSecretKey.getText().toString().trim();
            // appLockPrefs.edit().putString("app_lock_secret", secret).apply();
            // Toast.makeText(this, "Secret key saved!", Toast.LENGTH_SHORT).show();

            // Store the access key in Firestore under the user's document
            String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
            FirebaseFirestore db = FirebaseFirestore.getInstance();
            db.collection("users").document(uid)
                .update("picovoice_key", key)
                .addOnSuccessListener(aVoid -> Log.d("SaraSettingsActivity", "Picovoice key updated in Firestore"))
                .addOnFailureListener(e -> Log.e("SaraSettingsActivity", "Failed to update Picovoice key in Firestore", e));

            // Launch permissions checklist before going to MainActivity
            Intent checklistIntent = new Intent(this, PermissionsChecklistActivity.class);
            startActivity(checklistIntent);
            finish();
        });

        Button btnEnableDeviceAdmin = findViewById(R.id.btn_enable_device_admin);
        btnEnableDeviceAdmin.setOnClickListener(v -> {
            ComponentName adminComponent = new ComponentName(this, SaraDeviceAdminReceiver.class);
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin for Sara features.");
            startActivityForResult(intent, 1001);
        });

        Button btnGetAccessKey = findViewById(R.id.btn_get_access_key);
        btnGetAccessKey.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Access Key", "Hey Sarah");
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied 'Hey Sarah' to clipboard!", Toast.LENGTH_SHORT).show();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://picovoice.ai/platform/porcupine/"));
            startActivity(browserIntent);
        });

        TextView linkSetupGuide = findViewById(R.id.link_setup_guide);
        linkSetupGuide.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sara.tiiny.site"));
            startActivity(browserIntent);
        });
        linkSetupGuide.setMovementMethod(LinkMovementMethod.getInstance());

        // Fetch user role from Firestore
        editUpdateUrl.setVisibility(View.GONE);
        btnSaveUpdateUrl.setVisibility(View.GONE);
        editUpdateVersion.setVisibility(View.GONE);

        String uid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        db.collection("users").document(uid).get()
            .addOnSuccessListener(document -> {
                if (document.exists() && "admin".equals(document.getString("role"))) {
                    userRole = "admin";
                    editUpdateUrl.setVisibility(View.VISIBLE);
                    btnSaveUpdateUrl.setVisibility(View.VISIBLE);
                    editUpdateVersion.setVisibility(View.VISIBLE);
                    // Fetch current APK URL and version
                    db.collection("app_update").document("latest").get()
                        .addOnSuccessListener(updateDoc -> {
                            if (updateDoc.exists()) {
                                if (updateDoc.contains("apk_url")) {
                                    String url = updateDoc.getString("apk_url");
                                    if (url != null) editUpdateUrl.setText(url);
                                }
                                if (updateDoc.contains("version_code")) {
                                    long version = updateDoc.getLong("version_code");
                                    editUpdateVersion.setText(String.valueOf(version));
                                }
                            }
                        });
                }
            });

        btnSaveUpdateUrl.setOnClickListener(v -> {
            String url = editUpdateUrl.getText().toString().trim();
            String versionStr = editUpdateVersion.getText().toString().trim();
            if (!url.isEmpty() && !versionStr.isEmpty()) {
                long versionCode = Long.parseLong(versionStr);
                java.util.HashMap<String, Object> data = new java.util.HashMap<>();
                data.put("apk_url", url);
                data.put("version_code", versionCode);
                db.collection("app_update").document("latest")
                    .set(data)
                    .addOnSuccessListener(aVoid -> Toast.makeText(this, "Update URL and version saved!", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e -> Toast.makeText(this, "Failed to save update info", Toast.LENGTH_SHORT).show());
            } else {
                Toast.makeText(this, "Please enter a valid URL and version code", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == PICK_PPN_FILE && resultCode == Activity.RESULT_OK && data != null) {
            Uri uri = data.getData();
            if (uri != null) {
                // ppnFilePath = uri.toString(); // This line is removed
                // ppnFileName = getFileNameFromUri(uri); // This line is removed
                // textPpnFile.setText(ppnFileName != null ? ppnFileName : ppnFilePath); // This line is removed
            }
        }
    }

    private String getFileNameFromUri(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (nameIndex >= 0) {
                        result = cursor.getString(nameIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }
} 