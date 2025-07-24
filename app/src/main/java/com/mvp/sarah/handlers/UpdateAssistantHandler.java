package com.mvp.sarah.handlers;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.Toast;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import java.util.Arrays;
import java.util.List;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

public class UpdateAssistantHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("update the sara") || lower.contains("update the voice assistant");
    }

    @Override
    public void handle(Context context, String command) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("app_update").document("latest").get()
            .addOnSuccessListener((DocumentSnapshot document) -> {
                if (document.exists() && document.contains("apk_url")) {
                    String url = document.getString("apk_url");
                    try {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                        context.startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(context, "Unable to open update link.", Toast.LENGTH_LONG).show();
                    }
                } else {
                    Toast.makeText(context, "No update link found. Please check again later.", Toast.LENGTH_LONG).show();
                }
            })
            .addOnFailureListener(e -> Toast.makeText(context, "Failed to check for updates.", Toast.LENGTH_LONG).show());
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList("update the sara", "update the voice assistant");
    }
} 
 
 
 