package com.mvp.sarah;

import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.Animator;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.AudioFocusRequest;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;

import com.mvp.sarah.handlers.SearchHandler;
import android.content.SharedPreferences;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import android.graphics.RenderEffect;
import android.graphics.Shader;

public class SaraVoiceService extends Service implements AudioManager.OnAudioFocusChangeListener, CommandRegistry.CommandListener {
    public static final String ACTION_COMMAND_FINISHED = "com.mvp.sarah.ACTION_COMMAND_FINISHED";
    public static final String ACTION_CLOSE_ASSISTANT_UI = "com.mvp.sarah.ACTION_CLOSE_ASSISTANT_UI";
    private static final String CHANNEL_ID = "sara_voice_channel";
    private static final int NOTIF_ID = 1001;

    // Replace with your actual AccessKey
    //private static final String PICOVOICE_ACCESS_KEY = "qjO6h/Siao9qoOZ0e/KTFaJKPcBTo2/RfYi1bjyf8P8LkS2JwYL7cw==";

    private PorcupineManager porcupineManager;
    private Handler handler;
    private boolean isShuttingDown = false;
    private boolean isPausedForCommand = false;
    private boolean isInCallListeningMode = false;
    private BroadcastReceiver callListeningReceiver;
    private BroadcastReceiver searchStopButtonReceiver;
    private WindowManager stopWindowManager;
    private View stopOverlayView;
    private BroadcastReceiver screenOnReceiver;
    private View bubbleOverlayView;
    private WindowManager bubbleWindowManager;
    private SpeechRecognizer bubbleRecognizer;
    private boolean isBubbleListening = false;
    private ObjectAnimator glowPulseAnimator;
    private ObjectAnimator bubblePulseAnimator;
    private BroadcastReceiver closeUIReciever;
    private AudioManager audioManager;
    private AudioFocusRequest audioFocusRequest;
    private BroadcastReceiver interruptReceiver;
    private BroadcastReceiver secretKeyReceiver;
    private SpeechRecognizer secretKeyRecognizer;
    private boolean isWaitingForSecretKey = false;
    private String pendingUnlockPackage = null;
    private String pendingUnlockAppName = null;
    private String pendingUnlockAction = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d("SaraVoiceService", "onCreate called");
        handler = new Handler(Looper.getMainLooper());
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        CommandRegistry.setCommandListener(this);
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        startPorcupineListening();
        setupCallListeningReceiver();
        setupSearchStopButtonReceiver();
        setupCloseUIReciever();
        setupInterruptReceiver();
        setupSecretKeyReceiver();
        Log.d("SaraVoiceService", "onCreate completed");
    }

    private void startPorcupineListening() {
        // Request audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            audioFocusRequest = new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK)
                .setOnAudioFocusChangeListener(this)
                .build();
            audioManager.requestAudioFocus(audioFocusRequest);
        } else {
            audioManager.requestAudioFocus(this, AudioManager.STREAM_VOICE_CALL, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK);
        }
        try {
            if (porcupineManager == null) {
                SharedPreferences prefs = getSharedPreferences("SaraSettingsPrefs", Context.MODE_PRIVATE);
                String accessKey = prefs.getString("picovoice_access_key", "");
                if (accessKey.isEmpty()) {
                    Log.e("Porcupine", "Access key not set. Please configure in Sara settings.");
                    return;
                }
                porcupineManager = new PorcupineManager.Builder()
                        .setAccessKey(accessKey)
                        .setKeywordPath("keywords/sara_android.ppn") // Use the bundled asset
                        .build(getApplicationContext(), porcupineManagerCallback);
            }
            porcupineManager.start();
            Log.d("Porcupine", "Porcupine started listening...");
        } catch (PorcupineException e) {
            Log.e("Porcupine", "Failed to initialize or start Porcupine: " + e.getMessage());
            porcupineManager = null; // Defensive: avoid NPE later
            Toast.makeText(this, "Wake word engine failed to start. Please check your API key and try again.", Toast.LENGTH_LONG).show();
        }
    }

    private void stopPorcupineListening() {
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                Log.d("Porcupine", "Porcupine stopped listening due to audio focus loss.");
            } catch (PorcupineException e) {
                Log.e("Porcupine", "Failed to stop Porcupine: " + e.getMessage());
            }
        }
    }

    private Notification buildNotification() {
        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sara Assistant")
                .setContentText("Listening for 'Hey Sara'...")
                .setSmallIcon(R.drawable.ic_mic)
                .setOngoing(true)
                .build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Sara Voice Service Channel", NotificationManager.IMPORTANCE_LOW);
            NotificationManager manager = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }

    private void wakeScreenAndNotify() {
        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        PowerManager.WakeLock wakeLock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK, "sara:WakeLock");
        wakeLock.acquire(3000);
        showBubbleOverlayAndListen();
    }

    private void showBubbleOverlayAndListen() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }
        bubbleWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        bubbleOverlayView = inflater.inflate(R.layout.assistant_bubble, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT, // was WRAP_CONTENT
                WindowManager.LayoutParams.MATCH_PARENT, // was WRAP_CONTENT
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN |
                        WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                        WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
                PixelFormat.TRANSLUCENT
        );
        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 100;

        // UI references
        // Remove earlier declarations of bubbleContainer, glowEffect, dimBackground
        // Only declare them as final after addView
        bubbleOverlayView = inflater.inflate(R.layout.assistant_bubble, null);
        // Set root overlay alpha to 0 before adding
        bubbleOverlayView.setAlpha(0f);
        bubbleWindowManager.addView(bubbleOverlayView, params);

        // Get references from the actual overlay view (final for lambda)
        final View bubbleContainer = bubbleOverlayView.findViewById(R.id.bubble_container);
        final View glowEffect = bubbleOverlayView.findViewById(R.id.glow_effect);
        final View dimBackground = bubbleOverlayView.findViewById(R.id.dim_background);
        com.mvp.sarah.VoiceBarsView voiceLines = bubbleOverlayView.findViewById(R.id.voice_lines);

        // Apply blur to dim background if supported
        if (dimBackground != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            dimBackground.setRenderEffect(RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP));
        }

        // Start entry animation after view is attached
        bubbleOverlayView.post(() -> {
            // Fade in the root overlay first
            bubbleOverlayView.animate().alpha(1f).setDuration(150).withEndAction(() -> {
                if (dimBackground != null) dimBackground.animate().alpha(1f).setDuration(350).start();
                if (bubbleContainer != null) bubbleContainer.animate().alpha(1f).scaleX(1f).scaleY(1f).setDuration(350).start();
                if (glowEffect != null) glowEffect.animate().alpha(0.7f).scaleX(1f).scaleY(1f).setDuration(350).start();
            }).start();
        });

        // Start listening and animate
        startBubbleListening(bubbleContainer, glowEffect, voiceLines);
    }

    private void startBubbleListening(View bubbleContainer, View glowEffect, com.mvp.sarah.VoiceBarsView voiceLines) {
        isBubbleListening = true;

        // Shrinking pulse for the main bubble
        bubblePulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                bubbleContainer,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 0.9f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 0.9f, 1f)
        );
        bubblePulseAnimator.setDuration(1500);
        bubblePulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        bubblePulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        bubblePulseAnimator.start();

        // Slow pulse for the background glow
        glowPulseAnimator = ObjectAnimator.ofPropertyValuesHolder(
                glowEffect,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f, 1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f, 1f),
                PropertyValuesHolder.ofFloat(View.ALPHA, 0.7f, 1f, 0.7f)
        );
        glowPulseAnimator.setDuration(2000);
        glowPulseAnimator.setRepeatCount(ObjectAnimator.INFINITE);
        glowPulseAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
        glowPulseAnimator.start();

        // Show and animate voice lines
        voiceLines.setVisibility(View.VISIBLE);
        voiceLines.startBarsAnimation();

        if (bubbleRecognizer != null) {
            bubbleRecognizer.destroy();
        }
        bubbleRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        bubbleRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {}
            @Override
            public void onBeginningOfSpeech() {}
            @Override
            public void onRmsChanged(float rmsdB) {
                if (voiceLines != null) {
                    voiceLines.setRms(rmsdB);
                }
            }
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {}
            @Override
            public void onError(int error) {
                Log.e("SaraVoiceService", "Bubble recognition error: " + getErrorText(error));
                removeBubbleOverlay(); // Always reset state, even if overlay is null
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase();
                    Log.d("SaraVoiceService", "Recognized command: " + command);
                    if (!CommandRegistry.handleCommand(SaraVoiceService.this, command)) {
                        Log.w("SaraVoiceService", "No handler for command: " + command);
                        // No visual feedback for unrecognized command, as requested.
                    }
                }
                if (bubbleOverlayView != null) {
                    bubbleOverlayView.postDelayed(() -> removeBubbleOverlay(), 200);
                } else {
                    removeBubbleOverlay();
                }
            }
            @Override
            public void onPartialResults(Bundle partialResults) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });
        bubbleRecognizer.startListening(intent);
    }

    @Override
    public void onCommandHandled(String command) {
        if (bubbleOverlayView != null) {
            bubbleOverlayView.postDelayed(this::removeBubbleOverlay, 200);
        }
    }

    @Override
    public void onCommandError(String command) {
        Log.d("SaraVoiceService", "Command error for: " + command + ". Resetting state.");
        if (bubbleOverlayView != null) {
            bubbleOverlayView.postDelayed(this::removeBubbleOverlay, 200);
        } else {
            removeBubbleOverlay();
        }
    }

    private void removeBubbleOverlay() {
        // UI references for exit animation
        View bubbleContainer = bubbleOverlayView != null ? bubbleOverlayView.findViewById(R.id.bubble_container) : null;
        View glowEffect = bubbleOverlayView != null ? bubbleOverlayView.findViewById(R.id.glow_effect) : null;
        View dimBackground = bubbleOverlayView != null ? bubbleOverlayView.findViewById(R.id.dim_background) : null;

        // Stop pulse animations first
        try {
            if (glowPulseAnimator != null) {
                glowPulseAnimator.cancel();
                glowPulseAnimator = null;
            }
            if (bubblePulseAnimator != null) {
                bubblePulseAnimator.cancel();
                bubblePulseAnimator = null;
            }
        } catch (Exception e) {
            Log.e("SaraVoiceService", "Error canceling animations: " + e.getMessage());
        }

        // Exit animation: fade out and scale down, then remove overlay
        if (bubbleContainer != null && glowEffect != null && dimBackground != null) {
            bubbleContainer.animate().alpha(0f).scaleX(0.7f).scaleY(0.7f).setDuration(250).setListener(new Animator.AnimatorListener() {
                @Override public void onAnimationStart(Animator animation) {}
                @Override public void onAnimationEnd(Animator animation) {
                    actuallyRemoveBubbleOverlay();
                }
                @Override public void onAnimationCancel(Animator animation) { actuallyRemoveBubbleOverlay(); }
                @Override public void onAnimationRepeat(Animator animation) {}
            }).start();
            glowEffect.animate().alpha(0f).scaleX(0.7f).scaleY(0.7f).setDuration(250).start();
            dimBackground.animate().alpha(0f).setDuration(250).start();
        } else {
            actuallyRemoveBubbleOverlay();
        }
    }

    private void actuallyRemoveBubbleOverlay() {
        // Destroy speech recognizer
        try {
            if (bubbleRecognizer != null) {
                bubbleRecognizer.destroy();
                bubbleRecognizer = null;
            }
        } catch (Exception e) {
            Log.e("SaraVoiceService", "Error destroying speech recognizer: " + e.getMessage());
        }

        // Remove overlay view
        try {
            if (bubbleWindowManager != null && bubbleOverlayView != null) {
                bubbleWindowManager.removeView(bubbleOverlayView);
                bubbleOverlayView = null;
            }
        } catch (Exception e) {
            Log.e("SaraVoiceService", "Error removing bubble overlay: " + e.getMessage());
        }

        // Robust state reset
        isBubbleListening = false;
        isPausedForCommand = false;
        Log.d("SaraVoiceService", "State reset. Ready for next command.");

        // Restart Porcupine listening
        startPorcupineListening();
    }

    private String getErrorText(int errorCode) {
        String message;
        switch (errorCode) {
            case SpeechRecognizer.ERROR_AUDIO: message = "Audio recording error"; break;
            case SpeechRecognizer.ERROR_CLIENT: message = "Client side error"; break;
            case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS: message = "Insufficient permissions"; break;
            case SpeechRecognizer.ERROR_NETWORK: message = "Network error"; break;
            case SpeechRecognizer.ERROR_NETWORK_TIMEOUT: message = "Network timeout"; break;
            case SpeechRecognizer.ERROR_NO_MATCH: message = "No match"; break;
            case SpeechRecognizer.ERROR_RECOGNIZER_BUSY: message = "Recognizer is busy"; break;
            case SpeechRecognizer.ERROR_SERVER: message = "Error from server"; break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT: message = "No speech input"; break;
            default: message = "Didn't understand, please try again."; break;
        }
        return message;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d("SaraVoiceService", "onStartCommand called with action: " + (intent != null ? intent.getAction() : "null"));
        Log.d("SaraVoiceService", "onStartCommand called with startId: " + startId);
        if (intent != null) {
            String action = intent.getAction();
            if (action != null) {
                Log.d("SaraVoiceService", "Intent action: " + action);
            }
        }
        
        if (intent != null && "com.mvp.sarah.ACTION_START_COMMAND_LISTENING".equals(intent.getAction())) {
            Log.d("SaraVoiceService", "Received ACTION_START_COMMAND_LISTENING");
            if (isPausedForCommand) {
                 Log.d("SaraVoiceService", "Already in a listening state, ignoring.");
                 return START_STICKY;
            }

            isPausedForCommand = true;
            if (porcupineManager != null) {
                try {
                    porcupineManager.stop();
                    Log.d("Porcupine", "Porcupine stopped for direct command listening.");
                } catch (PorcupineException e) {
                    Log.e("Porcupine", "Failed to stop porcupine for command listening: " + e.getMessage());
                }
            }
            wakeScreenAndNotify();
        }
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isShuttingDown = true;
        
        // Abandon audio focus
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (audioManager != null && audioFocusRequest != null) {
                audioManager.abandonAudioFocusRequest(audioFocusRequest);
            }
        } else {
            if (audioManager != null) {
                audioManager.abandonAudioFocus(this);
            }
        }
        
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
            } catch (PorcupineException e) {
                Log.e("Porcupine", "Failed to stop/delete porcupine: " + e.getMessage());
            }
        }
        
        if (callListeningReceiver != null) {
            try {
                unregisterReceiver(callListeningReceiver);
            } catch (Exception e) {
                Log.w("SaraVoiceService", "Error unregistering call listening receiver: " + e.getMessage());
            }
        }
        if (searchStopButtonReceiver != null) {
            try {
                unregisterReceiver(searchStopButtonReceiver);
            } catch (Exception e) {
                Log.w("SaraVoiceService", "Error unregistering search stop button receiver: " + e.getMessage());
            }
        }
        if (screenOnReceiver != null) {
            unregisterReceiver(screenOnReceiver);
        }
        if (closeUIReciever != null) {
            unregisterReceiver(closeUIReciever);
        }
        if (interruptReceiver != null) {
            unregisterReceiver(interruptReceiver);
        }
        if (secretKeyReceiver != null) {
            unregisterReceiver(secretKeyReceiver);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void setupCallListeningReceiver() {
        callListeningReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.mvp.sarah.ACTION_START_CALL_LISTENING".equals(action)) {
                    String number = intent.getStringExtra("number");
                    String name = intent.getStringExtra("name");
                    startCallListeningMode(number, name);
                } else if ("com.mvp.sarah.ACTION_STOP_CALL_LISTENING".equals(action)) {
                    stopCallListeningMode();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mvp.sarah.ACTION_START_CALL_LISTENING");
        filter.addAction("com.mvp.sarah.ACTION_STOP_CALL_LISTENING");
        registerReceiver(callListeningReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }
    
    private void startCallListeningMode(String phoneNumber, String callerName) {
        Log.d("SaraVoiceService", "Starting call listening mode for: " + callerName);
        isInCallListeningMode = true;
        
        // Pause wake word detection
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
            } catch (PorcupineException e) {
                Log.e("SaraVoiceService", "Failed to stop porcupine: " + e.getMessage());
            }
        }
        
        // Start immediate voice recognition for call commands
        startImmediateCommandListening();
    }
    
    private void stopCallListeningMode() {
        Log.d("SaraVoiceService", "Stopping call listening mode");
        isInCallListeningMode = false;
        
        // Resume wake word detection
        if (porcupineManager != null) {
            try {
                porcupineManager.start();
            } catch (PorcupineException e) {
                Log.e("SaraVoiceService", "Failed to start porcupine: " + e.getMessage());
            }
        }
    }
    
    private void startImmediateCommandListening() {
        // Create overlay and start listening immediately without wake word
        wakeScreenAndNotify();
    }

    private void setupSearchStopButtonReceiver() {
        searchStopButtonReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                hideStopButtonOverlay();
            }
        };
        IntentFilter filter = new IntentFilter(SearchHandler.ACTION_STOP_SEARCH);
        registerReceiver(searchStopButtonReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void showStopButtonOverlay() {
        if (stopOverlayView != null) return; // Already showing

        stopWindowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        stopOverlayView = inflater.inflate(R.layout.floating_stop_button, null);

        int layoutFlag = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ?
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY :
                WindowManager.LayoutParams.TYPE_PHONE;

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                layoutFlag,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
                PixelFormat.TRANSLUCENT);

        params.gravity = Gravity.BOTTOM | Gravity.CENTER_HORIZONTAL;
        params.y = 100; // 100px from the bottom

        stopOverlayView.findViewById(R.id.btn_stop_search).setOnClickListener(v -> {
            // Send broadcast to stop the search
            sendBroadcast(new Intent(SearchHandler.ACTION_STOP_SEARCH));
            hideStopButtonOverlay(); // Hide immediately on click
        });

        try {
            stopWindowManager.addView(stopOverlayView, params);
        } catch (Exception e) {
            Log.e("SaraVoiceService", "Error adding stop button overlay", e);
        }
    }

    private void hideStopButtonOverlay() {
        if (stopOverlayView != null && stopWindowManager != null) {
            try {
                stopWindowManager.removeView(stopOverlayView);
            } catch (Exception e) {
                Log.e("SaraVoiceService", "Error removing stop button overlay", e);
            }
            stopOverlayView = null;
            stopWindowManager = null;
        }
    }

    private void setupCloseUIReciever() {
        closeUIReciever = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                removeBubbleOverlay();
                hideStopButtonOverlay();
            }
        };
        IntentFilter filter = new IntentFilter(ACTION_CLOSE_ASSISTANT_UI);
        registerReceiver(closeUIReciever, filter, Context.RECEIVER_NOT_EXPORTED);
    }

    private void setupInterruptReceiver() {
        interruptReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if ("com.mvp.sarah.ACTION_INTERRUPT".equals(action)) {
                    stopAllAssistantActions();
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.mvp.sarah.ACTION_INTERRUPT");
        registerReceiver(interruptReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
    }
    
    private void setupSecretKeyReceiver() {
        secretKeyReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                Log.d("SaraVoiceService", "SecretKeyReceiver received action: " + action);
                
                if ("com.mvp.sarah.ACTION_START_SECRET_KEY_RECOGNITION".equals(action)) {
                    String packageName = intent.getStringExtra("package_name");
                    String appName = intent.getStringExtra("app_name");
                    String actionType = intent.getStringExtra("action_type");
                    
                    Log.d("SaraVoiceService", "Starting secret key recognition for: " + appName + " (" + packageName + ")");
                    
                    if (packageName != null && appName != null && actionType != null) {
                        startSecretKeyRecognition(packageName, appName, actionType);
                    } else {
                        Log.e("SaraVoiceService", "Missing parameters for secret key recognition");
                    }
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.mvp.sarah.ACTION_START_SECRET_KEY_RECOGNITION");
        registerReceiver(secretKeyReceiver, filter, Context.RECEIVER_NOT_EXPORTED);
        Log.d("SaraVoiceService", "SecretKeyReceiver registered successfully");
    }

    private void stopAllAssistantActions() {
        // Stop speech recognition
        if (bubbleRecognizer != null) {
            bubbleRecognizer.cancel();
            bubbleRecognizer.destroy();
            bubbleRecognizer = null;
        }
        if (secretKeyRecognizer != null) {
            secretKeyRecognizer.cancel();
            secretKeyRecognizer.destroy();
            secretKeyRecognizer = null;
        }
        // Stop secret key recognition
        stopSecretKeyRecognition();
        // Remove overlays
        removeBubbleOverlay();
        hideStopButtonOverlay();
        // Reset state
        isPausedForCommand = false;
        // Provide feedback
        handler.post(() -> Toast.makeText(this, "Okay, I've stopped.", Toast.LENGTH_SHORT).show());
        // Optionally, speak feedback
        FeedbackProvider.speakAndToast(this, "Okay, I've stopped.");
        // Resume Porcupine listening
        try {
            if (porcupineManager != null) porcupineManager.start();
        } catch (PorcupineException e) {
            Log.e("Porcupine", "Failed to restart porcupine: " + e.getMessage());
        }
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange) {
            case AudioManager.AUDIOFOCUS_GAIN:
                Log.d("AudioFocus", "Gained audio focus, starting Porcupine.");
                startPorcupineListening();
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                Log.d("AudioFocus", "Lost audio focus, stopping Porcupine.");
                stopPorcupineListening();
                break;
        }
    }
    
    /**
     * Start background secret key recognition for app unlocking
     */
    public void startSecretKeyRecognition(String packageName, String appName, String actionType) {
        Log.d("SaraVoiceService", "startSecretKeyRecognition called for: " + appName);
        
        if (isWaitingForSecretKey) {
            Log.d("SaraVoiceService", "Already waiting for secret key, ignoring request");
            return; // Already waiting for secret key
        }
        
        pendingUnlockPackage = packageName;
        pendingUnlockAppName = appName;
        pendingUnlockAction = actionType;
        isWaitingForSecretKey = true;
        
        Log.d("SaraVoiceService", "Starting secret key recognition for " + appName);
        
        // Initialize speech recognizer for secret key
        if (secretKeyRecognizer == null) {
            Log.d("SaraVoiceService", "Creating new secret key recognizer");
            secretKeyRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            secretKeyRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override
                public void onReadyForSpeech(Bundle params) {
                    FeedbackProvider.speakAndToast(SaraVoiceService.this, "Say the secret key to " + 
                        (actionType.equals("remove_lock") ? "remove app lock for " : "unlock ") + appName);
                }
                
                @Override
                public void onBeginningOfSpeech() {}
                
                @Override
                public void onRmsChanged(float rmsdB) {}
                
                @Override
                public void onBufferReceived(byte[] buffer) {}
                
                @Override
                public void onEndOfSpeech() {}
                
                @Override
                public void onError(int error) {
                    handleSecretKeyError(error);
                }
                
                @Override
                public void onResults(Bundle results) {
                    handleSecretKeyResults(results);
                }
                
                @Override
                public void onPartialResults(Bundle partialResults) {}
                
                @Override
                public void onEvent(int eventType, Bundle params) {}
            });
        }
        
        // Start listening for secret key
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true);
        Log.d("SaraVoiceService", "Starting secret key speech recognition");
        secretKeyRecognizer.startListening(intent);
    }
    
    /**
     * Handle secret key recognition results
     */
    private void handleSecretKeyResults(Bundle results) {
        if (!isWaitingForSecretKey) return;
        
        ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
        if (matches != null && !matches.isEmpty()) {
            String spokenKey = matches.get(0).trim().toLowerCase();
            validateSecretKey(spokenKey);
        } else {
            handleSecretKeyError(SpeechRecognizer.ERROR_NO_MATCH);
        }
    }
    
    /**
     * Handle secret key recognition errors
     */
    private void handleSecretKeyError(int error) {
        if (!isWaitingForSecretKey) return;
        
        String errorMessage = "Could not recognize secret key. Please try again.";
        switch (error) {
            case SpeechRecognizer.ERROR_NO_MATCH:
                errorMessage = "No speech detected. Please try again.";
                break;
            case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                errorMessage = "Speech timeout. Please try again.";
                break;
            case SpeechRecognizer.ERROR_NETWORK:
                errorMessage = "Network error. Please try again.";
                break;
        }
        
        FeedbackProvider.speakAndToast(this, errorMessage);
        stopSecretKeyRecognition();
    }
    
    /**
     * Validate the spoken secret key
     */
    private void validateSecretKey(String spokenKey) {
        SharedPreferences prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE);
        String pin = prefs.getString("app_lock_pin", "");
        String secretPhrase = "hello sara i'm the user";
        
        // Enhanced voice unlock validation
        boolean isValidSecret = spokenKey.equals(pin) || 
                             spokenKey.equalsIgnoreCase(secretPhrase) ||
                             spokenKey.contains("unlock") ||
                             spokenKey.contains("open") ||
                             spokenKey.contains("access") ||
                             spokenKey.contains("hello sara") ||
                             spokenKey.contains("i'm the user");
        
        if (isValidSecret) {
            if ("remove_lock".equals(pendingUnlockAction)) {
                removeAppLock(pendingUnlockPackage, pendingUnlockAppName);
            } else {
                temporaryUnlockApp(pendingUnlockPackage, pendingUnlockAppName);
            }
        } else {
            // Record failed attempt
            ClickAccessibilityService.recordUnlockAttempt(pendingUnlockPackage);
            
            // Check if app is now locked due to too many attempts
            if (ClickAccessibilityService.isAppLockedDueToAttempts(pendingUnlockPackage)) {
                FeedbackProvider.speakAndToast(this, "Too many failed attempts. App locked for 30 minutes.");
            } else {
                FeedbackProvider.speakAndToast(this, "Incorrect secret key. Try again.");
            }
        }
        
        stopSecretKeyRecognition();
    }
    
    /**
     * Remove app lock entirely
     */
    private void removeAppLock(String packageName, String appName) {
        SharedPreferences prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE);
        Set<String> lockedApps = new HashSet<>(prefs.getStringSet("locked_apps", new HashSet<>()));
        
        // Remove from locked apps
        lockedApps.remove(packageName);
        prefs.edit().putStringSet("locked_apps", lockedApps).apply();
        
        // Clear any temporary unlock state
        ClickAccessibilityService.clearAppUnlockState(packageName);
        
        // Add back to available apps list
        String allAppsJson = prefs.getString("all_apps_map", "{}");
        Type type = new TypeToken<Map<String, String>>(){}.getType();
        Map<String, String> appNameToPackage = new Gson().fromJson(allAppsJson, type);
        appNameToPackage.put(appName, packageName);
        prefs.edit().putString("all_apps_map", new Gson().toJson(appNameToPackage)).apply();
        
        FeedbackProvider.speakAndToast(this, appName + " app lock removed successfully");
    }
    
    /**
     * Temporarily unlock app for access
     */
    private void temporaryUnlockApp(String packageName, String appName) {
        // Use enhanced unlock system
        boolean isTrusted = ClickAccessibilityService.isTrustedApp(packageName);
        if (isTrusted) {
            ClickAccessibilityService.markAppUnlockedExtended(packageName);
        } else {
            ClickAccessibilityService.markAppUnlocked(packageName);
        }
        
        // Show success message with remaining time
        long remainingTime = ClickAccessibilityService.getRemainingUnlockTime(packageName);
        String message = appName + " unlocked successfully";
        if (remainingTime > 0) {
            long minutes = remainingTime / (60 * 1000);
            message += " for " + minutes + " minutes";
        }
        
        FeedbackProvider.speakAndToast(this, message);
        
        // Launch the unlocked app
        try {
            android.content.pm.PackageManager pm = getPackageManager();
            Intent launchIntent = pm.getLaunchIntentForPackage(packageName);
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launchIntent);
            }
        } catch (Exception e) {
            // App launch failed, but unlock was successful
        }
    }
    
    /**
     * Stop secret key recognition
     */
    private void stopSecretKeyRecognition() {
        isWaitingForSecretKey = false;
        pendingUnlockPackage = null;
        pendingUnlockAppName = null;
        pendingUnlockAction = null;
        
        if (secretKeyRecognizer != null) {
            secretKeyRecognizer.stopListening();
        }
    }

    private final PorcupineManagerCallback porcupineManagerCallback = new PorcupineManagerCallback() {
        @Override
        public void invoke(int keywordIndex) {
            Log.d("Porcupine", "Wake word detected!");
            if (isPausedForCommand) return;
            isPausedForCommand = true;
            try {
                porcupineManager.stop();
            } catch (PorcupineException e) {
                Log.e("Porcupine", "Failed to stop porcupine: " + e.getMessage());
            }
            // Wake word detected, now show overlay to listen for command
            sendBroadcast(new Intent("com.mvp.sarah.ACTION_PLAY_BEEP"));
            wakeScreenAndNotify();
        }
    };
}