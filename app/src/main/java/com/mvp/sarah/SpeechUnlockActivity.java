package com.mvp.sarah;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.widget.Toast;
import java.util.ArrayList;

public class SpeechUnlockActivity extends Activity {
    public static final String EXTRA_PACKAGE = "package_name";
    public static final String EXTRA_APPNAME = "app_name";
    public static final String EXTRA_ACTION_TYPE = "action_type";
    private static final int REQ_SPEECH = 1001;
    private String packageName;
    private String appName;
    private String actionType;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        packageName = getIntent().getStringExtra(EXTRA_PACKAGE);
        appName = getIntent().getStringExtra(EXTRA_APPNAME);
        actionType = getIntent().getStringExtra(EXTRA_ACTION_TYPE);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the secret key to unlock " + appName);
        startActivityForResult(intent, REQ_SPEECH);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_SPEECH && resultCode == RESULT_OK && data != null) {
            ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
            if (results != null && !results.isEmpty()) {
                String spokenKey = results.get(0).trim().toLowerCase();
                SecretKeySpeechActivity.handleSecretKeyResult(this, spokenKey, packageName, appName, actionType);
            } else {
                Toast.makeText(this, "Could not recognize speech. Try again.", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Speech recognition cancelled or failed.", Toast.LENGTH_SHORT).show();
        }
        finish();
    }
} 
 
 
 