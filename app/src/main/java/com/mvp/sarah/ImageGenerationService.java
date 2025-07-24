package com.mvp.sarah;

import android.app.IntentService;
import android.content.Intent;
import android.net.Uri;
import androidx.annotation.Nullable;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;

public class ImageGenerationService extends IntentService {

    public ImageGenerationService() {
        super("ImageGenerationService");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) return;
        String prompt = intent.getStringExtra("prompt");
        String apiKey = BuildConfig.GEMINI_API_KEY;
        try {
            JSONArray predictions = ImagenHelper.generateImages(prompt, 1, apiKey);
            JSONObject prediction = predictions.getJSONObject(0);
            String imageUrl = prediction.optString("imageUrl", null);
            String base64Image = prediction.optString("image", null);

            File imageFile = new File(getFilesDir(), "generated_image.jpg");
            if (imageUrl != null) {
                downloadImage(imageUrl, imageFile);
            } else if (base64Image != null) {
                byte[] imageBytes = android.util.Base64.decode(base64Image, android.util.Base64.DEFAULT);
                try (FileOutputStream fos = new FileOutputStream(imageFile)) {
                    fos.write(imageBytes);
                }
            }

            Intent displayIntent = new Intent(this, ImageDisplayActivity.class);
            displayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            displayIntent.setData(Uri.fromFile(imageFile));
            startActivity(displayIntent);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void downloadImage(String imageUrl, File destFile) throws IOException {
        URL url = new URL(imageUrl);
        try (InputStream in = url.openStream();
             FileOutputStream out = new FileOutputStream(destFile)) {
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        }
    }
} 