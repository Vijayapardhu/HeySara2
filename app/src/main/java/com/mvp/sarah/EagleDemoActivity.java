package com.mvp.sarah;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import ai.picovoice.android.voiceprocessor.VoiceProcessor;
import ai.picovoice.android.voiceprocessor.VoiceProcessorException;
import ai.picovoice.eagle.*;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class EagleDemoActivity extends AppCompatActivity {
    private static final String ACCESS_KEY = "YOUR_ACCESS_KEY_HERE"; // Replace with your Picovoice access key

    private EagleProfiler eagleProfiler;
    private Eagle eagle;
    private final List<EagleProfile> profiles = new ArrayList<>();
    private final ArrayList<Short> enrollmentPcm = new ArrayList<>();
    private float[] smoothScores;
    private final VoiceProcessor voiceProcessor = VoiceProcessor.getInstance();

    private TextView statusText;
    private Button enrollButton, testButton;

    private String getAccessKey() {
        android.content.SharedPreferences prefs = getSharedPreferences("SaraSettingsPrefs", MODE_PRIVATE);
        return prefs.getString("picovoice_access_key", "");
    }

    private void saveProfile(EagleProfile profile, int speakerIndex) {
        try {
            String filename = "eagle_profile_" + speakerIndex + ".pv";
            FileOutputStream fos = openFileOutput(filename, MODE_PRIVATE);
            fos.write(profile.getBytes());
            fos.close();
        } catch (IOException e) {
            runOnUiThread(() -> statusText.setText("Failed to save profile: " + e.getMessage()));
        }
    }

    private List<EagleProfile> loadProfiles() {
        List<EagleProfile> loadedProfiles = new ArrayList<>();
        int i = 1;
        while (true) {
            String filename = "eagle_profile_" + i + ".pv";
            try {
                FileInputStream fis = openFileInput(filename);
                byte[] bytes = new byte[fis.available()];
                fis.read(bytes);
                fis.close();
                loadedProfiles.add(new EagleProfile(bytes));
                i++;
            } catch (IOException e) {
                break;
            }
        }
        return loadedProfiles;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        statusText = new TextView(this);
        enrollButton = new Button(this);
        testButton = new Button(this);
        enrollButton.setText("Enroll Speaker");
        testButton.setText("Test Speaker");
        setContentView(new android.widget.LinearLayout(this) {{
            setOrientation(VERTICAL);
            addView(statusText);
            addView(enrollButton);
            addView(testButton);
        }});

        enrollButton.setOnClickListener(v -> startEnrollment());
        testButton.setOnClickListener(v -> startTesting());

        // Request permission
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, 0);
        }

        String accessKey = getAccessKey();
        if (accessKey.isEmpty()) {
            statusText.setText("Picovoice access key not set. Please configure in settings.");
            enrollButton.setEnabled(false);
            testButton.setEnabled(false);
            return;
        }
        try {
            eagleProfiler = new EagleProfiler.Builder()
                    .setAccessKey(accessKey)
                    .build(getApplicationContext());
        } catch (EagleException e) {
            statusText.setText("EagleProfiler error: " + e.getMessage());
        }
        profiles.addAll(loadProfiles());
        statusText.setText("Enrolled speakers: " + profiles.size());
    }

    private void startEnrollment() {
        statusText.setText("Speak to enroll...");
        enrollmentPcm.clear();

        voiceProcessor.addFrameListener(frame -> {
            for (short sample : frame) {
                enrollmentPcm.add(sample);
            }
            if (enrollmentPcm.size() > eagleProfiler.getMinEnrollSamples()) {
                short[] enrollFrame = new short[enrollmentPcm.size()];
                for (int i = 0; i < enrollmentPcm.size(); i++) {
                    enrollFrame[i] = enrollmentPcm.get(i);
                }
                enrollmentPcm.clear();
                enrollSpeaker(enrollFrame);
            }
        });

        try {
            voiceProcessor.start(1024, eagleProfiler.getSampleRate());
        } catch (VoiceProcessorException e) {
            statusText.setText("VoiceProcessor error: " + e.getMessage());
        }
    }

    private void enrollSpeaker(short[] enrollFrame) {
        try {
            EagleProfilerEnrollResult result = eagleProfiler.enroll(enrollFrame);
            if (result.getFeedback() == EagleProfilerEnrollFeedback.AUDIO_OK && result.getPercentage() == 100) {
                stopAudio();
                EagleProfile profile = eagleProfiler.export();
                profiles.add(profile);
                saveProfile(profile, profiles.size());
                statusText.setText("Enrollment complete! Speaker #" + profiles.size());
            } else {
                statusText.setText("Enrollment: " + result.getFeedback() + " " + result.getPercentage() + "%");
            }
        } catch (EagleException e) {
            statusText.setText("Enroll error: " + e.getMessage());
        }
    }

    private void startTesting() {
        if (profiles.isEmpty()) {
            statusText.setText("No enrolled speakers!");
            return;
        }
        String accessKey = getAccessKey();
        if (accessKey.isEmpty()) {
            statusText.setText("Picovoice access key not set. Please configure in settings.");
            return;
        }
        try {
            eagle = new Eagle.Builder()
                    .setSpeakerProfiles(profiles.toArray(new EagleProfile[0]))
                    .setAccessKey(accessKey)
                    .build(getApplicationContext());
            smoothScores = new float[profiles.size()];
        } catch (EagleException e) {
            statusText.setText("Eagle error: " + e.getMessage());
            return;
        }

        statusText.setText("Testing... Speak now!");

        voiceProcessor.addFrameListener(frame -> {
            try {
                float[] scores = eagle.process(frame);
                for (int i = 0; i < scores.length; i++) {
                    float alpha = 0.25f;
                    smoothScores[i] = alpha * smoothScores[i] + (1 - alpha) * scores[i];
                }
                runOnUiThread(() -> statusText.setText("Scores: " + java.util.Arrays.toString(smoothScores)));
            } catch (EagleException e) {
                runOnUiThread(() -> statusText.setText("Test error: " + e.getMessage()));
            }
        });

        try {
            voiceProcessor.start(eagle.getFrameLength(), eagle.getSampleRate());
        } catch (VoiceProcessorException e) {
            statusText.setText("VoiceProcessor error: " + e.getMessage());
        }
    }

    private void stopAudio() {
        try {
            voiceProcessor.stop();
            voiceProcessor.clearFrameListeners();
        } catch (VoiceProcessorException e) {
            statusText.setText("Stop error: " + e.getMessage());
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopAudio();
        if (eagleProfiler != null) eagleProfiler.delete();
        if (eagle != null) eagle.delete();
    }

    // Handle permission result
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Microphone permission required", Toast.LENGTH_LONG).show();
            finish();
        }
    }
} 