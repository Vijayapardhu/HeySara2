package com.mvp.sara;

import ai.picovoice.porcupine.PorcupineActivationException;
import ai.picovoice.porcupine.PorcupineActivationLimitException;
import ai.picovoice.porcupine.PorcupineActivationRefusedException;
import ai.picovoice.porcupine.PorcupineActivationThrottledException;
import ai.picovoice.porcupine.PorcupineException;
import ai.picovoice.porcupine.PorcupineInvalidArgumentException;
import ai.picovoice.porcupine.PorcupineManager;
import ai.picovoice.porcupine.PorcupineManagerCallback;

import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.PixelFormat;
import android.media.AudioAttributes;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;

import com.mvp.sara.handlers.SearchHandler;

public class SaraVoiceService extends Service {
    public static final String ACTION_COMMAND_FINISHED = "com.mvp.sara.ACTION_COMMAND_FINISHED";
    private static final String CHANNEL_ID = "sara_voice_channel";
    private static final int NOTIF_ID = 1001;

    // Replace with your actual AccessKey
    private static final String PICOVOICE_ACCESS_KEY = "qjO6h/Siao9qoOZ0e/KTFaJKPcBTo2/RfYi1bjyf8P8LkS2JwYL7cw==";

    private PorcupineManager porcupineManager;
    private Handler handler;
    private boolean isShuttingDown = false;
    private boolean isPausedForCommand = false;
    private boolean isInCallListeningMode = false;
    private BroadcastReceiver callListeningReceiver;
    private BroadcastReceiver searchStopButtonReceiver;
    private WindowManager stopWindowManager;
    private View stopOverlayView;

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
            sendBroadcast(new Intent("com.mvp.sara.ACTION_PLAY_BEEP"));
            wakeScreenAndNotify();
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        handler = new Handler(Looper.getMainLooper());
        createNotificationChannel();
        startForeground(NOTIF_ID, buildNotification());
        startPorcupineListening();
        setupCallListeningReceiver();
        setupSearchStopButtonReceiver();
    }

    private void startPorcupineListening() {
        try {
            porcupineManager = new PorcupineManager.Builder()
                    .setAccessKey(PICOVOICE_ACCESS_KEY)
                    .setKeywordPath("keywords/sara_android.ppn")
                    .build(getApplicationContext(), porcupineManagerCallback);

            porcupineManager.start();
            Log.d("Porcupine", "Porcupine started listening...");
        } catch (PorcupineInvalidArgumentException e) {
            Log.e("Porcupine", "Porcupine Invalid Argument: " + e.getMessage());
        } catch (PorcupineActivationException e) {
            Log.e("Porcupine", "AccessKey activation error");
        } catch (PorcupineActivationLimitException e) {
            Log.e("Porcupine", "AccessKey reached its device limit");
        } catch (PorcupineActivationRefusedException e) {
            Log.e("Porcupine", "AccessKey refused");
        } catch (PorcupineActivationThrottledException e) {
            Log.e("Porcupine", "AccessKey has been throttled");
        } catch (PorcupineException e) {
            Log.e("Porcupine", "Failed to initialize Porcupine: " + e.getMessage());
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

        createOverlayWindow();
    }

    private void createOverlayWindow() {
        if (!Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
            intent.setData(android.net.Uri.parse("package:" + getPackageName()));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            return;
        }

        WindowManager windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        LayoutInflater inflater = LayoutInflater.from(this);
        View overlayView = inflater.inflate(R.layout.dialog_voice_popup, null);

        WindowManager.LayoutParams params = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
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
        params.y = 150; // Pixels from bottom

        windowManager.addView(overlayView, params);

        startCommandListening(overlayView, windowManager, params);
    }

    private void startCommandListening(View overlayView, WindowManager windowManager, WindowManager.LayoutParams params) {
        VoiceBarsView barsView = overlayView.findViewById(R.id.voice_bars);
        View circleContainer = overlayView.findViewById(R.id.voice_circle_container);

        startOverlayAnimations(circleContainer, barsView);

        SpeechRecognizer commandRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);

        commandRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {
                Log.d("SaraVoiceService", "Ready for command");
            }
            @Override
            public void onBeginningOfSpeech() {
                Log.d("SaraVoiceService", "Beginning of command");
            }
            @Override
            public void onRmsChanged(float rmsdB) {}
            @Override
            public void onBufferReceived(byte[] buffer) {}
            @Override
            public void onEndOfSpeech() {
                Log.d("SaraVoiceService", "End of command");
            }
            @Override
            public void onError(int error) {
                String errorMessage = getErrorText(error);
                Log.e("SaraVoiceService", "Command recognition error: " + errorMessage);
                FeedbackProvider.speakAndToast(SaraVoiceService.this, "Error: " + errorMessage, Toast.LENGTH_LONG);
                removeOverlay(windowManager, overlayView, commandRecognizer);
            }
            @Override
            public void onResults(Bundle results) {
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                if (matches != null && !matches.isEmpty()) {
                    String command = matches.get(0).toLowerCase();
                    Log.d("SaraVoiceService", "Recognized command: " + command);
                    if (!CommandRegistry.handleCommand(SaraVoiceService.this, command)) {
                        Log.d("SaraVoiceService", "No handler matched for: " + command);
                        FeedbackProvider.speakAndToast(SaraVoiceService.this, "Command not recognized", Toast.LENGTH_SHORT);
                    }
                } else {
                    Log.d("SaraVoiceService", "No command recognized");
                    FeedbackProvider.speakAndToast(SaraVoiceService.this, "Didn't catch that. Please try again!", Toast.LENGTH_SHORT);
                }
                removeOverlay(windowManager, overlayView, commandRecognizer);
            }
            @Override
            public void onPartialResults(Bundle partialResults) {}
            @Override
            public void onEvent(int eventType, Bundle params) {}
        });

        commandRecognizer.startListening(intent);
        Log.d("SaraVoiceService", "Command recognizer started in overlay");
    }

    private void startOverlayAnimations(View circleContainer, VoiceBarsView barsView) {
        ObjectAnimator scaleAnim = ObjectAnimator.ofPropertyValuesHolder(
                circleContainer,
                PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.1f),
                PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.1f)
        );
        scaleAnim.setDuration(600);
        scaleAnim.setRepeatCount(ObjectAnimator.INFINITE);
        scaleAnim.setRepeatMode(ObjectAnimator.REVERSE);
        scaleAnim.start();

        barsView.startBarsAnimation();
    }

    private void removeOverlay(WindowManager windowManager, View overlayView, SpeechRecognizer recognizer) {
        try {
            if (recognizer != null) {
                recognizer.destroy();
            }
            if (windowManager != null && overlayView != null) {
                windowManager.removeView(overlayView);
            }
        } catch (Exception e) {
            Log.e("SaraVoiceService", "Error removing overlay: " + e.getMessage());
        }

        // Restart Porcupine listening after the command is processed
        isPausedForCommand = false;
        try {
            porcupineManager.start();
            Log.d("Porcupine", "Porcupine restarted listening.");
        } catch (PorcupineException e) {
            Log.e("Porcupine", "Failed to restart porcupine: " + e.getMessage());
        }
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
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        isShuttingDown = true;
        
        if (porcupineManager != null) {
            try {
                porcupineManager.stop();
                porcupineManager.delete();
            } catch (PorcupineException e) {
                Log.e("Porcupine", "Failed to stop porcupine: " + e.getMessage());
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
                if ("com.mvp.sara.START_CALL_LISTENING".equals(action)) {
                    startCallListeningMode(intent.getStringExtra("phone_number"), 
                                         intent.getStringExtra("caller_name"));
                } else if ("com.mvp.sara.STOP_CALL_LISTENING".equals(action)) {
                    stopCallListeningMode();
                }
            }
        };
        
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mvp.sara.START_CALL_LISTENING");
        filter.addAction("com.mvp.sara.STOP_CALL_LISTENING");
        registerReceiver(callListeningReceiver, filter, Context.RECEIVER_EXPORTED);
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
                if (SearchHandler.ACTION_SHOW_STOP_BUTTON.equals(intent.getAction())) {
                    showStopButtonOverlay();
                } else if (SearchHandler.ACTION_HIDE_STOP_BUTTON.equals(intent.getAction())) {
                    hideStopButtonOverlay();
                }
            }
        };
        IntentFilter filter = new IntentFilter();
        filter.addAction(SearchHandler.ACTION_SHOW_STOP_BUTTON);
        filter.addAction(SearchHandler.ACTION_HIDE_STOP_BUTTON);
        registerReceiver(searchStopButtonReceiver, filter, Context.RECEIVER_EXPORTED);
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
}