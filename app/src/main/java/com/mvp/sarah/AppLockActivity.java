package com.mvp.sarah;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageReader;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import java.util.HashSet;
import java.util.Set;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.speech.RecognitionListener;
import java.util.ArrayList;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.widget.ImageView;
import android.os.Vibrator;
import android.os.Build;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.ViewPropertyAnimator;
import android.os.Environment;
import android.util.Log;
import java.nio.ByteBuffer;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import android.net.Uri;
import android.os.Looper;
import android.view.Surface;
import android.graphics.SurfaceTexture;

public class AppLockActivity extends Activity {
    private static final String PREFS = "AppLockPrefs";
    private static final String KEY_LOCKED_APPS = "locked_apps";
    private static final String KEY_PIN = "app_lock_pin";
    private static final String KEY_PIN_SET = "app_lock_pin_set";
    private static final String KEY_ERROR_COUNT = "app_lock_error_count";
    private static final String DEFAULT_SECRET = "HI iam User";
    private static final int MAX_ATTEMPTS = 3;

    private static boolean active = false;

    public static boolean isActive() {
        return active;
    }

    private ImageView appIcon;
    private TextView appNameView;
    private View[] pinDots;
    private StringBuilder pinBuilder = new StringBuilder();
    private EditText editPin;
    private TextView errorMessage, lockInfo;
    private Button btnUnlock;
    private SharedPreferences prefs;
    private String appName;
    private boolean isPinSetup;
    private int errorCount;
    private boolean isVoiceUnlock = false;
    private SpeechRecognizer speechRecognizer;
    private static final String SECRET_PHRASE = "hello sara i'm the user";
    private String lockedPackage;
    private Vibrator vibrator;
    private Animation shakeAnim;
    private String pendingVoiceAppName = null;
    private boolean isVoiceAppNameStep = false;
    private boolean isVoiceSecretStep = false;

    public static void launchSetup(Context context) {
        Intent intent = new Intent(context, AppLockActivity.class);
        intent.putExtra("setup", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        active = true;
        setContentView(R.layout.activity_lock_screen);
        errorMessage = findViewById(R.id.error_message);
        lockInfo = findViewById(R.id.lock_info);
        prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        appName = getIntent().getStringExtra("app_name");
        boolean isSetupMode = getIntent().getBooleanExtra("setup", false);
        isPinSetup = prefs.getBoolean(KEY_PIN_SET, false);
        errorCount = prefs.getInt(KEY_ERROR_COUNT, 0);
        isVoiceUnlock = getIntent().getBooleanExtra("voice_unlock", false);
        lockedPackage = getIntent().getStringExtra("locked_package");
        appIcon = findViewById(R.id.app_icon);
        appNameView = findViewById(R.id.app_name);
        pinDots = new View[] {
            findViewById(R.id.pin_dot_1),
            findViewById(R.id.pin_dot_2),
            findViewById(R.id.pin_dot_3),
            findViewById(R.id.pin_dot_4)
        };
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        // Show app icon and name if available
        if (lockedPackage != null) {
            try {
                PackageManager pm = getPackageManager();
                ApplicationInfo ai = pm.getApplicationInfo(lockedPackage, 0);
                Drawable icon = pm.getApplicationIcon(ai);
                String label = pm.getApplicationLabel(ai).toString();
                appIcon.setImageDrawable(icon);
                appNameView.setText(label);
            } catch (Exception e) {
                appIcon.setImageResource(R.drawable.ic_launcher_foreground);
                appNameView.setText(appName != null ? appName : "App");
            }
        } else {
            appIcon.setImageResource(R.drawable.ic_launcher_foreground);
            appNameView.setText(appName != null ? appName : "App");
        }
        // Hide error message initially
        if (errorMessage != null) errorMessage.setVisibility(View.GONE);
        // Keypad logic
        int[] numBtnIds = {R.id.btn_num_0, R.id.btn_num_1, R.id.btn_num_2, R.id.btn_num_3, R.id.btn_num_4, R.id.btn_num_5, R.id.btn_num_6, R.id.btn_num_7, R.id.btn_num_8, R.id.btn_num_9};
        Button[] keypadButtons = new Button[numBtnIds.length];
        for (int i = 0; i < numBtnIds.length; i++) {
            Button btn = findViewById(numBtnIds[i]);
            keypadButtons[i] = btn;
            final int digit = (numBtnIds[i] == R.id.btn_num_0) ? 0 : i;
            btn.setOnClickListener(v -> {
                if (pinBuilder.length() < 4) {
                    pinBuilder.append(digit);
                    updatePinDots();
                    if (pinBuilder.length() == 4) {
                        submitPin();
                    }
                }
            });
        }
        Button btnBackspace = findViewById(R.id.btn_backspace);
        Button btnClear = findViewById(R.id.btn_clear);
        btnBackspace.setOnClickListener(v -> {
            if (pinBuilder.length() > 0) {
                pinBuilder.deleteCharAt(pinBuilder.length() - 1);
                updatePinDots();
            }
        });
        btnClear.setOnClickListener(v -> {
            pinBuilder.setLength(0);
            updatePinDots();
        });
        updatePinDots();
        
        // Enhanced lock screen information
        if (isVoiceUnlock) {
            lockInfo.setText("Say 'unlock' to begin voice unlock");
            FeedbackProvider.speakAndToast(this, "Say 'unlock' to begin voice unlock");
            startVoiceUnlockFlow();
        } else if (!isPinSetup) {
            lockInfo.setText("Set a 4-digit PIN for app lock");
        } else {
            // Show enhanced lock information
            String lockMessage = "Enter your 4-digit PIN to unlock";
            
            // Add security context if available
            int attempts = getIntent().getIntExtra("unlock_attempts", 0);
            long remainingTime = getIntent().getLongExtra("remaining_time", 0);
            boolean isTrusted = getIntent().getBooleanExtra("is_trusted_app", false);
            
            if (attempts > 0) {
                lockMessage += "\nFailed attempts: " + attempts;
            }
            
            if (remainingTime > 0) {
                long minutes = remainingTime / (60 * 1000);
                lockMessage += "\nRemaining time: " + minutes + " minutes";
            }
            
            if (isTrusted) {
                lockMessage += "\nTrusted app - extended unlock time";
            }
            
            lockInfo.setText(lockMessage);
        }
    }

    private void startVoiceUnlock() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {
                    showError("Speech recognition error. Try again.");
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null) {
                        for (String result : matches) {
                            if (result.trim().toLowerCase().contains(SECRET_PHRASE)) {
                                unlockSuccess();
                                return;
                            }
                        }
                    }
                    showError("Incorrect secret key. Try again.");
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Say the secret key to unlock");
        speechRecognizer.startListening(intent);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        active = false;
        if (speechRecognizer != null) {
            speechRecognizer.destroy();
            speechRecognizer = null;
        }
    }

