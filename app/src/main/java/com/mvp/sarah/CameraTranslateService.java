package com.mvp.sarah;

import android.app.Service;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.IBinder;
import android.util.Log;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import com.google.mlkit.vision.common.InputImage;
import com.google.mlkit.vision.text.Text;
import com.google.mlkit.vision.text.TextRecognition;
import com.google.mlkit.vision.text.TextRecognizer;
import com.google.mlkit.vision.text.latin.TextRecognizerOptions;
import java.io.File;
import java.io.FileInputStream;
import java.util.HashMap;
import java.util.Map;

public class CameraTranslateService extends Service {
    private TextRecognizer textRecognizer;
    private static final Map<String, String> langMap = new HashMap<>();
    static {
        langMap.put("french", TranslateLanguage.FRENCH);
        langMap.put("spanish", TranslateLanguage.SPANISH);
        langMap.put("german", TranslateLanguage.GERMAN);
        langMap.put("italian", TranslateLanguage.ITALIAN);
        langMap.put("hindi", TranslateLanguage.HINDI);
        // Add more...
    }

    @Override
    public void onCreate() {
        super.onCreate();
        textRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String imagePath = intent.getStringExtra("image_path");
        String targetLangName = intent.getStringExtra("target_lang");
        String targetLangCode = langMap.getOrDefault(targetLangName, TranslateLanguage.SPANISH);

        if (imagePath == null) {
            stopSelf();
            return START_NOT_STICKY;
        }

        try {
            File imgFile = new File(imagePath);
            Bitmap bitmap = BitmapFactory.decodeStream(new FileInputStream(imgFile));
            recognizeTextAndTranslate(bitmap, targetLangCode);
        } catch (Exception e) {
            Log.e("TranslateService", "Error processing image", e);
            FeedbackProvider.speakAndToast(this, "Sorry, I couldn't process the image for translation.");
            stopSelf();
        }

        return START_NOT_STICKY;
    }

    private void recognizeTextAndTranslate(Bitmap bitmap, String targetLangCode) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        textRecognizer.process(image)
            .addOnSuccessListener(visionText -> {
                if (visionText.getText().isEmpty()) {
                    FeedbackProvider.speakAndToast(this, "I couldn't find any text in the image.");
                    stopSelf();
                } else {
                    translateText(visionText.getText(), targetLangCode);
                }
            })
            .addOnFailureListener(e -> {
                Log.e("TranslateService", "Text recognition failed", e);
                FeedbackProvider.speakAndToast(this, "Sorry, I failed to recognize text.");
                stopSelf();
            });
    }

    private void translateText(String text, String targetLangCode) {
        TranslatorOptions options = new TranslatorOptions.Builder()
                .setSourceLanguage(TranslateLanguage.ENGLISH)
                .setTargetLanguage(targetLangCode)
                .build();
        Translator translator = Translation.getClient(options);

        DownloadConditions conditions = new DownloadConditions.Builder().requireWifi().build();
        translator.downloadModelIfNeeded(conditions)
            .addOnSuccessListener(v -> {
                translator.translate(text)
                    .addOnSuccessListener(translatedText -> {
                        FeedbackProvider.speakAndToast(this, "Translation: " + translatedText);
                        translator.close();
                        stopSelf();
                    })
                    .addOnFailureListener(e -> {
                        Log.e("TranslateService", "Translation failed", e);
                        FeedbackProvider.speakAndToast(this, "Sorry, I couldn't translate the text.");
                        translator.close();
                        stopSelf();
                    });
            })
            .addOnFailureListener(e -> {
                Log.e("TranslateService", "Model download failed", e);
                FeedbackProvider.speakAndToast(this, "Sorry, the translation model couldn't be downloaded.");
                translator.close();
                stopSelf();
            });
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
} 