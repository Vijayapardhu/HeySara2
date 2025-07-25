package com.mvp.sarah;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import ai.picovoice.eagle.EagleProfile;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class SpeakerManagementActivity extends AppCompatActivity {
    private List<EagleProfile> profiles = new ArrayList<>();
    private List<String> speakerNames = new ArrayList<>();
    private SpeakerAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_speaker_management);

        RecyclerView recycler = findViewById(R.id.speakerRecycler);
        recycler.setLayoutManager(new LinearLayoutManager(this));
        adapter = new SpeakerAdapter(speakerNames, new SpeakerAdapter.OnSpeakerActionListener() {
            @Override
            public void onDelete(int pos) {
                deleteProfile(pos);
            }
            @Override
            public void onRename(int pos) {
                showRenameDialog(pos);
            }
        });
        recycler.setAdapter(adapter);

        loadProfiles();
        updateSpeakerNames();

        if (profiles.isEmpty()) {
            new android.app.AlertDialog.Builder(this)
                .setTitle("No Voice Profile")
                .setMessage("You have not set your voice. Please add your own voice profile to use speaker identification.")
                .setPositiveButton("Add Now", (dialog, which) -> findViewById(R.id.enrollButton).performClick())
                .setNegativeButton("Cancel", null)
                .show();
        }

        findViewById(R.id.enrollButton).setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, EagleDemoActivity.class));
        });
    }

    private void loadProfiles() {
        profiles.clear();
        speakerNames.clear();
        int i = 1;
        while (true) {
            String filename = "eagle_profile_" + i + ".pv";
            try {
                FileInputStream fis = openFileInput(filename);
                byte[] bytes = new byte[fis.available()];
                fis.read(bytes);
                fis.close();
                profiles.add(new EagleProfile(bytes));
                speakerNames.add("Speaker " + i);
                i++;
            } catch (IOException e) {
                break;
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void updateSpeakerNames() {
        for (int i = 0; i < speakerNames.size(); i++) {
            if (speakerNames.get(i) == null || speakerNames.get(i).startsWith("Speaker ")) {
                speakerNames.set(i, "Speaker " + (i + 1));
            }
        }
        adapter.notifyDataSetChanged();
    }

    private void deleteProfile(int index) {
        String filename = "eagle_profile_" + (index + 1) + ".pv";
        deleteFile(filename);
        Toast.makeText(this, "Deleted " + speakerNames.get(index), Toast.LENGTH_SHORT).show();
        loadProfiles();
    }

    private void showRenameDialog(int index) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Rename Speaker");         
        final EditText input = new EditText(this);
        input.setText(speakerNames.get(index));
        builder.setView(input);
        builder.setPositiveButton("OK", (dialog, which) -> {
            speakerNames.set(index, input.getText().toString());
            adapter.notifyItemChanged(index);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
        builder.show();
    }
} 