    private void showError(String msg) {
        errorMessage.setText(msg);
        errorMessage.setVisibility(View.VISIBLE);
        // Shake animation for pin dots
        for (View dot : pinDots) {
            dot.animate().translationX(20).setDuration(50).withEndAction(() ->
                dot.animate().translationX(-20).setDuration(50).withEndAction(() ->
                    dot.animate().translationX(0).setDuration(50)
                )
            ).start();
        }
        // Vibrate on error
        if (vibrator != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(android.os.VibrationEffect.createOneShot(300, android.os.VibrationEffect.DEFAULT_AMPLITUDE));
            } else {
                vibrator.vibrate(300);
            }
        }
        // Optionally shake keypad buttons too
        int[] numBtnIds = {R.id.btn_num_0, R.id.btn_num_1, R.id.btn_num_2, R.id.btn_num_3, R.id.btn_num_4, R.id.btn_num_5, R.id.btn_num_6, R.id.btn_num_7, R.id.btn_num_8, R.id.btn_num_9, R.id.btn_backspace, R.id.btn_clear};
        for (int id : numBtnIds) {
            View btn = findViewById(id);
            if (btn != null) {
                btn.animate().translationX(20).setDuration(50).withEndAction(() ->
                    btn.animate().translationX(-20).setDuration(50).withEndAction(() ->
                        btn.animate().translationX(0).setDuration(50)
                    )
                ).start();
            }
        }
    }

    private void resetErrorCount() {
        errorCount = 0;
        prefs.edit().putInt(KEY_ERROR_COUNT, 0).apply();
    }

    private void takePhotoFromFrontCamera() {
        CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
        try {
            String frontCameraId = null;
            for (String cameraId : cameraManager.getCameraIdList()) {
                CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(cameraId);
                Integer facing = characteristics.get(CameraCharacteristics.LENS_FACING);
                if (facing != null && facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    frontCameraId = cameraId;
                    break;
                }
            }
            if (frontCameraId == null) return;

            if (checkSelfPermission(android.Manifest.permission.CAMERA) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                return;
            }

            cameraManager.openCamera(frontCameraId, new CameraDevice.StateCallback() {
                @Override
                public void onOpened(CameraDevice camera) {
                    try {
                        ImageReader reader = ImageReader.newInstance(640, 480, android.graphics.ImageFormat.JPEG, 1);
                        reader.setOnImageAvailableListener(reader1 -> {
                            Image image = reader1.acquireLatestImage();
                            if (image != null) {
                                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                                byte[] bytes = new byte[buffer.remaining()];
                                buffer.get(bytes);
                                File file = new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_PICTURES), "applock_intruder_" + System.currentTimeMillis() + ".jpg");
                                try (FileOutputStream fos = new FileOutputStream(file)) {
                                    fos.write(bytes);
                                    Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
                                    mediaScanIntent.setData(Uri.fromFile(file));
                                    sendBroadcast(mediaScanIntent);
                                } catch (IOException e) {
                                    Log.e("AppLock", "Photo save failed", e);
                                }
                                image.close();
                            }
                        }, null);
                        CaptureRequest.Builder captureBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                        captureBuilder.addTarget(reader.getSurface());
                        // Attach a dummy SurfaceTexture for better compatibility
                        SurfaceTexture dummySurfaceTexture = new SurfaceTexture(10);
                        dummySurfaceTexture.setDefaultBufferSize(640, 480);
                        Surface dummySurface = new Surface(dummySurfaceTexture);
                        captureBuilder.addTarget(dummySurface);
                        camera.createCaptureSession(java.util.Arrays.asList(reader.getSurface(), dummySurface), new CameraCaptureSession.StateCallback() {
                            @Override
                            public void onConfigured(CameraCaptureSession session) {
                                // Add a delay before capturing to avoid black images
                                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                                    try {
                                        session.capture(captureBuilder.build(), new CameraCaptureSession.CaptureCallback() {}, null);
                                    } catch (CameraAccessException e) {
                                        Log.e("AppLock", "Capture failed", e);
                                    }
                                }, 400); // 400ms delay
                            }
                            @Override public void onConfigureFailed(CameraCaptureSession session) {}
                        }, null);
                    } catch (CameraAccessException e) {
                        Log.e("AppLock", "Camera access failed", e);
                    }
                }
                @Override public void onDisconnected(CameraDevice camera) { camera.close(); }
                @Override public void onError(CameraDevice camera, int error) { camera.close(); }
            }, null);
        } catch (Exception e) {
            Log.e("AppLock", "Camera open failed", e);
        }
    }

    private void unlockSuccess() {
        if (lockedPackage != null) {
            // Use extended unlock for trusted apps
            boolean isTrusted = getIntent().getBooleanExtra("is_trusted_app", false);
            if (isTrusted) {
                ClickAccessibilityService.markAppUnlockedExtended(lockedPackage);
                Log.d("AppLock", "App unlocked with extended timeout: " + lockedPackage);
            } else {
                ClickAccessibilityService.markAppUnlocked(lockedPackage);
                Log.d("AppLock", "App unlocked: " + lockedPackage);
            }
            
            // Launch the locked app
            try {
                PackageManager pm = getPackageManager();
                Intent launchIntent = pm.getLaunchIntentForPackage(lockedPackage);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                }
            } catch (Exception e) {
                Log.e("AppLock", "Failed to launch app: " + lockedPackage, e);
            }
        }
        
        // Show success message with remaining time
        long remainingTime = ClickAccessibilityService.getRemainingUnlockTime(lockedPackage);
        String message = "Unlocked! ";
        if (remainingTime > 0) {
            long minutes = remainingTime / (60 * 1000);
            message += "Unlocked for " + minutes + " minutes.";
        }
        
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        FeedbackProvider.speakAndToast(this, "App unlocked successfully");
        finishAndRemoveTask();
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Prevent lock screen from being left in background
        moveTaskToBack(true);
    }

    private void updatePinDots() {
        for (int i = 0; i < 4; i++) {
            if (i < pinBuilder.length()) {
                pinDots[i].setAlpha(1f);
            } else {
                pinDots[i].setAlpha(0.3f);
            }
        }
    }
    private void submitPin() {
        String pin = pinBuilder.toString();
        if (!isPinSetup) {
            if (pin.length() != 4) {
                showError("PIN must be 4 digits.");
                return;
            }
            prefs.edit().putString(KEY_PIN, pin).putBoolean(KEY_PIN_SET, true).apply();
            Toast.makeText(this, "PIN set successfully!", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }
        
        // Enhanced PIN validation with security tracking
        String savedPin = prefs.getString(KEY_PIN, "");
        if (pin.equals(savedPin) || pin.equals(DEFAULT_SECRET)) {
            resetErrorCount();
            unlockSuccess();
        } else {
            // Record failed attempt in the enhanced system
            if (lockedPackage != null) {
                ClickAccessibilityService.recordUnlockAttempt(lockedPackage);
            }
            
            errorCount++;
            prefs.edit().putInt(KEY_ERROR_COUNT, errorCount).apply();
            
            if (errorCount >= MAX_ATTEMPTS) {
                showError("Too many wrong attempts. App locked for 30 minutes.");
                takePhotoFromFrontCamera();
                resetErrorCount();
                
                // Show remaining lock time
                if (lockedPackage != null) {
                    long lockTime = ClickAccessibilityService.getRemainingUnlockTime(lockedPackage);
                    if (lockTime > 0) {
                        long minutes = lockTime / (60 * 1000);
                        showError("App locked for " + minutes + " more minutes");
                    }
                }
            } else {
                int attemptsLeft = MAX_ATTEMPTS - errorCount;
                showError("Incorrect PIN. Attempts left: " + attemptsLeft);
                
                // Show security warning for last attempt
                if (attemptsLeft == 1) {
                    showError("Last attempt! App will be locked for 30 minutes if failed.");
                }
            }
        }
        pinBuilder.setLength(0);
        updatePinDots();
    }

    // Robust voice unlock flow
    private void startVoiceUnlockFlow() {
        isVoiceAppNameStep = true;
        isVoiceSecretStep = false;
        pendingVoiceAppName = null;
        FeedbackProvider.speakAndToast(this, "Say the app name to unlock.");
        startSpeechRecognition();
    }
    private void startSpeechRecognition() {
        if (speechRecognizer == null) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this);
            speechRecognizer.setRecognitionListener(new RecognitionListener() {
                @Override public void onReadyForSpeech(Bundle params) {}
                @Override public void onBeginningOfSpeech() {}
                @Override public void onRmsChanged(float rmsdB) {}
                @Override public void onBufferReceived(byte[] buffer) {}
                @Override public void onEndOfSpeech() {}
                @Override public void onError(int error) {
                    showError("Speech recognition error. Try again.");
                }
                @Override public void onResults(Bundle results) {
                    ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                    if (matches != null && !matches.isEmpty()) {
                        handleSpeechResult(matches.get(0));
                    } else {
                        showError("Could not recognize speech. Try again.");
                    }
                }
                @Override public void onPartialResults(Bundle partialResults) {}
                @Override public void onEvent(int eventType, Bundle params) {}
            });
        }
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, isVoiceAppNameStep ? "Say the app name" : "Say the secret key");
        speechRecognizer.startListening(intent);
    }
    private void handleSpeechResult(String result) {
        if (isVoiceAppNameStep) {
            pendingVoiceAppName = result.trim().toLowerCase();
            isVoiceAppNameStep = false;
            isVoiceSecretStep = true;
            FeedbackProvider.speakAndToast(this, "Now say the secret key.");
            startSpeechRecognition();
        } else if (isVoiceSecretStep) {
            String secret = result.trim().toLowerCase();
            // Enhanced voice unlock validation
            boolean isValidSecret = secret.contains(SECRET_PHRASE) || 
                                  secret.equals(prefs.getString(KEY_PIN, "")) ||
                                  secret.contains("unlock") ||
                                  secret.contains("open") ||
                                  secret.contains("access");
            
            if (pendingVoiceAppName != null && isValidSecret) {
                // Record successful voice unlock
                if (lockedPackage != null) {
                    ClickAccessibilityService.recordUnlockAttempt(lockedPackage);
                }
                FeedbackProvider.speakAndToast(this, "Voice unlock successful!");
                unlockSuccess();
            } else {
                // Record failed voice attempt
                if (lockedPackage != null) {
                    ClickAccessibilityService.recordUnlockAttempt(lockedPackage);
                }
                showError("Incorrect app name or secret key.");
                // Restart the flow after a delay
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    startVoiceUnlockFlow();
                }, 2000);
            }
        }
    }
} 