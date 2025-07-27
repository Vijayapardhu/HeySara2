package com.mvp.sarah;

import android.content.Context;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.util.Log;
import android.widget.Toast;

import java.util.Locale;
import java.util.UUID;

public class FeedbackProvider {

    private static TextToSpeech tts;
    private static boolean isTtsInitialized = false;
    private static String pendingMessage = null;

    private static void initializeTts(Context context, final String messageToSpeak) {
        if (tts == null) {
            pendingMessage = messageToSpeak;
            tts = new TextToSpeech(context.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The specified language is not supported!");
                    } else {
                        isTtsInitialized = true;
                        Log.i("TTS", "TextToSpeech engine initialized successfully.");
                        if (pendingMessage != null) {
                            speak(pendingMessage);
                            pendingMessage = null;
                        }
                    }
                } else {
                    Log.e("TTS", "TextToSpeech initialization failed!");
                }
            });
        } else if (isTtsInitialized) {
            speak(messageToSpeak);
        } else {
            // TTS is initializing, queue the message
            pendingMessage = messageToSpeak;
        }
    }
    
    public static void shutdown() {
        if (tts != null) {
            tts.shutdown();
            tts = null;
            isTtsInitialized = false;
        }
    }

    public static void speakAndToast(Context context, String message, int duration) {
        initializeTts(context, message);
    }

    private static void speak(String message) {
        if (isTtsInitialized && tts != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(message, TextToSpeech.QUEUE_ADD, null, null);
            } else {
                //noinspection deprecation
                tts.speak(message, TextToSpeech.QUEUE_ADD, null);
            }
        } else {
            Log.w("TTS", "TTS not ready or null, cannot speak message.");
        }
    }

    public static void speakAndToast(Context context, String message) {
        speakAndToast(context, message, Toast.LENGTH_SHORT);
    }

    public static void speakAndToastWithCallback(Context context, String message, Runnable onDone) {
        initializeTtsWithCallback(context, message, onDone);
    }

    private static void initializeTtsWithCallback(Context context, final String messageToSpeak, final Runnable onDone) {
        if (tts == null) {
            tts = new TextToSpeech(context.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    int result = tts.setLanguage(Locale.getDefault());
                    if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                        Log.e("TTS", "The specified language is not supported!");
                    } else {
                        isTtsInitialized = true;
                        Log.i("TTS", "TextToSpeech engine initialized successfully.");
                        speakWithCallback(messageToSpeak, onDone);
                    }
                } else {
                    Log.e("TTS", "TextToSpeech initialization failed!");
                }
            });
        } else if (isTtsInitialized) {
            speakWithCallback(messageToSpeak, onDone);
        } else {
            // TTS is initializing, queue the message and callback
            pendingMessage = messageToSpeak;
            // Not handling callback queue for simplicity
        }
    }

    private static void speakWithCallback(String message, Runnable onDone) {
        if (isTtsInitialized && tts != null) {
            String utteranceId = UUID.randomUUID().toString();
            tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                @Override
                public void onStart(String utteranceId) {}
                @Override
                public void onDone(String utteranceId) {
                    if (onDone != null) onDone.run();
                }
                @Override
                public void onError(String utteranceId) {
                    if (onDone != null) onDone.run();
                }
            });
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(message, TextToSpeech.QUEUE_ADD, null, utteranceId);
            } else {
                //noinspection deprecation
                tts.speak(message, TextToSpeech.QUEUE_ADD, null);
            }
        } else {
            Log.w("TTS", "TTS not ready or null, cannot speak message.");
            if (onDone != null) onDone.run();
        }
    }

    /*public static void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
            tts = null;
            isTtsInitialized = false;
            pendingMessage = null;
        }
    }
}

     */
}