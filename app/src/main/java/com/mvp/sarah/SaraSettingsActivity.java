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
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.TreeMap;
import java.util.Collections;
import java.util.Comparator;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

public class SaraSettingsActivity extends AppCompatActivity {
    private static final int PICK_PPN_FILE = 101;
    private static final String PREFS = "SaraSettingsPrefs";
    private static final String KEY_ACCESS_KEY = "picovoice_access_key";
    private static final String KEY_PPN_PATH = "porcupine_ppn_path";
    private static final String KEY_DISABLED_COMMANDS = "disabled_commands";

    private EditText searchCommandsEditText;
    private RecyclerView commandsRecyclerView;
    private CommandCategoryAdapter commandAdapter;
    private Map<String, List<String>> categorizedCommands = new TreeMap<>();

    private EditText editAccessKey;
    private Button btnSave;
    private Button btnGetAccessKey;
    private Button btnEnableDeviceAdmin;
    private EditText editUpdateUrl;
    private EditText editUpdateVersion;
    private Button btnSaveUpdateUrl;
    private TextView linkSetupGuide;
    private String userRole = "user";
    private Set<String> disabledCommands = new HashSet<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sara_settings);

        // Wire up new/old views
        editAccessKey = findViewById(R.id.edit_access_key);
        btnSave = findViewById(R.id.btn_save_picovoice);
        btnGetAccessKey = findViewById(R.id.btn_get_access_key);
        btnEnableDeviceAdmin = findViewById(R.id.btn_enable_device_admin);
        editUpdateUrl = findViewById(R.id.edit_update_url);
        editUpdateVersion = findViewById(R.id.edit_update_version);
        btnSaveUpdateUrl = findViewById(R.id.btn_save_update_url);
        linkSetupGuide = findViewById(R.id.link_setup_guide);

        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String accessKey = prefs.getString(KEY_ACCESS_KEY, "");
        disabledCommands = prefs.getStringSet(KEY_DISABLED_COMMANDS, new HashSet<>());
        editAccessKey.setText(accessKey);

        btnSave.setOnClickListener(v -> {
            String key = editAccessKey.getText().toString().trim();
            if (key.isEmpty()) {
                Toast.makeText(this, "Please enter your Picovoice Access Key", Toast.LENGTH_SHORT).show();
                return;
            }
            String ppnPath = "keywords/sara_android.ppn";
            prefs.edit()
                .putString(KEY_ACCESS_KEY, key)
                .putString(KEY_PPN_PATH, ppnPath)
                .putStringSet(KEY_DISABLED_COMMANDS, disabledCommands)
                .apply();
            Toast.makeText(this, "Settings saved!", Toast.LENGTH_SHORT).show();
        });

        btnGetAccessKey.setOnClickListener(v -> {
            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("Access Key", "Hey Sarah");
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "Copied 'Hey Sarah' to clipboard!", Toast.LENGTH_SHORT).show();
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://picovoice.ai/platform/porcupine/"));
            startActivity(browserIntent);
        });

        btnEnableDeviceAdmin.setOnClickListener(v -> {
            ComponentName adminComponent = new ComponentName(this, SaraDeviceAdminReceiver.class);
            Intent intent = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            intent.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            intent.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Enable device admin for Sara features.");
            startActivityForResult(intent, 1001);
        });

        linkSetupGuide.setOnClickListener(v -> {
            Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://sara.tiiny.site"));
            startActivity(browserIntent);
        });
        linkSetupGuide.setMovementMethod(LinkMovementMethod.getInstance());

        // Admin update fields logic
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

        searchCommandsEditText = findViewById(R.id.search_commands);
        commandsRecyclerView = findViewById(R.id.commands_recycler_view);
        commandsRecyclerView.setLayoutManager(new LinearLayoutManager(this));

        // Gather and categorize commands, allowing commands to appear under all relevant categories
        List<CommandHandler> handlers = CommandRegistry.getAllHandlers();
        categorizedCommands.clear();
        Map<String, Set<String>> commandToCategories = new HashMap<>();
        for (CommandHandler handler : handlers) {
            String category = handler.getClass().getSimpleName().replace("Handler", "");
            if (handler instanceof CommandRegistry.SuggestionProvider) {
                List<String> suggestions = ((CommandRegistry.SuggestionProvider) handler).getSuggestions();
                Collections.sort(suggestions, String.CASE_INSENSITIVE_ORDER);
                if (!categorizedCommands.containsKey(category)) {
                    categorizedCommands.put(category, new ArrayList<>());
                }
                for (String command : suggestions) {
                    categorizedCommands.get(category).add(command);
                    if (!commandToCategories.containsKey(command)) {
                        commandToCategories.put(command, new HashSet<>());
                    }
                    commandToCategories.get(command).add(category);
                }
            }
        }
        commandAdapter = new CommandCategoryAdapter(categorizedCommands, commandToCategories);
        commandsRecyclerView.setAdapter(commandAdapter);

        // Filter logic
        searchCommandsEditText.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterCommands(s.toString());
            }
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
    }

    private void filterCommands(String query) {
        Map<String, List<String>> filtered = new TreeMap<>();
        for (String category : categorizedCommands.keySet()) {
            List<String> filteredList = new ArrayList<>();
            for (String command : categorizedCommands.get(category)) {
                if (command.toLowerCase().contains(query.toLowerCase())) {
                    filteredList.add(command);
                }
            }
            if (!filteredList.isEmpty()) {
                filtered.put(category, filteredList);
            }
        }
        commandAdapter.setData(filtered);
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