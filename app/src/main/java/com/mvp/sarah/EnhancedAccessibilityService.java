package com.mvp.sarah;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.WindowManager;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Intent;
import android.provider.Settings;
import android.provider.Telephony;
import android.telephony.TelephonyManager;
import android.provider.ContactsContract;
import android.net.Uri;
import android.media.AudioManager;
import android.os.PowerManager;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.hardware.camera2.CameraManager;
import android.database.Cursor;
import android.provider.CallLog;
import android.graphics.Bitmap;
import android.view.View;
import android.os.Environment;
import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.ArrayList;
import android.app.NotificationManager;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import android.view.KeyEvent;
import android.content.SharedPreferences;
import java.util.HashSet;
import android.content.pm.PackageManager;
import android.widget.Toast;

public class EnhancedAccessibilityService extends AccessibilityService {

    private static final String TAG = "EnhancedAccessibilitySvc";
    
    // State management
    private String currentPackageName;
    private String lastPackageName;
    private final long appLockTimeout = 30000L;
    private long lastAppLockTime;
    private final Set<String> lockedApps = new LinkedHashSet<>();
    private final Set<String> systemApps = new LinkedHashSet<>();
    private WindowManager windowManager;
    private View overlayView;
    private boolean isAppLockShowing = false;
    
    // Broadcast receiver
    private final BroadcastReceiver mainReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;
            
