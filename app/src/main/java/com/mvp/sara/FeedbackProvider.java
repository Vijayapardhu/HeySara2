package com.mvp.sara;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;

public class FeedbackProvider {

    private static TextToSpeech tts;
    private static boolean isInitialized = false;

    public static void init(Context context) {
        if (tts != null) {
            return;
        }
        tts = new TextToSpeech(context.getApplicationContext(), status -> {
            if (status == TextToSpeech.SUCCESS) {
                int result = tts.setLanguage(Locale.getDefault());
                if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("TTS", "The specified language is not supported!");
                } else {
                    isInitialized = true;
                    Log.i("TTS", "TextToSpeech engine initialized.");
                }
            } else {
                Log.e("TTS", "TextToSpeech initialization failed!");
            }
        });
    }

    public static void speakAndToast(Context context, String message, int duration) {
    

        if (isInitialized && tts != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(message, TextToSpeech.QUEUE_ADD, null, null);
            } else {
                //noinspection deprecation
                tts.speak(message, TextToSpeech.QUEUE_ADD, null);
            }
        } else {
            Log.w("TTS", "TTS not ready, cannot speak message.");
        }
    }

    public static void speakAndToast(Context context, String message) {
        speakAndToast(context, message, Toast.LENGTH_SHORT);
    }

    public static void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isInitialized = false;
        }
    }
} 