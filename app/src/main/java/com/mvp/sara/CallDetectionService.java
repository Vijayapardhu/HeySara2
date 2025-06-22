package com.mvp.sara;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.Uri;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.content.pm.PackageManager;
import androidx.core.content.ContextCompat;
import android.Manifest;
import android.os.Handler;
import android.os.Looper;

public class CallDetectionService extends Service {
    private static final String TAG = "CallDetectionService";
    private TelephonyManager telephonyManager;
    private PhoneStateListener phoneStateListener;
    private boolean isCallAnnounced = false;
    private String currentCallerNumber = null;
    private String currentCallerName = null;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "CallDetectionService created");
        
        telephonyManager = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        setupPhoneStateListener();
    }

    private void setupPhoneStateListener() {
        phoneStateListener = new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String phoneNumber) {
                Log.d(TAG, "Call state changed: " + state + ", number: " + phoneNumber);
                
                switch (state) {
                    case TelephonyManager.CALL_STATE_RINGING:
                        if (!isCallAnnounced && phoneNumber != null) {
                            handleIncomingCall(phoneNumber);
                        }
                        break;
                    case TelephonyManager.CALL_STATE_OFFHOOK:
                        // Call was answered
                        isCallAnnounced = false;
                        currentCallerNumber = null;
                        currentCallerName = null;
                        break;
                    case TelephonyManager.CALL_STATE_IDLE:
                        // Call ended or was rejected
                        isCallAnnounced = false;
                        currentCallerNumber = null;
                        currentCallerName = null;
                        break;
                }
            }
        };
        
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_CALL_STATE);
    }

    private void handleIncomingCall(String phoneNumber) {
        Log.d(TAG, "Handling incoming call from: " + phoneNumber);
        
        currentCallerNumber = phoneNumber;
        currentCallerName = getContactName(phoneNumber);
        isCallAnnounced = true;
        
        // Announce the call using Sara's voice
        announceCall();
    }

    private String getContactName(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            Log.w(TAG, "Phone number is empty");
            return "Unknown";
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CONTACTS) 
                != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "READ_CONTACTS permission not granted");
            return "Unknown";
        }

        try {
            Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, 
                    Uri.encode(phoneNumber));
            Cursor cursor = getContentResolver().query(uri, 
                    new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, 
                    null, null, null);

            if (cursor != null && cursor.moveToFirst()) {
                String name = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
                cursor.close();
                Log.d(TAG, "Found contact name: " + name);
                return name;
            }
            
            if (cursor != null) {
                cursor.close();
            }
            
            Log.d(TAG, "No contact found for number: " + phoneNumber);
            return "Unknown";
            
        } catch (Exception e) {
            Log.e(TAG, "Error getting contact name: " + e.getMessage());
            return "Unknown";
        }
    }

    private void announceCall() {
        String announcement;
        if ("Unknown".equals(currentCallerName)) {
            announcement = "You have an incoming call from " + currentCallerNumber + 
                          ". Do you want to answer or reject?";
        } else {
            announcement = "You have an incoming call from " + currentCallerName + 
                          ". Do you want to answer or reject?";
        }
        
        Log.d(TAG, "Announcing call: " + announcement);
        FeedbackProvider.speakAndToast(this, announcement);
        
        // Start listening for voice commands to answer/reject
        startCallCommandListening();
        
        // Automatically start Sara's voice recognition for immediate command listening
        startAutomaticVoiceRecognition();
    }

    private void startCallCommandListening() {
        // Register a broadcast receiver to listen for call commands
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mvp.sara.CALL_COMMAND");
        registerReceiver(callCommandReceiver, filter, Context.RECEIVER_EXPORTED);
        
        Log.d(TAG, "Started listening for call commands");
    }

    private final BroadcastReceiver callCommandReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String command = intent.getStringExtra("command");
            Log.d(TAG, "Received call command: " + command);
            
            if ("answer".equalsIgnoreCase(command)) {
                answerCall();
            } else if ("reject".equalsIgnoreCase(command)) {
                rejectCall();
            }
        }
    };

    private void answerCall() {
        Log.d(TAG, "Answering call");
        FeedbackProvider.speakAndToast(this, "Answering call");
        
        // Use accessibility service to answer the call
        Intent intent = new Intent("com.mvp.sara.ACTION_ANSWER_CALL");
        intent.putExtra("phone_number", currentCallerNumber);
        sendBroadcast(intent);
    }

    private void rejectCall() {
        Log.d(TAG, "Rejecting call");
        FeedbackProvider.speakAndToast(this, "Rejecting call");
        
        // Use accessibility service to reject the call
        Intent intent = new Intent("com.mvp.sara.ACTION_REJECT_CALL");
        intent.putExtra("phone_number", currentCallerNumber);
        sendBroadcast(intent);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "CallDetectionService started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "CallDetectionService destroyed");
        
        if (phoneStateListener != null) {
            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);
        }
        
        try {
            unregisterReceiver(callCommandReceiver);
        } catch (Exception e) {
            Log.w(TAG, "Error unregistering receiver: " + e.getMessage());
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void startAutomaticVoiceRecognition() {
        Log.d(TAG, "Starting automatic voice recognition for call commands");
        
        // Send broadcast to SaraVoiceService to start listening immediately
        Intent intent = new Intent("com.mvp.sara.START_CALL_LISTENING");
        intent.putExtra("phone_number", currentCallerNumber);
        intent.putExtra("caller_name", currentCallerName);
        sendBroadcast(intent);
        
        // Set a timeout to stop listening after 30 seconds if no command is received
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            stopAutomaticVoiceRecognition();
        }, 30000); // 30 seconds timeout
    }
    
    private void stopAutomaticVoiceRecognition() {
        Log.d(TAG, "Stopping automatic voice recognition for call commands");
        
        // Send broadcast to SaraVoiceService to stop call listening mode
        Intent intent = new Intent("com.mvp.sara.STOP_CALL_LISTENING");
        sendBroadcast(intent);
    }
} 