            switch (action) {
                case "com.mvp.sarah.ACTION_SWIPE_UP":
                    performSwipe("up");
                    break;
                case "com.mvp.sarah.ACTION_SWIPE_DOWN":
                    performSwipe("down");
                    break;
                case "com.mvp.sarah.ACTION_SWIPE_LEFT":
                    performSwipe("left");
                    break;
                case "com.mvp.sarah.ACTION_SWIPE_RIGHT":
                    performSwipe("right");
                    break;
                case "com.mvp.sarah.ACTION_SCROLL_UP":
                    performScrollUp();
                    break;
                case "com.mvp.sarah.ACTION_SCROLL_DOWN":
                    performScrollDown();
                    break;
                case "com.mvp.sarah.ACTION_SCROLL_LEFT":
                    performScrollLeft();
                    break;
                case "com.mvp.sarah.ACTION_SCROLL_RIGHT":
                    performScrollRight();
                    break;
                case "com.mvp.sarah.ACTION_CLICK_ON":
                    String clickText = intent.getStringExtra("text");
                    if (clickText != null) {
                        clickNodeWithText(clickText);
                    }
                    break;
                case "com.mvp.sarah.ACTION_TEXT_READ":
                    readScreen();
                    break;
                case "com.mvp.sarah.ACTION_TYPE_TEXT":
                    String text = intent.getStringExtra("text");
                    if (text != null) {
                        performTypeText(text);
                    }
                    break;
                case "com.mvp.sarah.ACTION_CAPTURE_PHOTO":
                    performPhotoCapture();
                    break;
                case "com.mvp.sarah.ACTION_RECORD_VIDEO":
                    performVideoRecording();
                    break;
                case "com.mvp.sarah.ACTION_SWITCH":
                    switchCamera();
                    break;
                case "com.mvp.sarah.ACTION_BLUETOOTH_ON":
                    toggleBluetooth();
                    break;
                case "com.mvp.sarah.ACTION_MOBILE_DATA":
                    toggleMobileData();
                    break;
                case "com.mvp.sarah.ACTION_WIFI":
                    toggleWifi();
                    break;
                case "com.mvp.sarah.ACTION_HOTSPOT":
                    String hotspotState = intent.getStringExtra("state");
                    toggleHotspot(hotspotState);
                    break;
                case "com.mvp.sarah.ACTION_ANSWER_CALL":
                    answerCall();
                    break;
                case "com.mvp.sarah.ACTION_REJECT_CALL":
                    rejectCall();
                    break;
                case "com.mvp.sarah.ACTION_PERFORM_BACK":
                    performBackAction();
                    break;
                case "com.mvp.sarah.ACTION_CLICK_POINT":
                    int x = intent.getIntExtra("x", -1);
                    int y = intent.getIntExtra("y", -1);
                    if (x != -1 && y != -1) {
                        performTapAtPosition(x, y);
                    }
                    break;
                // New hands-free phone commands
                case "com.mvp.sarah.ACTION_VOICE_DIAL":
                    String contactName = intent.getStringExtra("contact_name");
                    voiceDial(contactName);
                    break;
                case "com.mvp.sarah.ACTION_SEND_SMS":
                    String smsContact = intent.getStringExtra("contact_name");
                    String smsMessage = intent.getStringExtra("message");
                    sendSMS(smsContact, smsMessage);
                    break;
                case "com.mvp.sarah.ACTION_READ_SMS":
                    readLastSMS();
                    break;
                case "com.mvp.sarah.ACTION_MUTE_CALL":
                    muteCall();
                    break;
                case "com.mvp.sarah.ACTION_SPEAKER_ON":
                    toggleSpeaker();
                    break;
                case "com.mvp.sarah.ACTION_END_CALL":
                    endCall();
                    break;
                case "com.mvp.sarah.ACTION_OPEN_CONTACTS":
                    openContacts();
                    break;
                case "com.mvp.sarah.ACTION_OPEN_DIALER":
                    openDialer();
                    break;
                case "com.mvp.sarah.ACTION_OPEN_MESSAGES":
                    openMessages();
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_SILENT":
                    toggleSilentMode();
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_WIFI":
                    smartToggle("wifi");
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_BLUETOOTH":
                    smartToggle("bluetooth");
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_MOBILE_DATA":
                    smartToggle("data");
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_HOTSPOT":
                    smartToggle("hotspot");
                    break;
                case "com.mvp.sarah.ACTION_READ_QUICK_SETTINGS":
                    readQuickSettings();
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_QUICK_SETTING":
                    String settingName = intent.getStringExtra("setting_name");
                    boolean turnOn = intent.getBooleanExtra("turn_on", false);
                    toggleQuickSetting(settingName, turnOn);
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_GPS":
                    smartToggle("gps");
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_NFC":
                    smartToggle("nfc");
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_AIRPLANE":
                    smartToggle("airplane");
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_DND":
                    smartToggle("dnd");
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_FLASHLIGHT":
                    smartToggle("flashlight");
                    break;
                case "com.mvp.sarah.ACTION_TOGGLE_ROTATION":
                    smartToggle("rotation");
                    break;
                case "com.mvp.sarah.ACTION_SCREENSHOT":
                    takeScreenshot();
                    break;
                case "com.mvp.sarah.ACTION_OPEN_SETTINGS":
                    openSettings();
                    break;
                case "com.mvp.sarah.ACTION_OPEN_NOTIFICATIONS":
                    openNotifications();
                    break;
                case "com.mvp.sarah.ACTION_CLEAR_NOTIFICATIONS":
                    clearNotifications();
                    break;
            }
        }
    };

    // Add this to the class
    private final BroadcastReceiver screenOffReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.mvp.sarah.ACTION_SCREEN_OFF".equals(intent.getAction())) {
                Toast.makeText(context, "Screen off broadcast received", Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Screen off broadcast received");
                performGlobalAction(GLOBAL_ACTION_LOCK_SCREEN);
            }
        }
    };

    // Add these fields to the class
    private int volumeDownCount = 0;
    private long lastVolumeDownTime = 0;
    private static final int VOLUME_PRESS_EMERGENCY_COUNT = 4;
    private static final long VOLUME_PRESS_WINDOW_MS = 2000;

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "EnhancedAccessibilityService connected");
        
        // Initialize system apps list
        initializeSystemApps();
        
        // Configure accessibility service
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED | 
                         AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                         AccessibilityEvent.TYPE_VIEW_CLICKED |
                         AccessibilityEvent.TYPE_VIEW_FOCUSED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.DEFAULT | 
                    AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS | 
                    AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS |
                    AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS;
        info.notificationTimeout = 100L;
        setServiceInfo(info);

        // Initialize window manager
        windowManager = (WindowManager) getSystemService(Context.WINDOW_SERVICE);
        
        // Register broadcast receivers
        registerBroadcastReceivers();
        
        // Load locked apps
        loadLockedApps();
    }

    private void initializeSystemApps() {
        systemApps.add("com.android.systemui");
        systemApps.add("com.android.launcher");
        systemApps.add("com.google.android.apps.nexuslauncher");
        systemApps.add("com.sec.android.app.launcher");
        systemApps.add("com.miui.home");
        systemApps.add("com.huawei.android.launcher");
        systemApps.add("com.google.android.inputmethod.latin");
        systemApps.add("com.android.inputmethod.latin");
        systemApps.add("com.samsung.android.inputmethod");
        systemApps.add("android");
        systemApps.add("com.android.settings");
        systemApps.add("com.android.phone");
        systemApps.add("com.android.dialer");
    }

    private void registerBroadcastReceivers() {
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mvp.sarah.ACTION_SWIPE_UP");
        filter.addAction("com.mvp.sarah.ACTION_SWIPE_DOWN");
        filter.addAction("com.mvp.sarah.ACTION_SWIPE_LEFT");
        filter.addAction("com.mvp.sarah.ACTION_SWIPE_RIGHT");
        filter.addAction("com.mvp.sarah.ACTION_SCROLL_UP");
        filter.addAction("com.mvp.sarah.ACTION_SCROLL_DOWN");
        filter.addAction("com.mvp.sarah.ACTION_SCROLL_LEFT");
        filter.addAction("com.mvp.sarah.ACTION_SCROLL_RIGHT");
        filter.addAction("com.mvp.sarah.ACTION_CLICK_ON");
        filter.addAction("com.mvp.sarah.ACTION_TEXT_READ");
        filter.addAction("com.mvp.sarah.ACTION_TYPE_TEXT");
        filter.addAction("com.mvp.sarah.ACTION_CAPTURE_PHOTO");
        filter.addAction("com.mvp.sarah.ACTION_RECORD_VIDEO");
        filter.addAction("com.mvp.sarah.ACTION_SWITCH");
        filter.addAction("com.mvp.sarah.ACTION_BLUETOOTH_ON");
        filter.addAction("com.mvp.sarah.ACTION_MOBILE_DATA");
        filter.addAction("com.mvp.sarah.ACTION_WIFI");
        filter.addAction("com.mvp.sarah.ACTION_HOTSPOT");
        filter.addAction("com.mvp.sarah.ACTION_ANSWER_CALL");
        filter.addAction("com.mvp.sarah.ACTION_REJECT_CALL");
        filter.addAction("com.mvp.sarah.ACTION_PERFORM_BACK");
        filter.addAction("com.mvp.sarah.ACTION_CLICK_POINT");
        // New hands-free phone commands
        filter.addAction("com.mvp.sarah.ACTION_VOICE_DIAL");
        filter.addAction("com.mvp.sarah.ACTION_SEND_SMS");
        filter.addAction("com.mvp.sarah.ACTION_READ_SMS");
        filter.addAction("com.mvp.sarah.ACTION_MUTE_CALL");
        filter.addAction("com.mvp.sarah.ACTION_SPEAKER_ON");
        filter.addAction("com.mvp.sarah.ACTION_END_CALL");
        filter.addAction("com.mvp.sarah.ACTION_OPEN_CONTACTS");
        filter.addAction("com.mvp.sarah.ACTION_OPEN_DIALER");
        filter.addAction("com.mvp.sarah.ACTION_OPEN_MESSAGES");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_SILENT");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_WIFI");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_BLUETOOTH");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_MOBILE_DATA");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_HOTSPOT");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_GPS");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_NFC");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_AIRPLANE");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_DND");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_FLASHLIGHT");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_ROTATION");
        filter.addAction("com.mvp.sarah.ACTION_SCREENSHOT");
        filter.addAction("com.mvp.sarah.ACTION_OPEN_SETTINGS");
        filter.addAction("com.mvp.sarah.ACTION_OPEN_NOTIFICATIONS");
        filter.addAction("com.mvp.sarah.ACTION_CLEAR_NOTIFICATIONS");
        
        if (Build.VERSION.SDK_INT >= 33) {
            registerReceiver(mainReceiver, filter, RECEIVER_EXPORTED);
        } else {
            registerReceiver(mainReceiver, filter);
        }
    }

    private void loadLockedApps() {
        lockedApps.clear();
        // Add some example locked apps
        lockedApps.add("com.whatsapp");
        lockedApps.add("com.instagram.android");
        lockedApps.add("com.facebook.katana");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        try {
            int eventType = event.getEventType();
            
            if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
                handleWindowStateChanged(event);
            } else if (eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
                Log.d(TAG, "Content changed: " + event.getPackageName());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error in onAccessibilityEvent", e);
        }
    }

    private void handleWindowStateChanged(AccessibilityEvent event) {
        CharSequence packageName = event.getPackageName();
        if (packageName == null) return;
        String currentApp = packageName.toString();
        Log.d(TAG, "[AppLock] Current package: " + currentApp + ", Class: " + event.getClassName());
        if (currentApp.equals(lastPackageName)) {
            return;
        }
        lastPackageName = currentApp;
        // Check if it's a system app
        if (systemApps.contains(currentApp)) {
            Log.d(TAG, "[AppLock] Skipping system app: " + currentApp);
            return;
        }
        SharedPreferences prefs = getSharedPreferences("AppLockPrefs", Context.MODE_PRIVATE);
        Set<String> lockedApps = prefs.getStringSet("locked_apps", new HashSet<>());
        // Use the same unlocked logic as ClickAccessibilityService
        java.lang.reflect.Field unlockedField;
        Set<String> unlockedLockedApps = new HashSet<>();
        try {
            unlockedField = ClickAccessibilityService.class.getDeclaredField("unlockedLockedApps");
            unlockedField.setAccessible(true);
            Object unlockedObj = unlockedField.get(null);
            if (unlockedObj instanceof java.util.Map) {
                unlockedLockedApps = ((java.util.Map<String, Long>) unlockedObj).keySet();
            }
        } catch (Exception e) {
            // fallback: empty set
        }
        boolean isLocked = false;
        for (String locked : lockedApps) {
            if (currentApp.equals(locked)) {
                isLocked = true;
                break;
            }
        }
        if (isLocked && !unlockedLockedApps.contains(currentApp)) {
            if (!isAppLockShowing) {
                isAppLockShowing = true;
                Log.d(TAG, "[AppLock] Launching lock screen for: " + currentApp);
                Intent lockIntent = new Intent(this, AppLockActivity.class);
                lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                lockIntent.putExtra("app_name", currentApp);
                lockIntent.putExtra("locked_package", currentApp);
                startActivity(lockIntent);
                FeedbackProvider.speakAndToast(this, "App is locked. Please unlock to continue.");
            }
        } else {
            isAppLockShowing = false;
        }
    }

    public void performSwipe(String direction) {
        try {
            Log.d(TAG, "Performing swipe: " + direction);
            DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
            int screenWidth = displayMetrics.widthPixels;
            int screenHeight = displayMetrics.heightPixels;
            
            if (screenWidth == 0 || screenHeight == 0) {
                Log.e(TAG, "Invalid screen dimensions");
                return;
            }
            
            float centerX = screenWidth / 2f;
            float centerY = screenHeight / 2f;
            float startX, startY, endX, endY;
            
            switch (direction.toLowerCase()) {
                case "up":
                    startY = screenHeight * 0.75f;
                    endY = screenHeight * 0.25f;
                    startX = endX = centerX;
                    break;
                case "down":
                    startY = screenHeight * 0.25f;
                    endY = screenHeight * 0.75f;
                    startX = endX = centerX;
                    break;
                case "left":
                    startX = screenWidth * 0.95f;
                    endX = screenWidth * 0.05f;
                    startY = endY = centerY;
                    break;
                case "right":
                    startX = screenWidth * 0.05f;
                    endX = screenWidth * 0.95f;
                    startY = endY = centerY;
                    break;
                default:
                    Log.e(TAG, "Invalid swipe direction: " + direction);
                    return;
            }
            
            Path path = new Path();
            path.moveTo(startX, startY);
            path.lineTo(endX, endY);
            
            GestureDescription.StrokeDescription stroke = 
                new GestureDescription.StrokeDescription(path, 0, 300);
            
            GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
            
            dispatchGesture(gesture, new GestureResultCallback() {
                @Override
                public void onCompleted(GestureDescription gestureDescription) {
                    super.onCompleted(gestureDescription);
                    Log.d(TAG, "Swipe " + direction + " completed");
                    FeedbackProvider.speakAndToast(EnhancedAccessibilityService.this, 
                        "Swiped " + direction);
                }
                
                @Override
                public void onCancelled(GestureDescription gestureDescription) {
                    super.onCancelled(gestureDescription);
                    Log.w(TAG, "Swipe " + direction + " cancelled");
                    FeedbackProvider.speakAndToast(EnhancedAccessibilityService.this, 
                        "Swipe " + direction + " failed");
                }
            }, null);
            
        } catch (Exception e) {
            Log.e(TAG, "Error performing swipe: " + e.getMessage(), e);
        }
    }

    private void performScrollUp() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }
        
        AccessibilityNodeInfo scrollable = findScrollableNode(rootNode);
        if (scrollable != null) {
            boolean result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            scrollable.recycle();
            if (result) {
                FeedbackProvider.speakAndToast(this, "Scrolled up");
            } else {
                FeedbackProvider.speakAndToast(this, "Can't scroll up anymore");
            }
        } else {
            FeedbackProvider.speakAndToast(this, "No scrollable area found");
        }
        rootNode.recycle();
    }

    private void performScrollDown() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }
        
        AccessibilityNodeInfo scrollable = findScrollableNode(rootNode);
        if (scrollable != null) {
            boolean result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            scrollable.recycle();
            if (result) {
                FeedbackProvider.speakAndToast(this, "Scrolled down");
            } else {
                FeedbackProvider.speakAndToast(this, "Can't scroll down anymore");
            }
        } else {
            FeedbackProvider.speakAndToast(this, "No scrollable area found");
        }
        rootNode.recycle();
    }

    @SuppressWarnings("unchecked")
    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findScrollableNode(child);
            if (child != null) child.recycle();
            if (result != null) return result;
        }
        return null;
    }

    private void clickNodeWithText(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot perform click.");
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }

        // Try exact text match
        @SuppressWarnings("unchecked")
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        boolean clicked = tryClickNodes(nodes, text, "exact text");

        // Try partial text match
        if (!clicked) {
            clicked = findAndClickPartialText(rootNode, text.toLowerCase(Locale.ROOT));
        }

        // Try content description match
        if (!clicked) {
            clicked = findAndClickByContentDescription(rootNode, text.toLowerCase(Locale.ROOT));
        }

        if (!clicked) {
            FeedbackProvider.speakAndToast(this, "Couldn't find " + text);
        }

        rootNode.recycle();
    }

    private boolean tryClickNodes(List<AccessibilityNodeInfo> nodes, String text, String matchType) {
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null && node.isVisibleToUser()) {
                    AccessibilityNodeInfo clickableNode = findClickableAncestor(node);
                    if (clickableNode != null) {
                        Log.d(TAG, "Clicking ancestor node for " + matchType + ": " + text);
                        clickableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                        clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        clickableNode.recycle();
                        node.recycle();
                        return true;
                    }
                }
                if (node != null) node.recycle();
            }
        }
        return false;
    }

    private boolean findAndClickPartialText(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        boolean clicked = false;
        CharSequence nodeText = node.getText();
        if (nodeText != null && nodeText.toString().toLowerCase(Locale.ROOT).contains(text)) {
            AccessibilityNodeInfo clickableNode = findClickableAncestor(node);
            if (clickableNode != null) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                clickableNode.recycle();
                clicked = true;
            }
        }
        for (int i = 0; i < node.getChildCount() && !clicked; i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                clicked = findAndClickPartialText(child, text) || clicked;
                child.recycle();
            }
        }
        return clicked;
    }

    private boolean findAndClickByContentDescription(AccessibilityNodeInfo node, String text) {
        if (node == null) return false;
        boolean clicked = false;
        CharSequence desc = node.getContentDescription();
        if (desc != null && desc.toString().toLowerCase(Locale.ROOT).contains(text)) {
            AccessibilityNodeInfo clickableNode = findClickableAncestor(node);
            if (clickableNode != null) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                clickableNode.recycle();
                clicked = true;
            }
        }
        for (int i = 0; i < node.getChildCount() && !clicked; i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                clicked = findAndClickByContentDescription(child, text) || clicked;
                child.recycle();
            }
        }
        return clicked;
    }

    private AccessibilityNodeInfo findClickableAncestor(AccessibilityNodeInfo node) {
        AccessibilityNodeInfo current = node;
        while (current != null) {
            if (current.isClickable()) {
                return current;
            }
            AccessibilityNodeInfo parent = current.getParent();
            if (current != node) current.recycle();
            current = parent;
        }
        return null;
    }

    private void readScreen() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            FeedbackProvider.speakAndToast(this, "I can't access the screen right now.");
            return;
        }

        StringBuilder sb = new StringBuilder();
        traverseNode(rootNode, sb);
        rootNode.recycle();

        if (sb.length() > 0) {
            FeedbackProvider.speakAndToast(this, sb.toString());
        } else {
            FeedbackProvider.speakAndToast(this, "I couldn't find any text to read on the screen.");
        }
    }

    @SuppressWarnings("unchecked")
    private void traverseNode(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) return;

        if (node.getText() != null && !node.getText().toString().isEmpty()) {
            sb.append(node.getText().toString()).append(". ");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            traverseNode(child, sb);
        }
    }

    private void performTypeText(String text) {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node == null) {
            node = focusFirstEditableField();
        }
        if (node != null && node.isEditable()) {
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            boolean setTextResult = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            if (!setTextResult) {
                // Fallback: try clipboard paste
                android.content.ClipboardManager clipboard = 
                    (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("label", text);
                clipboard.setPrimaryClip(clip);
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
            node.recycle();
        } else {
            Log.w(TAG, "No editable text field focused for TYPE_TEXT");
        }
    }

    private void performSelectAll() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing SELECT_ALL action");
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Select all performed");
        } else {
            Log.w(TAG, "No editable text field focused for SELECT_ALL");
        }
    }

    private void performCopy() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing COPY action");
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Copy performed");
        } else {
            Log.w(TAG, "No editable text field focused for COPY");
        }
    }

    private void performCut() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing CUT action");
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Cut performed");
        } else {
            Log.w(TAG, "No editable text field focused for CUT");
        }
    }

    private void performPaste() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing PASTE action");
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Paste performed");
        } else {
            Log.w(TAG, "No editable text field focused for PASTE");
        }
    }

    private AccessibilityNodeInfo getCurrentInputField() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return null;
        AccessibilityNodeInfo focused = findFocusedEditableNode(root);
        root.recycle();
        return focused;
    }

    private AccessibilityNodeInfo findFocusedEditableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isFocused() && node.isEditable()) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findFocusedEditableNode(child);
            if (result != null) return result;
        }
        return null;
    }

    private AccessibilityNodeInfo focusFirstEditableField() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        AccessibilityNodeInfo editable = findFirstEditText(root);
        if (editable != null) {
            editable.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            return editable;
        }
        if (root != null) root.recycle();
        return null;
    }

    private AccessibilityNodeInfo findFirstEditText(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if ("android.widget.EditText".equals(node.getClassName())) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findFirstEditText(child);
            if (result != null) return result;
        }
        return null;
    }

    private void answerCall() {
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            try {
                telecomManager.acceptRingingCall();
                FeedbackProvider.speakAndToast(this, "Call answered");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException in acceptRingingCall: " + e.getMessage());
                FeedbackProvider.speakAndToast(this, "Unable to answer call: permission denied.");
            }
        } else {
            FeedbackProvider.speakAndToast(this, "Unable to answer call.");
        }
    }

    private void rejectCall() {
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    telecomManager.endCall();
                }
                FeedbackProvider.speakAndToast(this, "Call rejected");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException in endCall: " + e.getMessage());
                FeedbackProvider.speakAndToast(this, "Unable to reject call: permission denied.");
            }
        } else {
            FeedbackProvider.speakAndToast(this, "Unable to reject call.");
        }
    }

    private void performBackAction() {
        performGlobalAction(GLOBAL_ACTION_BACK);
    }

    private void performTapAtPosition(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription tap = 
            new GestureDescription.StrokeDescription(path, 0, 50);
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(tap);
        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Tap gesture completed at (" + x + ", " + y + ")");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Tap gesture cancelled at (" + x + ", " + y + ")");
            }
        }, null);
    }

    private void performPhotoCapture() {
        FeedbackProvider.speakAndToast(this, "Photo capture not implemented yet");
    }

    private void performVideoRecording() {
        FeedbackProvider.speakAndToast(this, "Video recording not implemented yet");
    }

    private void switchCamera() {
        FeedbackProvider.speakAndToast(this, "Camera switching not implemented yet");
    }

    private void toggleBluetooth() {
        BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
        if (bluetoothAdapter == null) {
            FeedbackProvider.speakAndToast(this, "Bluetooth not supported on this device");
            return;
        }
        if (bluetoothAdapter.isEnabled()) {
            // Permission check for Android 12+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    FeedbackProvider.speakAndToast(this, "Bluetooth permission denied");
                    return;
                }
            } else {
                if (checkSelfPermission(android.Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                    FeedbackProvider.speakAndToast(this, "Bluetooth admin permission denied");
                    return;
                }
            }
            bluetoothAdapter.disable();
            FeedbackProvider.speakAndToast(this, "Bluetooth turned off");
        } else {
            bluetoothAdapter.enable();
            FeedbackProvider.speakAndToast(this, "Bluetooth turned on");
        }
    }

    private void toggleMobileData() {
        // Cannot toggle directly; open data settings
        Intent panelIntent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
        panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(panelIntent);
        FeedbackProvider.speakAndToast(this, "Opening Mobile Data settings");
    }

    private void toggleWifi() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            // Open WiFi settings panel
            Intent panelIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(panelIntent);
            FeedbackProvider.speakAndToast(this, "Opening WiFi settings");
        } else {
            // For older versions, try to toggle directly (requires CHANGE_WIFI_STATE permission)
            FeedbackProvider.speakAndToast(this, "Direct WiFi toggle not implemented. Opening settings.");
            Intent panelIntent = new Intent(Settings.ACTION_WIFI_SETTINGS);
            panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(panelIntent);
        }
    }

    private void toggleHotspot(String state) {
        // Cannot toggle directly; open hotspot settings
        Intent panelIntent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
        panelIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(panelIntent);
        FeedbackProvider.speakAndToast(this, "Opening Hotspot settings");
    }

    private void performScrollLeft() {
        FeedbackProvider.speakAndToast(this, "Scroll left not implemented yet");
    }

    private void performScrollRight() {
        FeedbackProvider.speakAndToast(this, "Scroll right not implemented yet");
    }

    // New hands-free phone commands
    private void voiceDial(String contactName) {
        try {
            if (contactName != null && !contactName.isEmpty()) {
                // Search for contact in contacts database
                String[] projection = new String[] {
                    ContactsContract.Contacts._ID,
                    ContactsContract.Contacts.DISPLAY_NAME,
                    ContactsContract.Contacts.HAS_PHONE_NUMBER
                };
                
                String selection = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
                String[] selectionArgs = new String[] { "%" + contactName + "%" };
                
                Cursor cursor = getContentResolver().query(
                    ContactsContract.Contacts.CONTENT_URI,
                    projection,
                    selection,
                    selectionArgs,
                    ContactsContract.Contacts.DISPLAY_NAME + " ASC"
                );
                
                if (cursor != null && cursor.moveToFirst()) {
                    int contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                    int displayNameIndex = cursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME);
                    
                    if (contactIdIndex >= 0 && displayNameIndex >= 0) {
                        String contactId = cursor.getString(contactIdIndex);
                        String displayName = cursor.getString(displayNameIndex);
                    
                    // Get phone number for this contact
                    Cursor phoneCursor = getContentResolver().query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                        new String[] { contactId },
                        null
                    );
                    
                    if (phoneCursor != null && phoneCursor.moveToFirst()) {
                        int phoneNumberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                        if (phoneNumberIndex >= 0) {
                            String phoneNumber = phoneCursor.getString(phoneNumberIndex);
                        phoneCursor.close();
                        
                        // Dial the number
                        Intent intent = new Intent(Intent.ACTION_CALL);
                        intent.setData(Uri.parse("tel:" + phoneNumber));
                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        
                        FeedbackProvider.speakAndToast(this, "Calling " + displayName);
                        } else {
                            phoneCursor.close();
                            FeedbackProvider.speakAndToast(this, "No phone number found for " + displayName);
                        }
                    } else {
                        FeedbackProvider.speakAndToast(this, "No phone number found for " + displayName);
                    }
                    cursor.close();
                }
                } else {
                    // Contact not found, open dialer with search
                    Intent intent = new Intent(Intent.ACTION_DIAL);
                    intent.setData(Uri.parse("tel:" + contactName));
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    FeedbackProvider.speakAndToast(this, "Contact not found, opening dialer");
                }
            } else {
                // Open dialer
                Intent intent = new Intent(Intent.ACTION_DIAL);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                FeedbackProvider.speakAndToast(this, "Opening dialer");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in voice dial: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Permission denied for calling");
        } catch (Exception e) {
            Log.e(TAG, "Error in voice dial: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not open dialer");
        }
    }

    private void sendSMS(String contactName, String message) {
        try {
            if (contactName != null && message != null) {
                // First try to find contact and get phone number
                String phoneNumber = null;
                
                if (!contactName.matches("\\d+")) {
                    // Search for contact
                    String[] projection = new String[] {
                        ContactsContract.Contacts._ID,
                        ContactsContract.Contacts.DISPLAY_NAME
                    };
                    
                    String selection = ContactsContract.Contacts.DISPLAY_NAME + " LIKE ?";
                    String[] selectionArgs = new String[] { "%" + contactName + "%" };
                    
                    Cursor cursor = getContentResolver().query(
                        ContactsContract.Contacts.CONTENT_URI,
                        projection,
                        selection,
                        selectionArgs,
                        ContactsContract.Contacts.DISPLAY_NAME + " ASC"
                    );
                    
                    if (cursor != null && cursor.moveToFirst()) {
                        int contactIdIndex = cursor.getColumnIndex(ContactsContract.Contacts._ID);
                        if (contactIdIndex >= 0) {
                            String contactId = cursor.getString(contactIdIndex);
                        cursor.close();
                        
                        // Get phone number
                        Cursor phoneCursor = getContentResolver().query(
                            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                            new String[] { ContactsContract.CommonDataKinds.Phone.NUMBER },
                            ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                            new String[] { contactId },
                            null
                        );
                        
                        if (phoneCursor != null && phoneCursor.moveToFirst()) {
                                int phoneNumberIndex = phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER);
                                if (phoneNumberIndex >= 0) {
                                    phoneNumber = phoneCursor.getString(phoneNumberIndex);
                                }
                            phoneCursor.close();
                            }
                        }
                    }
                } else {
                    phoneNumber = contactName; // It's already a phone number
                }
                
                if (phoneNumber != null) {
                    // Send SMS directly
                    android.telephony.SmsManager smsManager = android.telephony.SmsManager.getDefault();
                    ArrayList<String> parts = smsManager.divideMessage(message);
                    for (String part : parts) {
                        smsManager.sendTextMessage(phoneNumber, null, part, null, null);
                    }
                    FeedbackProvider.speakAndToast(this, "SMS sent to " + contactName);
                } else {
                    // Open SMS app with pre-filled data
                    Intent intent = new Intent(Intent.ACTION_SENDTO);
                    intent.setData(Uri.parse("smsto:" + contactName));
                    intent.putExtra("sms_body", message);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(intent);
                    FeedbackProvider.speakAndToast(this, "Opening SMS for " + contactName);
                }
            } else {
                // Open SMS app
                Intent intent = new Intent(Intent.ACTION_MAIN);
                intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                FeedbackProvider.speakAndToast(this, "Opening messages");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in send SMS: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Permission denied for sending SMS");
        } catch (Exception e) {
            Log.e(TAG, "Error in send SMS: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not send SMS");
        }
    }

    private void readLastSMS() {
        try {
            // Query the last SMS
            String[] projection = new String[] {
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            };
            
            Cursor cursor = getContentResolver().query(
                Telephony.Sms.CONTENT_URI,
                projection,
                null,
                null,
                Telephony.Sms.DATE + " DESC LIMIT 1"
            );
            
            if (cursor != null && cursor.moveToFirst()) {
                String address = cursor.getString(cursor.getColumnIndex(Telephony.Sms.ADDRESS));
                String body = cursor.getString(cursor.getColumnIndex(Telephony.Sms.BODY));
                long date = cursor.getLong(cursor.getColumnIndex(Telephony.Sms.DATE));
                
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault());
                String dateStr = sdf.format(new Date(date));
                
                String message = "Last SMS from " + address + " on " + dateStr + ": " + body;
                FeedbackProvider.speakAndToast(this, message);
                cursor.close();
            } else {
                FeedbackProvider.speakAndToast(this, "No SMS messages found");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException reading SMS: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Permission denied to read SMS");
        } catch (Exception e) {
            Log.e(TAG, "Error reading SMS: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not read SMS");
        }
    }

    private void muteCall() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                boolean isMuted = audioManager.isMicrophoneMute();
                audioManager.setMicrophoneMute(!isMuted);
                FeedbackProvider.speakAndToast(this, isMuted ? "Call unmuted" : "Call muted");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error muting call: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not mute call");
        }
    }

    private void toggleSpeaker() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                boolean isSpeakerOn = audioManager.isSpeakerphoneOn();
                audioManager.setSpeakerphoneOn(!isSpeakerOn);
                FeedbackProvider.speakAndToast(this, isSpeakerOn ? "Speaker off" : "Speaker on");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling speaker: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not toggle speaker");
        }
    }

    private void endCall() {
        try {
            TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
            if (telecomManager != null) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    telecomManager.endCall();
                }
                FeedbackProvider.speakAndToast(this, "Call ended");
            }
        } catch (SecurityException e) {
            Log.e(TAG, "SecurityException in endCall: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Unable to end call: permission denied");
        } catch (Exception e) {
            Log.e(TAG, "Error ending call: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not end call");
        }
    }

    private void openContacts() {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            FeedbackProvider.speakAndToast(this, "Opening contacts");
        } catch (Exception e) {
            Log.e(TAG, "Error opening contacts: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not open contacts");
        }
    }

    private void openDialer() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            FeedbackProvider.speakAndToast(this, "Opening dialer");
        } catch (Exception e) {
            Log.e(TAG, "Error opening dialer: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not open dialer");
        }
    }

    private void openMessages() {
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_APP_MESSAGING);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            FeedbackProvider.speakAndToast(this, "Opening messages");
        } catch (Exception e) {
            Log.e(TAG, "Error opening messages: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not open messages");
        }
    }

    private void toggleSilentMode() {
        try {
            AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
            if (audioManager != null) {
                int currentMode = audioManager.getRingerMode();
                if (currentMode == AudioManager.RINGER_MODE_SILENT) {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_NORMAL);
                    FeedbackProvider.speakAndToast(this, "Silent mode off");
                } else {
                    audioManager.setRingerMode(AudioManager.RINGER_MODE_SILENT);
                    FeedbackProvider.speakAndToast(this, "Silent mode on");
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling silent mode: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not toggle silent mode");
        }
    }

    private void toggleAirplaneMode() {
        try {
            // For Android 4.2+, we need to use Settings.Global
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR1) {
                // Try to toggle directly if we have WRITE_SETTINGS permission
                int airplaneMode = Settings.Global.getInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, 0);
                Settings.Global.putInt(getContentResolver(), Settings.Global.AIRPLANE_MODE_ON, airplaneMode == 0 ? 1 : 0);
                
                // Broadcast the change
                Intent intent = new Intent(Intent.ACTION_AIRPLANE_MODE_CHANGED);
                intent.putExtra("state", airplaneMode == 0);
                sendBroadcast(intent);
                
                FeedbackProvider.speakAndToast(this, airplaneMode == 0 ? "Airplane mode on" : "Airplane mode off");
            } else {
                // Fallback to settings
                Intent intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                FeedbackProvider.speakAndToast(this, "Opening airplane mode settings");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling airplane mode: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not toggle airplane mode");
        }
    }

    private void toggleAutoRotation() {
        try {
            // Try to toggle directly if we have WRITE_SETTINGS permission
            int rotation = Settings.System.getInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
            Settings.System.putInt(getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, rotation == 0 ? 1 : 0);
            
            FeedbackProvider.speakAndToast(this, rotation == 0 ? "Auto rotation on" : "Auto rotation off");
        } catch (Exception e) {
            Log.e(TAG, "Error toggling rotation: " + e.getMessage());
            // Fallback to settings
            Intent intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            FeedbackProvider.speakAndToast(this, "Opening display settings");
        }
    }

    private void toggleFlashlight() {
        try {
            CameraManager cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cameraManager != null) {
                String[] cameraIds = cameraManager.getCameraIdList();
                for (String cameraId : cameraIds) {
                    android.hardware.camera2.CameraCharacteristics characteristics = 
                        cameraManager.getCameraCharacteristics(cameraId);
                    Boolean flashAvailable = characteristics.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    if (flashAvailable != null && flashAvailable) {
                        // Toggle flashlight using camera
                        cameraManager.setTorchMode(cameraId, true);
                        FeedbackProvider.speakAndToast(this, "Flashlight on");
                        return;
                    }
                }
                FeedbackProvider.speakAndToast(this, "Flashlight not available");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling flashlight: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not toggle flashlight");
        }
    }

    private void takeScreenshot() {
        try {
            // Use accessibility service to take screenshot
            // Note: AccessibilityService doesn't have getWindow() method
            // We'll use a different approach for screenshot
            FeedbackProvider.speakAndToast(this, "Screenshot functionality requires additional implementation");
        } catch (Exception e) {
            Log.e(TAG, "Error taking screenshot: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not take screenshot");
        }
    }

    private void openSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            FeedbackProvider.speakAndToast(this, "Opening settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening settings: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not open settings");
        }
    }

    private void openNotifications() {
        try {
            // Open notification panel
            Intent intent = new Intent("android.settings.NOTIFICATION_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            FeedbackProvider.speakAndToast(this, "Opening notification settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening notifications: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not open notifications");
        }
    }

    private void clearNotifications() {
        try {
            // Use accessibility service to clear notifications
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Look for clear all button or individual clear buttons
                @SuppressWarnings("unchecked")
                List<AccessibilityNodeInfo> clearButtons = rootNode.findAccessibilityNodeInfosByText("Clear all");
                if (clearButtons != null && !clearButtons.isEmpty()) {
                    for (AccessibilityNodeInfo button : clearButtons) {
                        if (button.isClickable()) {
                            button.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                            FeedbackProvider.speakAndToast(this, "Notifications cleared");
                            button.recycle();
                            break;
                        }
                        button.recycle();
                    }
                } else {
                    FeedbackProvider.speakAndToast(this, "No clear all button found");
                }
                rootNode.recycle();
            } else {
                FeedbackProvider.speakAndToast(this, "Cannot access notification panel");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error clearing notifications: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not clear notifications");
        }
    }

    private void toggleDoNotDisturb() {
        try {
            // Try to toggle DND directly
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                int currentFilter = notificationManager.getCurrentInterruptionFilter();
                int newFilter = (currentFilter == NotificationManager.INTERRUPTION_FILTER_NONE) ? 
                    NotificationManager.INTERRUPTION_FILTER_ALL : NotificationManager.INTERRUPTION_FILTER_NONE;
                
                notificationManager.setInterruptionFilter(newFilter);
                
                String status = (newFilter == NotificationManager.INTERRUPTION_FILTER_NONE) ? "Do not disturb on" : "Do not disturb off";
                FeedbackProvider.speakAndToast(this, status);
            } else {
                // Fallback to settings
                Intent intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                FeedbackProvider.speakAndToast(this, "Opening do not disturb settings");
            }
        } catch (Exception e) {
            Log.e(TAG, "Error toggling do not disturb: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not toggle do not disturb");
        }
    }

    private void openQuickSettings() {
        try {
            // Try to open quick settings panel
            Intent intent = new Intent("android.settings.QUICK_SETTINGS");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            FeedbackProvider.speakAndToast(this, "Opening quick settings");
        } catch (Exception e) {
            Log.e(TAG, "Error opening quick settings: " + e.getMessage());
            FeedbackProvider.speakAndToast(this, "Could not open quick settings");
        }
    }

    private void performAccessibilityToggle(String toggleType) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            FeedbackProvider.speakAndToast(this, "I can't access the screen right now.");
            return;
        }
        String[] keywords;
        switch (toggleType) {
            case "wifi":
                keywords = new String[]{"Wi-Fi", "WiFi", "WLAN"};
                break;
            case "bluetooth":
                keywords = new String[]{"Bluetooth"};
                break;
            case "data":
                keywords = new String[]{"Mobile data", "Cellular data", "Data"};
                break;
            case "hotspot":
                keywords = new String[]{"Hotspot", "Tethering"};
                break;
            case "gps":
                keywords = new String[]{"Location", "GPS"};
                break;
            case "nfc":
                keywords = new String[]{"NFC"};
                break;
            case "airplane":
                keywords = new String[]{"Airplane mode", "Flight mode"};
                break;
            case "dnd":
                keywords = new String[]{"Do Not Disturb", "DND"};
                break;
            case "flashlight":
                keywords = new String[]{"Flashlight", "Torch"};
                break;
            case "rotation":
                keywords = new String[]{"Auto-rotate", "Rotation", "Screen rotation"};
                break;
            default:
                FeedbackProvider.speakAndToast(this, "Unknown toggle: " + toggleType);
                return;
        }
        boolean toggled = false;
        for (String keyword : keywords) {
            @SuppressWarnings("unchecked")
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(keyword);
            if (nodes != null) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node.isClickable() && node.isEnabled()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        FeedbackProvider.speakAndToast(this, "Toggled " + keyword);
                        toggled = true;
                        node.recycle();
                        break;
                    }
                    node.recycle();
                }
            }
            if (toggled) break;
        }
        if (!toggled) {
            FeedbackProvider.speakAndToast(this, "Could not find toggle for " + toggleType);
        }
        rootNode.recycle();
    }

    private void smartToggle(String toggleType) {
        openSettingsScreenForToggle(toggleType);
        // Wait for the settings screen to load, then try to toggle
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            performAccessibilityToggle(toggleType);
        }, 1200); // Adjust delay as needed
    }

    private void openSettingsScreenForToggle(String toggleType) {
        Intent intent = null;
        switch (toggleType) {
            case "wifi":
                intent = new Intent(Settings.ACTION_WIFI_SETTINGS);
                break;
            case "bluetooth":
                intent = new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);
                break;
            case "data":
                intent = new Intent(Settings.ACTION_DATA_ROAMING_SETTINGS);
                break;
            case "hotspot":
                intent = new Intent(Settings.ACTION_WIRELESS_SETTINGS);
                break;
            case "gps":
                intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                break;
            case "nfc":
                intent = new Intent(Settings.ACTION_NFC_SETTINGS);
                break;
            case "airplane":
                intent = new Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS);
                break;
            case "dnd":
                intent = new Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS);
                break;
            case "flashlight":
                // No system intent for flashlight, must use quick settings or camera API
                break;
            case "rotation":
                intent = new Intent(Settings.ACTION_DISPLAY_SETTINGS);
                break;
        }
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            FeedbackProvider.speakAndToast(this, "Opening settings for " + toggleType);
        }
    }

    @Override
    public void onInterrupt() {
        Log.e(TAG, "Service interrupted.");
    }

    @Override
    protected boolean onKeyEvent(KeyEvent event) {
        if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_VOLUME_DOWN) {
            long now = System.currentTimeMillis();
            if (now - lastVolumeDownTime > VOLUME_PRESS_WINDOW_MS) {
                volumeDownCount = 1;
            } else {
                volumeDownCount++;
            }
            lastVolumeDownTime = now;
            if (volumeDownCount >= VOLUME_PRESS_EMERGENCY_COUNT) {
                volumeDownCount = 0;
                // Trigger emergency
                EmergencyActions.triggerEmergency(this);
                FeedbackProvider.speakAndToast(this, "Emergency triggered by volume down!");
                return true;
            }
        }
        return super.onKeyEvent(event);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        try {
            unregisterReceiver(mainReceiver);
            unregisterReceiver(screenOffReceiver);
        } catch (Exception e) {
            Log.e(TAG, "Error unregistering receiver", e);
        }
        // hideAppLockDialog(); // Method not implemented
    }
    
    // Quick Settings functionality
    private void readQuickSettings() {
        Log.d(TAG, "Reading quick settings from current screen");
        
        // Get the root node
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.w(TAG, "No root node available for reading quick settings");
            return;
        }
        
        // Look for quick settings tiles
        List<AccessibilityNodeInfo> quickSettingsNodes = new ArrayList<>();
        findQuickSettingsNodes(rootNode, quickSettingsNodes);
        
        if (quickSettingsNodes.isEmpty()) {
            Log.d(TAG, "No quick settings found on current screen");
            return;
        }
        
        // Extract and store quick settings labels
        for (AccessibilityNodeInfo node : quickSettingsNodes) {
            String label = getNodeText(node);
            if (label != null && !label.isEmpty()) {
                Log.d(TAG, "Found quick setting: " + label);
                // Store the quick setting for later use
                storeQuickSetting(label);
            }
        }
        
        Log.d(TAG, "Quick settings reading completed");
    }
    
    private void findQuickSettingsNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> results) {
        if (node == null) return;
        
        // Look for nodes that might be quick settings tiles
        // Common patterns: buttons, image buttons, or nodes with specific content descriptions
        if (isQuickSettingNode(node)) {
            results.add(node);
        }
        
        // Recursively search child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                findQuickSettingsNodes(child, results);
                child.recycle();
            }
        }
    }
    
    private boolean isQuickSettingNode(AccessibilityNodeInfo node) {
        if (node == null) return false;
        
        String text = getNodeText(node);
        String contentDesc = getNodeContentDescription(node);
        
        // Check if it's clickable (quick settings are usually clickable)
        if (!node.isClickable()) return false;
        
        // Look for common quick settings keywords
        String combined = (text + " " + contentDesc).toLowerCase();
        return combined.contains("wifi") || combined.contains("bluetooth") || 
               combined.contains("mobile data") || combined.contains("airplane") ||
               combined.contains("do not disturb") || combined.contains("flashlight") ||
               combined.contains("auto rotate") || combined.contains("battery") ||
               combined.contains("location") || combined.contains("hotspot") ||
               combined.contains("nfc") || combined.contains("cast") ||
               combined.contains("night light") || combined.contains("reading mode");
    }
    
    private String getNodeText(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence text = node.getText();
        return text != null ? text.toString() : null;
    }
    
    private String getNodeContentDescription(AccessibilityNodeInfo node) {
        if (node == null) return null;
        CharSequence desc = node.getContentDescription();
        return desc != null ? desc.toString() : null;
    }
    
    private void storeQuickSetting(String settingName) {
        // Store in SharedPreferences for the QuickSettingsHandler to access
        SharedPreferences prefs = getSharedPreferences("quick_settings_prefs", MODE_PRIVATE);
        Set<String> settings = prefs.getStringSet("quick_settings", new HashSet<String>());
        
        // Create a new set to avoid modification issues
        Set<String> newSettings = new HashSet<String>(settings);
        newSettings.add(settingName.toLowerCase(Locale.ROOT));
        
        prefs.edit().putStringSet("quick_settings", newSettings).apply();
        Log.d(TAG, "Stored quick setting: " + settingName);
    }
    
    private void toggleQuickSetting(String settingName, boolean turnOn) {
        Log.d(TAG, "Toggling quick setting: " + settingName + " to " + turnOn);
        
        // Get the root node
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.w(TAG, "No root node available for toggling quick setting");
            return;
        }
        
        // Find the quick setting node
        AccessibilityNodeInfo settingNode = findQuickSettingNode(rootNode, settingName);
        if (settingNode == null) {
            Log.w(TAG, "Quick setting not found: " + settingName);
            return;
        }
        
        // Click the setting to toggle it
        if (settingNode.isClickable()) {
            settingNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            Log.d(TAG, "Clicked quick setting: " + settingName);
        } else {
            // Try to find a clickable ancestor
            AccessibilityNodeInfo clickableAncestor = findClickableAncestor(settingNode);
            if (clickableAncestor != null) {
                clickableAncestor.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG, "Clicked quick setting ancestor: " + settingName);
            } else {
                Log.w(TAG, "No clickable element found for quick setting: " + settingName);
            }
        }
    }
    
    private AccessibilityNodeInfo findQuickSettingNode(AccessibilityNodeInfo node, String settingName) {
        if (node == null) return null;
        
        String text = getNodeText(node);
        String contentDesc = getNodeContentDescription(node);
        
        // Check if this node matches the setting name
        if (text != null && text.toLowerCase(Locale.ROOT).contains(settingName.toLowerCase(Locale.ROOT))) {
            return node;
        }
        if (contentDesc != null && contentDesc.toLowerCase(Locale.ROOT).contains(settingName.toLowerCase(Locale.ROOT))) {
            return node;
        }
        
        // Recursively search child nodes
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findQuickSettingNode(child, settingName);
                if (result != null) {
                    return result;
                }
                child.recycle();
            }
        }
        
        return null;
    }
} 