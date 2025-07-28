package com.mvp.sarah;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.telecom.TelecomManager;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import com.google.gson.Gson;
import java.util.List;
import java.util.Locale;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.Set;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.io.Serializable;

import com.mvp.sarah.handlers.ClickLabelHandler;
import com.mvp.sarah.handlers.OpenCameraHandler;
import com.mvp.sarah.handlers.TypeTextHandler;
import com.mvp.sarah.handlers.WhatsAppHandler;
import com.mvp.sarah.handlers.ReadScreenHandler;
import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Path;
import android.os.Bundle;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityNodeInfo;
import android.os.Handler;
import android.os.Looper;
import okhttp3.*;
import org.json.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import com.mvp.sarah.FeedbackProvider;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ComponentName;
import java.util.HashSet;
import java.util.Set;
import java.util.Arrays;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

public class ClickAccessibilityService extends AccessibilityService {

    private static final String TAG = "ClickAccessibilitySvc";

    // Camera node info class for caching
    private static class CameraNodeInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        public final String packageName;
        public final String nodeText;
        public final String nodeDescription;
        public final String nodeResourceId;
        public final String nodeClassName;
        public final int nodeX;
        public final int nodeY;
        public final int nodeWidth;
        public final int nodeHeight;
        public final long timestamp;

        public CameraNodeInfo(String packageName, String nodeText, String nodeDescription,
                              String nodeResourceId, String nodeClassName,
                              int nodeX, int nodeY, int nodeWidth, int nodeHeight) {
            this.packageName = packageName;
            this.nodeText = nodeText;
            this.nodeDescription = nodeDescription;
            this.nodeResourceId = nodeResourceId;
            this.nodeClassName = nodeClassName;
            this.nodeX = nodeX;
            this.nodeY = nodeY;
            this.nodeWidth = nodeWidth;
            this.nodeHeight = nodeHeight;
            this.timestamp = System.currentTimeMillis();
        }

        public boolean isExpired() {
            return System.currentTimeMillis() - timestamp > CAMERA_CACHE_TIMEOUT_MS;
        }

        @Override
        public String toString() {
            return "CameraNodeInfo{" +
                    "packageName='" + packageName + '\'' +
                    ", text='" + nodeText + '\'' +
                    ", desc='" + nodeDescription + '\'' +
                    ", resourceId='" + nodeResourceId + '\'' +
                    ", className='" + nodeClassName + '\'' +
                    ", bounds=(" + nodeX + "," + nodeY + "," + nodeWidth + "," + nodeHeight + ")" +
                    '}';
        }
    }

    private final BroadcastReceiver clickReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ClickLabelHandler.ACTION_CLICK_LABEL.equals(intent.getAction())) {
                String label = intent.getStringExtra(ClickLabelHandler.EXTRA_LABEL);
                Log.d(TAG, "Received broadcast with label: " + label);
                if (label != null && !label.isEmpty()) {
                    Log.d(TAG, "Received request for action: " + label);

                    if (label.equalsIgnoreCase("scroll")) {
                        performScroll();
                    } else if (label.equalsIgnoreCase("scroll_up")) {
                        performScrollUp();
                    } else if (label.equalsIgnoreCase("like")) {
                        Log.d(TAG, "About to perform double-tap for like");
                        performDoubleTapOnScreen();
                    } else if (label.equalsIgnoreCase("switch_camera")) {
                        Log.d(TAG, "About to switch camera");
                        switchCamera();
                    } else if (label.equalsIgnoreCase("next video")) {
                        Log.d(TAG, "About to swipe up for next video");
                        performSwipeUpToNextVideo();
                    } else if (label.equalsIgnoreCase("click on that video")) {
                        Log.d(TAG, "About to tap on current video");
                        performTapOnCurrentVideo();
                    }
                    else {
                        clickNodeWithText(label);
                    }
                } else {
                    Log.w(TAG, "Received broadcast but label was null or empty");
                }
            }
            else if ("com.mvp.sarah.ACTION_PERFORM_BACK".equals(intent.getAction())) {
                Log.d(TAG, "Received ACTION_PERFORM_BACK broadcast");
                performBackAction();
            } else if ("com.mvp.sarah.ACTION_ANSWER_CALL".equals(intent.getAction())) {
                Log.d(TAG, "About to answer call");
                answerCall();
            } else if ("com.mvp.sarah.ACTION_REJECT_CALL".equals(intent.getAction())) {
                Log.d(TAG, "About to reject call");
                rejectCall();
            } else if (TypeTextHandler.ACTION_TYPE_TEXT.equals(intent.getAction())) {
                String text = intent.getStringExtra(TypeTextHandler.EXTRA_TEXT);
                Log.d(TAG, "Received TYPE_TEXT action: " + text);
                performTypeText(text);
            } else if (TypeTextHandler.ACTION_NEXT_LINE.equals(intent.getAction())) {
                Log.d(TAG, "Received NEXT_LINE action");
                performTypeText("\n");
            } else if (TypeTextHandler.ACTION_SELECT_ALL.equals(intent.getAction())) {
                Log.d(TAG, "Received SELECT_ALL action");
                performSelectAll();
            } else if (TypeTextHandler.ACTION_COPY.equals(intent.getAction())) {
                Log.d(TAG, "Received COPY action");
                performCopy();
            } else if (TypeTextHandler.ACTION_CUT.equals(intent.getAction())) {
                Log.d(TAG, "Received CUT action");
                performCut();
            } else if (TypeTextHandler.ACTION_PASTE.equals(intent.getAction())) {
                Log.d(TAG, "Received PASTE action");
                performPaste();
            } else if ("com.mvp.sarah.ACTION_SEND_WHATSAPP".equals(intent.getAction())) {
                Log.d(TAG, "Received SEND_WHATSAPP action");
                String contactName = intent.getStringExtra("contact_name");
                String message = intent.getStringExtra("message");
                performWhatsAppSend(contactName, message);
            } else if (ClickLabelHandler.ACTION_TYPE_MUSIC_SEARCH.equals(intent.getAction())) {
                String query = intent.getStringExtra(ClickLabelHandler.EXTRA_MUSIC_SEARCH);
                if (query != null) {
                    Log.d(TAG, "Received music search query: " + query);
                    // Start the Amazon Music automation flow
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        performAmazonMusicSearchFlow(query);
                    }, 2500); // Wait for app to open

                }
            } else if ("com.mvp.sarah.ACTION_TOGGLE_WIFI_ADVANCED".equals(intent.getAction())) {
                Log.d(TAG, "Received advanced WiFi toggle request");
                toggleWifiAdvanced();
            } else if ("com.mvp.sarah.ACTION_TOGGLE_BLUETOOTH_ADVANCED".equals(intent.getAction())) {
                Log.d(TAG, "Received advanced Bluetooth toggle request");
                toggleBluetoothAdvanced();
            } else if ("com.mvp.sarah.ACTION_TOGGLE_MOBILE_DATA_ADVANCED".equals(intent.getAction())) {
                Log.d(TAG, "Received advanced Mobile Data toggle request");
                toggleMobileDataAdvanced();
            } else if ("com.mvp.sarah.ACTION_TOGGLE_HOTSPOT_ADVANCED".equals(intent.getAction())) {
                Log.d(TAG, "Received advanced Hotspot toggle request");
                toggleHotspotAdvanced();
            } else if ("com.mvp.sarah.ACTION_TAKE_SCREENSHOT".equals(intent.getAction())) {
                takeScreenshotQuickOrProjection();
            } else if ("com.mvp.sarah.ACTION_TRIGGER_QUICK_TILE".equals(intent.getAction())) {
                String tile = intent.getStringExtra("tile_keyword");
                if (tile != null && !tile.isEmpty()) {
                    triggerQuickSettingsTile(tile);
                }
            } else if ("com.mvp.sarah.ACTION_TRIGGER_QUICK_TILE_WITH_STATE".equals(intent.getAction())) {
                String tile = intent.getStringExtra("tile_keyword");
                boolean shouldTurnOn = intent.getBooleanExtra("should_turn_on", true);
                if (tile != null && !tile.isEmpty()) {
                    triggerQuickSettingsTileWithState(tile, shouldTurnOn);
                }
            } else if ("com.mvp.sarah.ACTION_DEBUG_QUICK_SETTINGS".equals(intent.getAction())) {
                debugQuickSettings();
            } else if ("com.mvp.sarah.ACTION_LIST_QUICK_SETTINGS".equals(intent.getAction())) {
                listQuickSettingsTiles();
            } else if ("com.mvp.sarah.ACTION_TAKE_PHOTO_AUTO".equals(intent.getAction())) {
                takePhotoAuto();
            } else if ("com.mvp.sarah.ACTION_TAKE_PHOTO_ONLY".equals(intent.getAction())) {
                takePhotoOnly();
            } else if ("com.mvp.sarah.ACTION_SWITCH_CAMERA".equals(intent.getAction())) {
                switchCamera();
            } else if ("com.mvp.sarah.ACTION_TAKE_PHOTO_WITH_TIMER".equals(intent.getAction())) {
                int seconds = intent.getIntExtra("seconds", 3);
                boolean isSelfie = intent.getBooleanExtra("is_selfie", false);
                takePhotoWithTimer(seconds, isSelfie);
            }
        }
    };

    private BroadcastReceiver readScreenReceiver;
    private BroadcastReceiver clickPointReceiver;

    // Enhanced app lock and unlock system
    private static final Map<String, Long> unlockedLockedApps = new ConcurrentHashMap<>();
    private static final Map<String, Integer> unlockAttempts = new ConcurrentHashMap<>();
    private static final long UNLOCK_TIMEOUT_MS = 15 * 60 * 1000; // 15 minutes (increased)
    private static final long UNLOCK_EXTENDED_TIMEOUT_MS = 60 * 60 * 1000; // 1 hour for trusted apps
    private static final int MAX_UNLOCK_ATTEMPTS = 5;
    private static final long ATTEMPT_RESET_TIMEOUT_MS = 30 * 60 * 1000; // 30 minutes
    private String lastPackageName = null;
    private long lastAppSwitchTime = 0;

    // Camera node caching for reliable camera switching
    private static final Map<String, CameraNodeInfo> cachedCameraNodes = new ConcurrentHashMap<>();
    private static final long CAMERA_CACHE_TIMEOUT_MS = 10 * 60 * 1000; // 10 minutes

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        Log.d(TAG, "ClickAccessibilityService connected");

        // Configure the accessibility service
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED |
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED |
                AccessibilityEvent.TYPE_VIEW_CLICKED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.flags = AccessibilityServiceInfo.DEFAULT |
                AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS |
                AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
        
        // Auto-discover quick settings tiles after a delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            discoverQuickSettingsTiles();
        }, 3000); // Wait 3 seconds for service to be fully connected
        IntentFilter filter = new IntentFilter();
        filter.addAction("com.mvp.sarah.ACTION_CLICK_POINT");
        filter.addAction("com.mvp.sarah.ACTION_TYPE_TEXT");
        filter.addAction("com.mvp.sarah.ACTION_SUBMIT");
        filter.addAction("com.mvp.sarah.ACTION_CLICK_ANSWER");
        filter.addAction("com.mvp.sarah.ACTION_AUTOMATE_ONLINE_TEST");
        filter.addAction("com.mvp.sarah.TEST_TIMER_SET");

        filter.addAction(ClickLabelHandler.ACTION_CLICK_LABEL);
        filter.addAction("com.mvp.sarah.ACTION_ANSWER_CALL");
        filter.addAction("com.mvp.sarah.ACTION_REJECT_CALL");
        filter.addAction(TypeTextHandler.ACTION_TYPE_TEXT);
        filter.addAction(TypeTextHandler.ACTION_NEXT_LINE);
        filter.addAction(TypeTextHandler.ACTION_SELECT_ALL);
        filter.addAction(TypeTextHandler.ACTION_COPY);
        filter.addAction(TypeTextHandler.ACTION_CUT);
        filter.addAction(TypeTextHandler.ACTION_PASTE);
        filter.addAction("com.mvp.sarah.ACTION_SEND_WHATSAPP");
        filter.addAction(ClickLabelHandler.ACTION_TYPE_MUSIC_SEARCH);
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_WIFI_ADVANCED");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_BLUETOOTH_ADVANCED");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_MOBILE_DATA_ADVANCED");
        filter.addAction("com.mvp.sarah.ACTION_TOGGLE_HOTSPOT_ADVANCED");
        filter.addAction("com.mvp.sarah.ACTION_TAKE_SCREENSHOT");
        filter.addAction("com.mvp.sarah.ACTION_TRIGGER_QUICK_TILE");
        filter.addAction("com.mvp.sarah.ACTION_TRIGGER_QUICK_TILE_WITH_STATE");
        filter.addAction("com.mvp.sarah.ACTION_DEBUG_QUICK_SETTINGS");
        filter.addAction("com.mvp.sarah.ACTION_LIST_QUICK_SETTINGS");
        filter.addAction("com.mvp.sarah.ACTION_TAKE_PHOTO_AUTO");
        filter.addAction("com.mvp.sarah.ACTION_TAKE_PHOTO_ONLY");
        filter.addAction("com.mvp.sarah.ACTION_SWITCH_CAMERA");
        filter.addAction("com.mvp.sarah.ACTION_TAKE_PHOTO_WITH_TIMER");
        registerReceiver(clickReceiver, filter, RECEIVER_EXPORTED);
        registerReceiver(actionReceiver, filter, RECEIVER_EXPORTED);
        setServiceInfo(info);

        readScreenReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (ReadScreenHandler.ACTION_READ_SCREEN.equals(intent.getAction())) {
                    Log.d(TAG, "Read screen action received.");
                    readScreen();
                }
            }
        };
        IntentFilter readScreenFilter = new IntentFilter(ReadScreenHandler.ACTION_READ_SCREEN);
        registerReceiver(readScreenReceiver, readScreenFilter, RECEIVER_EXPORTED);

        // Register for custom click point
        clickPointReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.mvp.sarah.ACTION_CLICK_POINT".equals(intent.getAction())) {
                    int x = intent.getIntExtra("x", -1);
                    int y = intent.getIntExtra("y", -1);
                    if (x != -1 && y != -1) {
                        performTapAtPosition(x, y);
                    }
                }
            }
        };
        IntentFilter clickPointFilter = new IntentFilter("com.mvp.sarah.ACTION_CLICK_POINT");
        registerReceiver(clickPointReceiver, clickPointFilter, RECEIVER_EXPORTED);
    }

    private void clickNodeWithText(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot perform click.");
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }

        // 1. Try exact text match (visible & clickable)
        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        boolean clicked = false;
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                String nodeText = node.getText() != null ? node.getText().toString() : "";
                if (node != null && node.isVisibleToUser() && node.isClickable() && text.equals(nodeText)) {
                    Log.d(TAG, "Clicking exact match node: " + nodeText);
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    node.recycle();
                    clicked = true;
                    break;
                }
                if (node != null) node.recycle();
            }
        }

        // 2. Fallback to partial/content description/class name as before, only if not clicked
        if (!clicked) {
            Log.d(TAG, "No exact visible clickable match found for '" + text + "'. Trying partial and fallback matches.");
            // Partial text match
            clicked = findAndClickPartialText(rootNode, text.toLowerCase(Locale.ROOT));
            // Content description match
            if (!clicked) {
                clicked = findAndClickByContentDescription(rootNode, text.toLowerCase(Locale.ROOT));
            }
            // Class name heuristics
            if (!clicked) {
                clicked = findAndClickByClassName(rootNode, "Button");
            }
        }

        // 3. Log node tree for debugging if still not clicked
        if (!clicked) {
            Log.d(TAG, "No match found. Dumping node tree for debugging:");
            dumpNodeTree(rootNode, 0);
            FeedbackProvider.speakAndToast(this, "Couldn't click " + text);
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
                Log.d(TAG, "Clicking ancestor node for partial text: " + text);
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
                Log.d(TAG, "Clicking ancestor node for content description: " + text);
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

    private boolean findAndClickByClassName(AccessibilityNodeInfo node, String className) {
        if (node == null) return false;
        boolean clicked = false;
        if (node.getClassName() != null && node.getClassName().toString().contains(className)) {
            AccessibilityNodeInfo clickableNode = findClickableAncestor(node);
            if (clickableNode != null) {
                Log.d(TAG, "Clicking ancestor node for class name: " + className);
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
                clicked = findAndClickByClassName(child, className) || clicked;
                child.recycle();
            }
        }
        return clicked;
    }

    private void dumpNodeTree(AccessibilityNodeInfo node, int depth) {
        if (node == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append("[").append(node.getClassName());
        if (node.getText() != null) sb.append(" text=\"").append(node.getText()).append("\"");
        if (node.getContentDescription() != null) sb.append(" desc=\"").append(node.getContentDescription()).append("\"");
        sb.append(" clickable=").append(node.isClickable());
        sb.append(" visible=").append(node.isVisibleToUser());
        sb.append("]");
        Log.d(TAG, sb.toString());
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                dumpNodeTree(child, depth + 1);
                child.recycle();
            }
        }
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

    private void performDoubleTapOnScreen() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int x = width / 2;
        int y = height / 2;

        Path tapPath = new Path();
        tapPath.moveTo(x, y);

        GestureDescription.StrokeDescription firstTap = new GestureDescription.StrokeDescription(tapPath, 0, 50);
        GestureDescription.StrokeDescription secondTap = new GestureDescription.StrokeDescription(tapPath, 150, 50);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(firstTap);
        builder.addStroke(secondTap);

        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Double-tap gesture completed.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Double tap performed");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Double-tap gesture cancelled.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Couldn't perform double tap");
            }
        }, null);
    }

    private void performScroll() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot perform scroll.");
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }
        AccessibilityNodeInfo scrollable = findScrollableNode(rootNode);
        if (scrollable != null) {
            Log.d(TAG, "Found scrollable node. Performing scroll forward.");
            boolean result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_FORWARD);
            scrollable.recycle();
            // Remove feedback for scroll actions
            // if (result) {
            //     FeedbackProvider.speakAndToast(this, "Scrolled down");
            // } else {
            //     FeedbackProvider.speakAndToast(this, "Can't scroll down anymore");
            // }
        } else {
            FeedbackProvider.speakAndToast(this, "No scrollable area found");
        }
        rootNode.recycle();
    }

    private void performScrollUp() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot perform scroll up.");
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }
        AccessibilityNodeInfo scrollable = findScrollableNode(rootNode);
        if (scrollable != null) {
            Log.d(TAG, "Found scrollable node. Performing scroll backward.");
            boolean result = scrollable.performAction(AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD);
            scrollable.recycle();
            // Remove feedback for scroll actions
            // if (result) {
            //     FeedbackProvider.speakAndToast(this, "Scrolled up");
            // } else {
            //     FeedbackProvider.speakAndToast(this, "Can't scroll up anymore");
            // }
        } else {
            FeedbackProvider.speakAndToast(this, "No scrollable area found");
        }
        rootNode.recycle();
    }

    private AccessibilityNodeInfo findScrollableNode(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isScrollable()) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findScrollableNode(child);
            if (child != null) child.recycle();
            if (result != null) return result;
        }
        return null;
    }

    private void switchCamera() {
        Log.d(TAG, "switchCamera called - looking for 'switch' label");
        // FeedbackProvider.speakAndToast(this, "Looking for camera switch button"); // Removed as requested

        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            Log.d(TAG, "Root node found, searching for 'switch' label");
            boolean switched = findAndClickCameraSwitchButton(rootNode);
            if (switched) {
                Log.d(TAG, "Found and clicked camera switch button with 'switch' label");
                rootNode.recycle();
                FeedbackProvider.speakAndToast(this, "Camera Changed");
                return;
            } else {
                Log.d(TAG, "No 'switch' label found in camera interface");
                // FeedbackProvider.speakAndToast(this, "Could not find camera switch button"); // Removed as requested
            }
            rootNode.recycle();
        } else {
            Log.d(TAG, "Root node is null, cannot search for camera switch button");
            FeedbackProvider.speakAndToast(this, "Cannot access camera interface");
        }
    }



    private boolean findAndClickCameraSwitchButton(AccessibilityNodeInfo node) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String resourceId = node.getViewIdResourceName();

        if (node.isClickable() && node.isVisibleToUser()) {
            // Specifically look for "switch" label
            if ((text != null && text.toString().toLowerCase().equals("switch")) ||
                    (desc != null && desc.toString().toLowerCase().equals("switch")) ||
                    (resourceId != null && resourceId.toLowerCase().contains("switch"))) {
                Log.d(TAG, "Found camera switch button with 'switch' label: text='" + text + "', desc='" + desc + "', resourceId='" + resourceId + "'");
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }

        // Recursively search children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            boolean found = findAndClickCameraSwitchButton(child);
            if (found) return true;
        }
        return false;
    }

    private void performTapAtPosition(int x, int y) {
        Log.d(TAG, "performTapAtPosition called at (" + x + ", " + y + ")");

        Path clickPath = new Path();
        clickPath.moveTo(x, y);

        GestureDescription.StrokeDescription clickStroke =
                new GestureDescription.StrokeDescription(clickPath, 0, 50);

        GestureDescription gestureDescription =
                new GestureDescription.Builder()
                        .addStroke(clickStroke)
                        .build();

        boolean result = dispatchGesture(gestureDescription, new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Camera switch tap completed successfully");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.e(TAG, "Camera switch tap was cancelled");
            }
        }, null);

        Log.d(TAG, "dispatchGesture result: " + result);
    }

    private void answerCall() {
        Log.d(TAG, "answerCall called");
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            try {
                telecomManager.acceptRingingCall();
                Log.d(TAG, "Called acceptRingingCall()");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException in acceptRingingCall: " + e.getMessage());
                FeedbackProvider.speakAndToast(this, "Unable to answer call: permission denied.");
            }
        } else {
            Log.e(TAG, "TelecomManager is null");
            FeedbackProvider.speakAndToast(this, "Unable to answer call.");
        }
    }

    private void rejectCall() {
        Log.d(TAG, "rejectCall called");
        TelecomManager telecomManager = (TelecomManager) getSystemService(Context.TELECOM_SERVICE);
        if (telecomManager != null) {
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    telecomManager.endCall();
                }
                Log.d(TAG, "Called endCall()");
            } catch (SecurityException e) {
                Log.e(TAG, "SecurityException in endCall: " + e.getMessage());
                FeedbackProvider.speakAndToast(this, "Unable to reject call: permission denied.");
            }
        } else {
            Log.e(TAG, "TelecomManager is null");
            FeedbackProvider.speakAndToast(this, "Unable to reject call.");
        }
    }

    private void performTypeText(String text) {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node == null) {
            // Try to focus the first editable field
            node = focusFirstEditableField();
        }
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing TYPE_TEXT: " + text);
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            boolean setTextResult = node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            if (!setTextResult) {
                // Fallback: try clipboard paste
                Log.d(TAG, "ACTION_SET_TEXT failed, trying clipboard paste fallback.");
                android.content.ClipboardManager clipboard = (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                android.content.ClipData clip = android.content.ClipData.newPlainText("label", text);
                clipboard.setPrimaryClip(clip);
                node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
                node.performAction(AccessibilityNodeInfo.ACTION_PASTE);
            }
            node.recycle();
        }
        else {
            Log.w(TAG, "No editable text field focused for TYPE_TEXT");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
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

    private void performSelectAll() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing SELECT_ALL action");
            // Focus the node first, then try to select all
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            // For select all, we'll use a gesture to select all text
            // This is a simplified approach - in practice, you might need to use IME actions
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Select all performed");
        } else {
            Log.w(TAG, "No editable text field focused for SELECT_ALL");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
    }

    private void performCopy() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing COPY action");
            // Focus the node and perform copy action
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Copy performed");
        } else {
            Log.w(TAG, "No editable text field focused for COPY");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
    }

    private void performCut() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing CUT action");
            // Focus the node and perform cut action
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Cut performed");
        } else {
            Log.w(TAG, "No editable text field focused for CUT");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
    }

    private void performPaste() {
        AccessibilityNodeInfo node = getCurrentInputField();
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing PASTE action");
            // Focus the node and perform paste action
            node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            node.recycle();
            FeedbackProvider.speakAndToast(this, "Paste performed");
        } else {
            Log.w(TAG, "No editable text field focused for PASTE");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
    }

    private void performWhatsAppSend(String contactName, String message) {
        Log.d(TAG, "Performing WhatsApp send for: " + contactName);

        // Wait a bit for WhatsApp to open and load
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Look for the send button in WhatsApp
            AccessibilityNodeInfo rootNode = getRootInActiveWindow();
            if (rootNode != null) {
                // Try to find the send button by various methods
                AccessibilityNodeInfo sendButton = findWhatsAppSendButton(rootNode);
                if (sendButton != null) {
                    Log.d(TAG, "Found WhatsApp send button, clicking it");
                    sendButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    sendButton.recycle();

                    // Wait a bit more and then close WhatsApp
                    new Handler(Looper.getMainLooper()).postDelayed(() -> {
                        performBackAction();
                        FeedbackProvider.speakAndToast(this, "WhatsApp message sent to " + contactName);
                    }, 1000);
                } else {
                    Log.w(TAG, "Could not find WhatsApp send button, trying fallback method");
                    // Fallback: click 1cm to the right of the documents button
                    performFallbackWhatsAppSend(contactName);
                }
                rootNode.recycle();
            }
        }, 2000); // Wait 2 seconds for WhatsApp to load
    }

    private void performFallbackWhatsAppSend(String contactName) {
        Log.d(TAG, "Using fallback method to click send button");

        // Get screen dimensions
        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // Calculate position: send button is typically at the bottom-right area
        // Documents button is usually at the bottom, so we click 1cm to the right
        int clickX = screenWidth - 100; // 100px from right edge (approximately 1cm)
        int clickY = screenHeight - 150; // 150px from bottom edge

        Log.d(TAG, "Clicking at position: (" + clickX + ", " + clickY + ")");

        // Perform tap gesture at the calculated position
        performTapGesture(clickX, clickY);

        // Wait a bit more and then close WhatsApp
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            performBackAction();
            FeedbackProvider.speakAndToast(this, "WhatsApp message sent to " + contactName);
        }, 1000);
    }

    private AccessibilityNodeInfo findWhatsAppSendButton(AccessibilityNodeInfo node) {
        if (node == null) return null;

        // Try to find by text content (more specific WhatsApp send button texts)
        CharSequence text = node.getText();
        if (text != null) {
            String lowerText = text.toString().toLowerCase();
            if (lowerText.equals("send") || lowerText.equals("enviar") ||
                    lowerText.equals("→") || lowerText.equals("▶") ||
                    lowerText.equals("send") || lowerText.equals("send") ||
                    lowerText.contains("send") || lowerText.contains("enviar")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found send button by text: " + text);
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }

        // Try to find by resource ID (more specific WhatsApp send button IDs)
        String resourceId = node.getViewIdResourceName();
        if (resourceId != null) {
            String lowerResourceId = resourceId.toLowerCase();
            if (lowerResourceId.contains("send") ||
                    lowerResourceId.contains("send_button") ||
                    lowerResourceId.contains("sendbutton") ||
                    lowerResourceId.contains("fab_send") ||
                    lowerResourceId.contains("send_fab") ||
                    lowerResourceId.contains("com.whatsapp:id/send")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found send button by resource ID: " + resourceId);
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }

        // Try to find by content description
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null) {
            String lowerContentDesc = contentDesc.toString().toLowerCase();
            if (lowerContentDesc.contains("send") ||
                    lowerContentDesc.contains("enviar") ||
                    lowerContentDesc.contains("send message")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found send button by content description: " + contentDesc);
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }

        // Recursively search children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findWhatsAppSendButton(child);
                if (result != null) {
                    child.recycle();
                    return result;
                }
                child.recycle();
            }
        }

        return null;
    }

    private void performBackAction() {
        // Perform back action to close WhatsApp
        performGlobalAction(GLOBAL_ACTION_BACK);
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

    private void performAmazonMusicSearchFlow(String query) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot start Amazon Music search flow.");
            return;
        }
        // 1. Click the 'Find' button
        AccessibilityNodeInfo findButton = findNodeByDescOrText(rootNode, "find");
        if (findButton != null && findButton.isClickable()) {
            findButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            findButton.recycle();
            Log.d(TAG, "Clicked 'Find' button.");
            // 2. Wait, then click the search bar
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                clickAmazonMusicSearchBarAndType(query);
            }, 1200);
        } else {
            Log.e(TAG, "Could not find 'Find' button in Amazon Music.");
        }
        rootNode.recycle();
    }

    private void clickAmazonMusicSearchBarAndType(String query) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        AccessibilityNodeInfo searchBar = findNodeByDescOrText(rootNode, "What do you want to hear?");
        if (searchBar != null && searchBar.isClickable()) {
            searchBar.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            searchBar.recycle();
            Log.d(TAG, "Clicked search bar.");
            // Wait, then type the query
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                typeInAmazonMusicSearch(query);
            }, 800);
        } else {
            Log.e(TAG, "Could not find search bar in Amazon Music.");
        }
        rootNode.recycle();
    }

    private void typeInAmazonMusicSearch(String query) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        AccessibilityNodeInfo editText = findFirstEditText(rootNode);
        if (editText != null) {
            editText.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, query);
            editText.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            editText.recycle();
            Log.d(TAG, "Typed song name: " + query);
            // Wait, then press 'Done' (tick) on keyboard
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                pressKeyboardDoneButton();
            }, 800);
        } else {
            Log.e(TAG, "Could not find EditText to type in Amazon Music.");
        }
        rootNode.recycle();
    }

    private void pressKeyboardDoneButton() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        AccessibilityNodeInfo doneButton = findNodeByDescOrText(rootNode, "done");
        if (doneButton == null) {
            // Try tick unicode (✓)
            doneButton = findNodeByDescOrText(rootNode, "✓");
        }
        if (doneButton != null && doneButton.isClickable()) {
            doneButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            doneButton.recycle();
            Log.d(TAG, "Pressed 'Done' button on keyboard.");
            // Wait, then click the first song result
            new Handler(Looper.getMainLooper()).postDelayed(this::clickFirstAmazonMusicResult, 1200);
        } else {
            Log.e(TAG, "Could not find 'Done' button on keyboard.");
        }
        rootNode.recycle();
    }

    private void clickFirstAmazonMusicResult() {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;
        // Try to find the first clickable node in the results list
        AccessibilityNodeInfo firstSong = findFirstClickableSongResult(rootNode);
        if (firstSong != null) {
            firstSong.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            firstSong.recycle();
            Log.d(TAG, "Clicked first song result in Amazon Music.");
        } else {
            Log.e(TAG, "Could not find first song result in Amazon Music.");
        }
        rootNode.recycle();
    }

    private AccessibilityNodeInfo findNodeByDescOrText(AccessibilityNodeInfo node, String keyword) {
        if (node == null) return null;
        String lowerKeyword = keyword.toLowerCase();
        if ((node.getContentDescription() != null && node.getContentDescription().toString().toLowerCase().contains(lowerKeyword)) ||
                (node.getText() != null && node.getText().toString().toLowerCase().contains(lowerKeyword))) {
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findNodeByDescOrText(child, keyword);
            if (result != null) return result;
        }
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

    private AccessibilityNodeInfo findFirstClickableSongResult(AccessibilityNodeInfo node) {
        if (node == null) return null;
        if (node.isClickable() && node.getClassName() != null && node.getClassName().toString().toLowerCase().contains("view")) {
            // Heuristic: clickable view in results
            return AccessibilityNodeInfo.obtain(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findFirstClickableSongResult(child);
            if (result != null) return result;
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

    private void traverseNode(AccessibilityNodeInfo node, StringBuilder sb) {
        if (node == null) {
            return;
        }

        if (node.getText() != null && !node.getText().toString().isEmpty()) {
            sb.append(node.getText().toString()).append(". ");
        }

        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            traverseNode(child, sb);
        }
    }

    @Override
    public boolean onUnbind(Intent intent) {
        if (clickPointReceiver != null) unregisterReceiver(clickPointReceiver);
        if (readScreenReceiver != null) unregisterReceiver(readScreenReceiver);
        return super.onUnbind(intent);
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = String.valueOf(event.getPackageName());
            SharedPreferences prefs = getSharedPreferences("AppLockPrefs", MODE_PRIVATE);
            Set<String> lockedApps = new HashSet<>(prefs.getStringSet("locked_apps", new HashSet<>()));

            // Enhanced app switching logic
            long currentTime = System.currentTimeMillis();
            if (lastPackageName != null && !lastPackageName.equals(packageName)) {
                // App switch detected - handle security transitions
                handleAppSwitch(lastPackageName, packageName, currentTime);
            }
            lastPackageName = packageName;
            lastAppSwitchTime = currentTime;

            // Check if this is a camera app and cache camera switch button
            
            // Enhanced app lock logic
            if (lockedApps.contains(packageName)) {
                handleLockedAppAccess(packageName, currentTime);
            }
        }
    }

    /**
     * Handle app switching with enhanced security
     */
    private void handleAppSwitch(String previousPackage, String newPackage, long currentTime) {
        // Clear unlock state for previous app if it's been too long
        Long previousUnlockTime = unlockedLockedApps.get(previousPackage);
        if (previousUnlockTime != null) {
            long timeSinceUnlock = currentTime - previousUnlockTime;
            long timeout = isTrustedApp(previousPackage) ? UNLOCK_EXTENDED_TIMEOUT_MS : UNLOCK_TIMEOUT_MS;

            if (timeSinceUnlock > timeout) {
                unlockedLockedApps.remove(previousPackage);
                Log.d(TAG, "App unlock expired on switch: " + previousPackage);
            }
        }

        // Log app switch for security tracking
        Log.d(TAG, "App switch: " + previousPackage + " -> " + newPackage);
    }

    /**
     * Enhanced locked app access handling
     */
    private void handleLockedAppAccess(String packageName, long currentTime) {
        // Check if app is currently unlocked
        if (isAppUnlocked(packageName)) {
            Log.d(TAG, "App is unlocked: " + packageName);
            return;
        }

        // Check if app is locked due to too many failed attempts
        if (isAppLockedDueToAttempts(packageName)) {
            Log.d(TAG, "App locked due to failed attempts: " + packageName);
            showLockedAppMessage(packageName, "Too many failed unlock attempts. Try again later.");
            return;
        }

        // Check if AppLockActivity is already active
        if (AppLockActivity.isActive()) {
            Log.d(TAG, "AppLockActivity already active, skipping lock screen");
            return;
        }

        // Launch lock screen
        Log.d(TAG, "Launching lock screen for: " + packageName);
        Intent lockIntent = new Intent(this, AppLockActivity.class);
        lockIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        lockIntent.putExtra("locked_package", packageName);

        // Add additional security context
        lockIntent.putExtra("unlock_attempts", unlockAttempts.get(packageName));
        lockIntent.putExtra("remaining_time", getRemainingUnlockTime(packageName));
        lockIntent.putExtra("is_trusted_app", isTrustedApp(packageName));

        startActivity(lockIntent);
    }

    /**
     * Show message for locked app (alternative to lock screen)
     */
    private void showLockedAppMessage(String packageName, String message) {
        // You can implement a custom dialog or notification here
        Log.d(TAG, "Locked app message: " + packageName + " - " + message);
        FeedbackProvider.speakAndToast(this, message);
    }

    @Override
    public void onInterrupt() {
        Log.e("AccessService", "Service interrupted.");
    }


    // Intercept touch events to learn the switch button location
    @Override
    public boolean onGesture(int gestureId) {
        // Not used, but required to override
        return super.onGesture(gestureId);
    }

    public boolean onTouchEvent(android.view.MotionEvent event) {
        if (OpenCameraHandler.isLearningSwitchButton() && event.getAction() == android.view.MotionEvent.ACTION_DOWN) {
            int x = (int) event.getRawX();
            int y = (int) event.getRawY();
            OpenCameraHandler.setSwitchButtonCoordinates(x, y);
            FeedbackProvider.speakAndToast(this, "Switch button location saved.");
            return true;
        }
        return false;
    }

    private void performSwipeUpToNextVideo() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int x = width / 2;
        int startY = (int) (height * 0.75);
        int endY = (int) (height * 0.25);

        Path swipePath = new Path();
        swipePath.moveTo(x, startY);
        swipePath.lineTo(x, endY);

        GestureDescription.StrokeDescription swipe = new GestureDescription.StrokeDescription(swipePath, 0, 300);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(swipe);

        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Swipe up gesture completed.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Next video");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Swipe up gesture cancelled.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Couldn't go to next video");
            }
        }, null);
    }

    private void performTapOnCurrentVideo() {
        int width = getResources().getDisplayMetrics().widthPixels;
        int height = getResources().getDisplayMetrics().heightPixels;
        int x = width / 2;
        int y = height / 2;

        Path tapPath = new Path();
        tapPath.moveTo(x, y);

        GestureDescription.StrokeDescription tap = new GestureDescription.StrokeDescription(tapPath, 0, 50);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(tap);

        dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Tap on video completed.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Clicked on video");
            }
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Tap on video cancelled.");
                FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Couldn't click on video");
            }
        }, null);
    }

    private void performTapGesture(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription tap = new GestureDescription.StrokeDescription(path, 0, 50);
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

    // Advanced Brand-Aware Toggle System
    private static final String[] BRANDS = {"samsung", "xiaomi", "huawei", "oneplus", "oppo", "vivo", "realme", "motorola", "lg", "sony", "google", "pixel"};
    private static final String[] LOCALES = {"en", "es", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko", "ar", "hi", "th", "vi"};

    // Comprehensive keyword mappings for different toggles
    private static final String[][] WIFI_KEYWORDS = {
            {"wifi", "wi-fi", "wireless", "internet", "network"},
            {"wifi", "wi-fi", "inalámbrico", "internet", "red"},
            {"wifi", "wi-fi", "sans fil", "internet", "réseau"},
            {"wifi", "wi-fi", "drahtlos", "internet", "netzwerk"},
            {"wifi", "wi-fi", "senza fili", "internet", "rete"},
            {"wifi", "wi-fi", "sem fio", "internet", "rede"},
            {"wifi", "wi-fi", "беспроводной", "интернет", "сеть"},
            {"wifi", "wi-fi", "无线", "网络", "互联网"},
            {"wifi", "wi-fi", "ワイファイ", "無線", "インターネット"},
            {"wifi", "wi-fi", "와이파이", "무선", "인터넷"},
            {"wifi", "wi-fi", "واي فاي", "لاسلكي", "إنترنت"},
            {"wifi", "wi-fi", "वाईफाई", "वायरलेस", "इंटरनेट"},
            {"wifi", "wi-fi", "ไวไฟ", "ไร้สาย", "อินเทอร์เน็ต"},
            {"wifi", "wi-fi", "không dây", "internet", "mạng"}
    };

    private static final String[][] BLUETOOTH_KEYWORDS = {
            {"bluetooth", "bluetooth"},
            {"bluetooth", "bluetooth"},
            {"bluetooth", "bluetooth"},
            {"bluetooth", "bluetooth"},
            {"bluetooth", "bluetooth"},
            {"bluetooth", "bluetooth"},
            {"bluetooth", "блютуз"},
            {"bluetooth", "蓝牙"},
            {"bluetooth", "ブルートゥース"},
            {"bluetooth", "블루투스"},
            {"bluetooth", "بلوتوث"},
            {"bluetooth", "ब्लूटूथ"},
            {"bluetooth", "บลูทูธ"},
            {"bluetooth", "bluetooth"}
    };

    private static final String[][] MOBILE_DATA_KEYWORDS = {
            {"mobile data", "cellular data", "data", "mobile network"},
            {"datos móviles", "datos celulares", "datos", "red móvil"},
            {"données mobiles", "données cellulaires", "données", "réseau mobile"},
            {"mobildaten", "zellulardaten", "daten", "mobilfunk"},
            {"dati mobili", "dati cellulari", "dati", "rete mobile"},
            {"dados móveis", "dados celulares", "dados", "rede móvel"},
            {"мобильные данные", "сотовые данные", "данные", "мобильная сеть"},
            {"移动数据", "蜂窝数据", "数据", "移动网络"},
            {"モバイルデータ", "セルラーデータ", "データ", "モバイルネットワーク"},
            {"모바일 데이터", "셀룰러 데이터", "데이터", "모바일 네트워크"},
            {"بيانات الجوال", "البيانات الخلوية", "البيانات", "شبكة الجوال"},
            {"मोबाइल डेटा", "सेलुलर डेटा", "डेटा", "मोबाइल नेटवर्क"},
            {"ข้อมูลมือถือ", "ข้อมูลเซลลูลาร์", "ข้อมูล", "เครือข่ายมือถือ"},
            {"dữ liệu di động", "dữ liệu di động", "dữ liệu", "mạng di động"}
    };

    private static final String[][] HOTSPOT_KEYWORDS = {
            {"hotspot", "tethering", "mobile hotspot", "wifi hotspot"},
            {"punto de acceso", "anclaje", "punto de acceso móvil", "wifi punto de acceso"},
            {"point d'accès", "partage de connexion", "point d'accès mobile", "wifi point d'accès"},
            {"hotspot", "tethering", "mobiler hotspot", "wifi hotspot"},
            {"hotspot", "tethering", "hotspot mobile", "wifi hotspot"},
            {"hotspot", "tethering", "hotspot móvel", "wifi hotspot"},
            {"точка доступа", "модем", "мобильная точка доступа", "wifi точка доступа"},
            {"热点", "网络共享", "移动热点", "wifi热点"},
            {"ホットスポット", "テザリング", "モバイルホットスポット", "wifiホットスポット"},
            {"핫스팟", "테더링", "모바일 핫스팟", "wifi 핫스팟"},
            {"نقطة اتصال", "ربط", "نقطة اتصال محمولة", "wifi نقطة اتصال"},
            {"हॉटस्पॉट", "टेथरिंग", "मोबाइल हॉटस्पॉट", "wifi हॉटस्पॉट"},
            {"ฮอตสปอต", "เทเธอริ่ง", "โมบายล์ฮอตสปอต", "wifi ฮอตสปอต"},
            {"điểm phát sóng", "chia sẻ kết nối", "điểm phát sóng di động", "wifi điểm phát sóng"}
    };

    // Brand-specific resource ID patterns
    private static final String[][] WIFI_RESOURCE_PATTERNS = {
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"},
            {"wifi", "wifi_switch", "wifi_toggle", "wifi_button", "wifi_icon"}
    };

    // Advanced toggle methods with brand awareness
    public void toggleWifiAdvanced() {
        Log.d(TAG, "Starting advanced WiFi toggle");
        String deviceBrand = getDeviceBrand();
        String deviceLocale = getDeviceLocale();

        // Strategy 1: Try direct toggle via settings
        if (tryDirectToggle("wifi", deviceBrand, deviceLocale)) {
            return;
        }

        // Strategy 2: Navigate to WiFi settings and toggle
        if (tryNavigateAndToggle("wifi", deviceBrand, deviceLocale)) {
            return;
        }

        // Strategy 3: Use quick settings panel
        if (tryQuickSettingsToggle("wifi", deviceBrand, deviceLocale)) {
            return;
        }

        // Strategy 4: Fallback to settings app
        trySettingsAppToggle("wifi", deviceBrand, deviceLocale);
    }

    public void toggleBluetoothAdvanced() {
        Log.d(TAG, "Starting advanced Bluetooth toggle");
        String deviceBrand = getDeviceBrand();
        String deviceLocale = getDeviceLocale();

        if (tryDirectToggle("bluetooth", deviceBrand, deviceLocale)) {
            return;
        }

        if (tryNavigateAndToggle("bluetooth", deviceBrand, deviceLocale)) {
            return;
        }

        if (tryQuickSettingsToggle("bluetooth", deviceBrand, deviceLocale)) {
            return;
        }

        trySettingsAppToggle("bluetooth", deviceBrand, deviceLocale);
    }

    public void toggleMobileDataAdvanced() {
        Log.d(TAG, "Starting advanced Mobile Data toggle");
        String deviceBrand = getDeviceBrand();
        String deviceLocale = getDeviceLocale();

        if (tryDirectToggle("mobile_data", deviceBrand, deviceLocale)) {
            return;
        }

        if (tryNavigateAndToggle("mobile_data", deviceBrand, deviceLocale)) {
            return;
        }

        if (tryQuickSettingsToggle("mobile_data", deviceBrand, deviceLocale)) {
            return;
        }

        trySettingsAppToggle("mobile_data", deviceBrand, deviceLocale);
    }

    public void toggleHotspotAdvanced() {
        Log.d(TAG, "Starting advanced Hotspot toggle");
        String deviceBrand = getDeviceBrand();
        String deviceLocale = getDeviceLocale();

        if (tryDirectToggle("hotspot", deviceBrand, deviceLocale)) {
            return;
        }

        if (tryNavigateAndToggle("hotspot", deviceBrand, deviceLocale)) {
            return;
        }

        if (tryQuickSettingsToggle("hotspot", deviceBrand, deviceLocale)) {
            return;
        }

        trySettingsAppToggle("hotspot", deviceBrand, deviceLocale);
    }

    private boolean tryDirectToggle(String toggleType, String brand, String locale) {
        Log.d(TAG, "Trying direct toggle for " + toggleType);

        // Try to find toggle in current screen
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return false;

        boolean result = findAndClickToggleRecursive(rootNode, toggleType, brand, locale, 0);
        rootNode.recycle();

        if (result) {
            FeedbackProvider.speakAndToast(this, "Toggled " + toggleType);
            return true;
        }

        return false;
    }

    private boolean tryNavigateAndToggle(String toggleType, String brand, String locale) {
        Log.d(TAG, "Trying navigate and toggle for " + toggleType);

        // Open settings for the specific toggle
        String settingsIntent = getSettingsIntent(toggleType, brand);
        if (settingsIntent != null) {
            try {
                Intent intent = new Intent(settingsIntent);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);

                // Wait for settings to load, then find and click toggle
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    findAndClickToggleInSettings(toggleType, brand, locale);
                }, 2000);

                return true;
            } catch (Exception e) {
                Log.e(TAG, "Failed to open settings for " + toggleType, e);
            }
        }

        return false;
    }

    private boolean tryQuickSettingsToggle(String toggleType, String brand, String locale) {
        Log.d(TAG, "Trying quick settings toggle for " + toggleType);

        // Try to open quick settings panel
        try {
            performGlobalAction(GLOBAL_ACTION_NOTIFICATIONS);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                findAndClickToggleInQuickSettings(toggleType, brand, locale);
            }, 1000);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Failed to open quick settings", e);
            return false;
        }
    }

    private void trySettingsAppToggle(String toggleType, String brand, String locale) {
        Log.d(TAG, "Trying settings app toggle for " + toggleType);

        // Open main settings app
        try {
            Intent intent = new Intent(android.provider.Settings.ACTION_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                navigateToToggleInSettings(toggleType, brand, locale);
            }, 2000);

        } catch (Exception e) {
            Log.e(TAG, "Failed to open settings app", e);
            FeedbackProvider.speakAndToast(this, "Unable to toggle " + toggleType);
        }
    }

    private boolean findAndClickToggleRecursive(AccessibilityNodeInfo node, String toggleType, String brand, String locale, int depth) {
        if (node == null || depth > 10) return false; // Prevent infinite recursion

        // Try multiple identification methods
        if (isToggleNode(node, toggleType, brand, locale)) {
            Log.d(TAG, "Found toggle node for " + toggleType + " at depth " + depth);
            return performToggleClick(node);
        }

        // Recursively search children
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean result = findAndClickToggleRecursive(child, toggleType, brand, locale, depth + 1);
                child.recycle();
                if (result) return true;
            }
        }

        return false;
    }

    private boolean isToggleNode(AccessibilityNodeInfo node, String toggleType, String brand, String locale) {
        if (node == null || !node.isClickable()) return false;

        // Get keywords for this toggle type and locale
        String[] keywords = getKeywordsForToggle(toggleType, locale);
        String[] resourcePatterns = getResourcePatternsForToggle(toggleType, brand);

        // Check text content
        CharSequence text = node.getText();
        if (text != null) {
            String lowerText = text.toString().toLowerCase();
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    Log.d(TAG, "Found toggle by text: " + text);
                    return true;
                }
            }
        }

        // Check content description
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null) {
            String lowerDesc = contentDesc.toString().toLowerCase();
            for (String keyword : keywords) {
                if (lowerDesc.contains(keyword.toLowerCase())) {
                    Log.d(TAG, "Found toggle by content description: " + contentDesc);
                    return true;
                }
            }
        }

        // Check resource ID
        String resourceId = node.getViewIdResourceName();
        if (resourceId != null) {
            String lowerResourceId = resourceId.toLowerCase();
            for (String pattern : resourcePatterns) {
                if (lowerResourceId.contains(pattern.toLowerCase())) {
                    Log.d(TAG, "Found toggle by resource ID: " + resourceId);
                    return true;
                }
            }
        }

        // Check class name for toggle indicators
        CharSequence className = node.getClassName();
        if (className != null) {
            String lowerClassName = className.toString().toLowerCase();
            if (lowerClassName.contains("switch") || lowerClassName.contains("toggle") ||
                    lowerClassName.contains("checkbox") || lowerClassName.contains("button")) {
                // Additional check: is it near toggle-related text?
                if (hasToggleRelatedTextNearby(node, toggleType, locale)) {
                    Log.d(TAG, "Found toggle by class name: " + className);
                    return true;
                }
            }
        }

        return false;
    }

    private boolean hasToggleRelatedTextNearby(AccessibilityNodeInfo node, String toggleType, String locale) {
        // Check if this node is near text that matches the toggle type
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null) {
            return findToggleTextInNode(parent, toggleType, locale);
        }
        return false;
    }

    private boolean findToggleTextInNode(AccessibilityNodeInfo node, String toggleType, String locale) {
        if (node == null) return false;

        String[] keywords = getKeywordsForToggle(toggleType, locale);

        // Check text
        CharSequence text = node.getText();
        if (text != null) {
            String lowerText = text.toString().toLowerCase();
            for (String keyword : keywords) {
                if (lowerText.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        // Check content description
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null) {
            String lowerDesc = contentDesc.toString().toLowerCase();
            for (String keyword : keywords) {
                if (lowerDesc.contains(keyword.toLowerCase())) {
                    return true;
                }
            }
        }

        // Recursively check children
        for (int i = 0; i < node.getChildCount(); i++) {
            @SuppressWarnings("unchecked")
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                boolean result = findToggleTextInNode(child, toggleType, locale);
                child.recycle();
                if (result) return true;
            }
        }

        return false;
    }

    private boolean performToggleClick(AccessibilityNodeInfo node) {
        if (node == null) return false;

        try {
            // Focus first, then click
            boolean focusResult = node.performAction(AccessibilityNodeInfo.ACTION_FOCUS);
            Log.d(TAG, "Focus result: " + focusResult);

            // Small delay for focus
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                boolean clickResult = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                Log.d(TAG, "Click result: " + clickResult);
            }, 100);

            return true;
        } catch (Exception e) {
            Log.e(TAG, "Error performing toggle click", e);
            return false;
        }
    }

    private String[] getKeywordsForToggle(String toggleType, String locale) {
        int localeIndex = getLocaleIndex(locale);

        switch (toggleType) {
            case "wifi":
                return WIFI_KEYWORDS[localeIndex];
            case "bluetooth":
                return BLUETOOTH_KEYWORDS[localeIndex];
            case "mobile_data":
                return MOBILE_DATA_KEYWORDS[localeIndex];
            case "hotspot":
                return HOTSPOT_KEYWORDS[localeIndex];
            default:
                return new String[]{toggleType};
        }
    }

    private String[] getResourcePatternsForToggle(String toggleType, String brand) {
        int brandIndex = getBrandIndex(brand);

        switch (toggleType) {
            case "wifi":
                return WIFI_RESOURCE_PATTERNS[brandIndex];
            case "bluetooth":
                return new String[]{"bluetooth", "bluetooth_switch", "bluetooth_toggle", "bluetooth_button"};
            case "mobile_data":
                return new String[]{"mobile_data", "data_switch", "data_toggle", "data_button"};
            case "hotspot":
                return new String[]{"hotspot", "tethering", "hotspot_switch", "hotspot_toggle"};
            default:
                return new String[]{toggleType};
        }
    }

    private String getSettingsIntent(String toggleType, String brand) {
        switch (toggleType) {
            case "wifi":
                return android.provider.Settings.ACTION_WIFI_SETTINGS;
            case "bluetooth":
                return android.provider.Settings.ACTION_BLUETOOTH_SETTINGS;
            case "mobile_data":
                return android.provider.Settings.ACTION_DATA_ROAMING_SETTINGS;
            case "hotspot":
                return android.provider.Settings.ACTION_WIRELESS_SETTINGS;
            default:
                return null;
        }
    }

    private void findAndClickToggleInSettings(String toggleType, String brand, String locale) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        boolean found = findAndClickToggleRecursive(rootNode, toggleType, brand, locale, 0);
        rootNode.recycle();

        if (found) {
            FeedbackProvider.speakAndToast(this, "Toggled " + toggleType + " in settings");
        } else {
            FeedbackProvider.speakAndToast(this, "Could not find " + toggleType + " toggle");
        }
    }

    private void findAndClickToggleInQuickSettings(String toggleType, String brand, String locale) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        boolean found = findAndClickToggleRecursive(rootNode, toggleType, brand, locale, 0);
        rootNode.recycle();

        if (found) {
            FeedbackProvider.speakAndToast(this, "Toggled " + toggleType );
        } else {
            FeedbackProvider.speakAndToast(this, "Could not find " + toggleType + " toggle");
        }
    }

    private void navigateToToggleInSettings(String toggleType, String brand, String locale) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) return;

        // Try to find and click on the settings category for this toggle
        String[] keywords = getKeywordsForToggle(toggleType, locale);
        boolean found = false;

        for (String keyword : keywords) {
            @SuppressWarnings("unchecked")
            List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(keyword);
            if (nodes != null && !nodes.isEmpty()) {
                for (AccessibilityNodeInfo node : nodes) {
                    if (node != null && node.isClickable()) {
                        node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        node.recycle();
                        found = true;

                        // Wait for navigation, then find toggle
                        new Handler(Looper.getMainLooper()).postDelayed(() -> {
                            findAndClickToggleInSettings(toggleType, brand, locale);
                        }, 1500);
                        break;
                    }
                    if (node != null) node.recycle();
                }
                if (found) break;
            }
        }
    }

    // Restore static methods for app lock/unlock management
    public static void markAppUnlocked(String packageName) {
        long currentTime = System.currentTimeMillis();
        unlockedLockedApps.put(packageName, currentTime);
        unlockAttempts.remove(packageName); // Reset attempts on successful unlock
        Log.d(TAG, "App unlocked: " + packageName + " at " + currentTime);
    }

    public static void markAppUnlockedExtended(String packageName) {
        long currentTime = System.currentTimeMillis();
        unlockedLockedApps.put(packageName, currentTime);
        unlockAttempts.remove(packageName);
        Log.d(TAG, "App unlocked with extended timeout: " + packageName);
    }

    public static boolean isAppUnlocked(String packageName) {
        Long unlockTime = unlockedLockedApps.get(packageName);
        if (unlockTime == null) return false;
        long currentTime = System.currentTimeMillis();
        long timeout = isTrustedApp(packageName) ? UNLOCK_EXTENDED_TIMEOUT_MS : UNLOCK_TIMEOUT_MS;
        boolean isUnlocked = (currentTime - unlockTime) < timeout;
        if (!isUnlocked) {
            unlockedLockedApps.remove(packageName);
            Log.d(TAG, "App unlock expired: " + packageName);
        }
        return isUnlocked;
    }

    public static void recordUnlockAttempt(String packageName) {
        Integer attempts = unlockAttempts.get(packageName);
        int currentAttempts = attempts != null ? attempts : 0;
        unlockAttempts.put(packageName, currentAttempts + 1);
        Log.d(TAG, "Unlock attempt recorded for " + packageName + ": " + (currentAttempts + 1));
    }

    public static boolean isAppLockedDueToAttempts(String packageName) {
        Integer attempts = unlockAttempts.get(packageName);
        if (attempts == null) return false;
        long currentTime = System.currentTimeMillis();
        // Reset attempts after timeout
        if (currentTime - attempts > ATTEMPT_RESET_TIMEOUT_MS) {
            unlockAttempts.remove(packageName);
            return false;
        }
        return attempts >= MAX_UNLOCK_ATTEMPTS;
    }

    public static boolean isTrustedApp(String packageName) {
        return packageName != null && (
                packageName.contains("com.android.settings") ||
                        packageName.contains("com.google.android.apps.maps") ||
                        packageName.contains("com.whatsapp") ||
                        packageName.contains("com.facebook") ||
                        packageName.contains("com.instagram") ||
                        packageName.contains("com.twitter") ||
                        packageName.contains("com.snapchat") ||
                        packageName.contains("com.spotify") ||
                        packageName.contains("com.netflix") ||
                        packageName.contains("com.amazon") ||
                        packageName.contains("com.google.android.gm") ||
                        packageName.contains("com.microsoft.teams") ||
                        packageName.contains("com.skype") ||
                        packageName.contains("com.zoom")
        );
    }

    public static long getRemainingUnlockTime(String packageName) {
        Long unlockTime = unlockedLockedApps.get(packageName);
        if (unlockTime == null) return 0;
        long currentTime = System.currentTimeMillis();
        long timeout = isTrustedApp(packageName) ? UNLOCK_EXTENDED_TIMEOUT_MS : UNLOCK_TIMEOUT_MS;
        long remaining = timeout - (currentTime - unlockTime);
        return Math.max(0, remaining);
    }

    public static void clearAppUnlockState(String packageName) {
        unlockedLockedApps.remove(packageName);
        unlockAttempts.remove(packageName);
        Log.d(TAG, "Unlock state cleared for: " + packageName);
    }

    public static void clearAllUnlockStates() {
        unlockedLockedApps.clear();
        unlockAttempts.clear();
        Log.d(TAG, "All unlock states cleared");
    }

    // Restore missing methods for quick settings, camera, and device info
    public void takeScreenshotQuickOrProjection() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            performGlobalAction(GLOBAL_ACTION_TAKE_SCREENSHOT);
        } else {
            // Fallback: try to find screenshot tile in quick settings
            performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                AccessibilityNodeInfo root = getRootInActiveWindow();
                if (root != null) {
                    boolean clicked = false;
                    String[] keywords = {"Screenshot", "Screen capture", "Capture d'écran", "スクリーンショット", "Скриншот", "Captura de pantalla", "Schermata", "Bildschirmfoto", "截屏", "Captura", "PrtSc", "PrtScn"};
                    for (String keyword : keywords) {
                        clicked = findAndClickQuickSettingToggle(root, keyword);
                        if (clicked) break;
                    }
                    root.recycle();
                    if (clicked) {
                        FeedbackProvider.speakAndToast(this, "Screenshot taken.");
                    } else {
                        FeedbackProvider.speakAndToast(this, "Could not find screenshot tile.");
                    }
                }
                if (android.os.Build.VERSION.SDK_INT >= 28) {
                    performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
                }
            }, 300);
        }
    }

    // --- Quick Settings Tile Trigger ---
    private void triggerQuickSettingsTile(final String tileKeyword) {
        Log.d(TAG, "Triggering quick settings tile: " + tileKeyword);
        // Open quick settings panel first
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
        
        // Use a more dynamic approach with retries
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            attemptQuickSettingsClick(tileKeyword, 0);
        }, 500); // Increased initial delay
    }

    private void attemptQuickSettingsClick(final String tileKeyword, int attempt) {
        if (attempt >= 5) {
            FeedbackProvider.speakAndToast(this, "Could not find " + tileKeyword + " in quick settings after multiple attempts");
            dismissQuickSettings();
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            boolean clicked = clickQuickSettingsTileByLabel(root, tileKeyword);
            root.recycle();
            if (clicked) {
                dismissQuickSettings();
                return;
            }
        }

        // Retry after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            attemptQuickSettingsClick(tileKeyword, attempt + 1);
        }, 300);
    }

    private void dismissQuickSettings() {
        if (android.os.Build.VERSION.SDK_INT >= 28) {
            performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
        }
    }

    public void triggerQuickSettingsTileWithState(final String tileKeyword, final boolean shouldTurnOn) {
        Log.d(TAG, "Triggering quick settings tile with state: " + tileKeyword + " (shouldTurnOn: " + shouldTurnOn + ")");
        // Open quick settings panel first
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
        
        // Use a more dynamic approach with retries
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            attemptQuickSettingsClickWithState(tileKeyword, shouldTurnOn, 0);
        }, 500); // Increased initial delay
    }

    private void attemptQuickSettingsClickWithState(final String tileKeyword, final boolean shouldTurnOn, int attempt) {
        if (attempt >= 5) {
            FeedbackProvider.speakAndToast(this, "Could not find " + tileKeyword + " in quick settings after multiple attempts");
            dismissQuickSettings();
            return;
        }

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            boolean clicked = clickQuickSettingsTileByLabelAndState(root, tileKeyword, shouldTurnOn);
            root.recycle();
            if (clicked) {
                dismissQuickSettings();
                return;
            }
        }

        // Retry after a short delay
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            attemptQuickSettingsClickWithState(tileKeyword, shouldTurnOn, attempt + 1);
        }, 300);
    }

    private boolean clickQuickSettingsTileByLabel(AccessibilityNodeInfo node, String label) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        
        if (node.isClickable() && node.isVisibleToUser()) {
            String textStr = text != null ? text.toString().toLowerCase() : "";
            String descStr = desc != null ? desc.toString().toLowerCase() : "";
            String labelLower = label.toLowerCase();
            
            // More flexible matching
            if (textStr.contains(labelLower) || descStr.contains(labelLower) ||
                textStr.equals(labelLower) || descStr.equals(labelLower)) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                FeedbackProvider.speakAndToast(this, label + " toggled");
                return true;
            }
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (clickQuickSettingsTileByLabel(child, label)) {
                return true;
            }
        }
        return false;
    }

    private boolean clickQuickSettingsTileByLabelAndState(AccessibilityNodeInfo node, String label, boolean shouldTurnOn) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        boolean isToggle = false;
        boolean isOn = false;
        if (node.isCheckable()) {
            isToggle = true;
            isOn = node.isChecked();
        }
        
        if (node.isClickable() && node.isVisibleToUser()) {
            String textStr = text != null ? text.toString().toLowerCase() : "";
            String descStr = desc != null ? desc.toString().toLowerCase() : "";
            String labelLower = label.toLowerCase();
            
            // More flexible matching
            if (textStr.contains(labelLower) || descStr.contains(labelLower) ||
                textStr.equals(labelLower) || descStr.equals(labelLower)) {
                if (isToggle && isOn == shouldTurnOn) {
                    FeedbackProvider.speakAndToast(this, label + " is already " + (isOn ? "on" : "off"));
                    return true;
                }
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                FeedbackProvider.speakAndToast(this, label + (shouldTurnOn ? " enabled" : " disabled"));
                return true;
            }
        }
        
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (clickQuickSettingsTileByLabelAndState(child, label, shouldTurnOn)) {
                return true;
            }
        }
        return false;
    }

    // --- Accessibility Tree Debugging ---
    private void dumpAccessibilityTree(AccessibilityNodeInfo node, int depth) {
        if (node == null) return;
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append("[").append(node.getClassName());
        if (node.getText() != null) sb.append(" text=\"").append(node.getText()).append("\"");
        if (node.getContentDescription() != null) sb.append(" desc=\"").append(node.getContentDescription()).append("\"");
        sb.append(" clickable=").append(node.isClickable());
        sb.append(" visible=").append(node.isVisibleToUser());
        sb.append("]");
        Log.d(TAG, sb.toString());
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            dumpAccessibilityTree(child, depth + 1);
        }
    }

    // --- Camera Switch Button Caching (stub) ---
    private void cacheCameraSwitchButton() {
        // For a real implementation, you would store the node info for later use
        FeedbackProvider.speakAndToast(this, "Caching camera switch button (stub)");
    }

    private boolean findAndClickQuickSettingToggle(AccessibilityNodeInfo node, String keyword) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String resourceId = node.getViewIdResourceName();
        if (node.isClickable() && node.isVisibleToUser()) {
            if ((text != null && text.toString().toLowerCase().contains(keyword.toLowerCase())) ||
                    (desc != null && desc.toString().toLowerCase().contains(keyword.toLowerCase())) ||
                    (resourceId != null && resourceId.toLowerCase().contains(keyword.toLowerCase()))) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            boolean found = findAndClickQuickSettingToggle(child, keyword);
            if (found) return true;
        }
        return false;
    }

    private boolean findAndClickQuickSettingToggleWithState(AccessibilityNodeInfo node, String keyword, boolean shouldTurnOn) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String resourceId = node.getViewIdResourceName();
        if (node.isClickable() && node.isVisibleToUser()) {
            if ((text != null && text.toString().toLowerCase().contains(keyword.toLowerCase())) ||
                    (desc != null && desc.toString().toLowerCase().contains(keyword.toLowerCase())) ||
                    (resourceId != null && resourceId.toLowerCase().contains(keyword.toLowerCase()))) {
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            boolean found = findAndClickQuickSettingToggleWithState(child, keyword, shouldTurnOn);
            if (found) return true;
        }
        return false;
    }

    private String getDeviceBrand() {
        String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
        return manufacturer;
    }

    private boolean isMotorolaDevice() {
        String manufacturer = android.os.Build.MANUFACTURER.toLowerCase();
        String brand = android.os.Build.BRAND.toLowerCase();
        String model = android.os.Build.MODEL.toLowerCase();

        return manufacturer.contains("motorola") ||
                brand.contains("motorola") ||
                model.contains("moto") ||
                model.contains("motorola");
    }

    private String getDeviceLocale() {
        return java.util.Locale.getDefault().getLanguage();
    }

    private int getBrandIndex(String brand) {
        String[] BRANDS = {"samsung", "xiaomi", "huawei", "oneplus", "oppo", "vivo", "realme", "motorola", "lg", "sony", "google", "pixel"};
        for (int i = 0; i < BRANDS.length; i++) {
            if (BRANDS[i].equals(brand)) {
                return i;
            }
        }
        return 0;
    }

    private int getLocaleIndex(String locale) {
        String[] LOCALES = {"en", "es", "fr", "de", "it", "pt", "ru", "zh", "ja", "ko", "ar", "hi", "th", "vi"};
        for (int i = 0; i < LOCALES.length; i++) {
            if (LOCALES[i].equals(locale)) {
                return i;
            }
        }
        return 0;
    }

    private boolean isCameraApp(String packageName) {
        return packageName != null && (
                packageName.contains("camera") ||
                        packageName.contains("photo") ||
                        packageName.contains("gallery") ||
                        packageName.equals("com.google.android.GoogleCamera") ||
                        packageName.equals("com.android.camera") ||
                        packageName.equals("com.motorola.camera3") ||
                        packageName.equals("com.sec.android.camera") ||
                        packageName.equals("com.oneplus.camera") ||
                        packageName.equals("com.oppo.camera") ||
                        packageName.equals("com.vivo.camera") ||
                        packageName.equals("com.xiaomi.camera") ||
                        packageName.equals("com.huawei.camera")
        );
    }


    private final BroadcastReceiver actionReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action == null) return;

            switch (action) {
                case "com.mvp.sarah.ACTION_CLICK_POINT":
                    int x = intent.getIntExtra("x", -1);
                    int y = intent.getIntExtra("y", -1);
                    if (x != -1 && y != -1) {
                        performTapAtPosition(x, y);
                    }
                    break;
                case "com.mvp.sarah.ACTION_TYPE_TEXT":
                    String text = intent.getStringExtra("text");
                    if (text != null) {
                        performTypeText(text);
                    }
                    break;
                case "com.mvp.sarah.ACTION_SUBMIT":
                    performSubmit();
                    break;
                case "com.mvp.sarah.ACTION_CLICK_ANSWER":
                    String answer = intent.getStringExtra("answer");
                    if (answer != null) {
                        clickAnswerByText(answer);
                    }
                    break;
                case "com.mvp.sarah.ACTION_AUTOMATE_ONLINE_TEST":
                    automateTestFlow();
                    break;
                case "com.mvp.sarah.TEST_TIMER_SET":
                    int minutes = intent.getIntExtra("minutes", 0);
                    if (minutes > 0) {
                        testDurationMinutes = minutes;
                        scheduleSubmitBeforeEnd();
                        automateTestFlow(); // Start automation after timer is set
                    }
                    break;
            }
        }
    };

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(actionReceiver);
        unregisterReceiver(clickReceiver);
        submitHandler.removeCallbacks(submitRunnable);
    }

    private void performSubmit() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo submitNode = findNodeByTextOrDesc(root, "Submit");
        if (submitNode != null && submitNode.isClickable() && submitNode.isVisibleToUser()) {
            submitNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            submitNode.recycle();
        }
        root.recycle();
    }

    private AccessibilityNodeInfo findNodeByTextOrDesc(AccessibilityNodeInfo node, String target) {
        if (node == null) return null;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if ((text != null && text.toString().equalsIgnoreCase(target)) ||
                (desc != null && desc.toString().equalsIgnoreCase(target))) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findNodeByTextOrDesc(child, target);
            if (result != null) return result;
        }
        return null;
    }

    // 1. Extract all visible texts (question + options)
    private List<String> extractAllVisibleTexts() {
        List<String> texts = new ArrayList<>();
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            extractTextsRecursive(root, texts);
            root.recycle();
        }
        return texts;
    }

    private void extractTextsRecursive(AccessibilityNodeInfo node, List<String> texts) {
        if (node == null) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (node.isVisibleToUser()) {
            if (text != null && text.length() > 0) {
                texts.add(text.toString());
            } else if (desc != null && desc.length() > 0) {
                texts.add(desc.toString());
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            extractTextsRecursive(child, texts);
            if (child != null) child.recycle();
        }
    }

    // 2. Build prompt for Gemini
    private String buildGeminiPrompt(List<String> texts) {
        StringBuilder question = new StringBuilder();
        List<String> options = new ArrayList<>();
        for (String t : texts) {
            if (t.matches("^[A-D]\\).*")) { // Option lines like "A) 6048"
                options.add(t);
            } else {
                question.append(t).append(" ");
            }
        }
        StringBuilder prompt = new StringBuilder();
        prompt.append("Question: ").append(question.toString().trim()).append("\nOptions:\n");
        for (String opt : options) prompt.append(opt).append("\n");
        prompt.append("Analyis the question and Only reply with the correct answer text, not the letter or explanation.");
        return prompt.toString();
    }

    // 3. Call Gemini API (now fetch API key from Firestore first)
    private void sendToGemini(String prompt) {
        Log.d(TAG, "Fetching Gemini API key from Firestore...");
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("config").document("gemini");
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String apiKey = documentSnapshot.getString("apiKey");
                if (apiKey != null && !apiKey.isEmpty()) {
                    Log.d(TAG, "Gemini API key retrieved from Firestore.");
                    callGeminiApi(prompt, apiKey);
                } else {
                    Log.e(TAG, "Gemini API key is missing in Firestore document.");
                }
            } else {
                Log.e(TAG, "Gemini Firestore document does not exist.");
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch Gemini API key from Firestore", e);
        });
    }

    // Helper to call Gemini API with the provided key
    private void callGeminiApi(String prompt, String apiKey) {
        Log.d(TAG, "Calling Gemini API with prompt: " + prompt);
        OkHttpClient client = new OkHttpClient();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-pro-latest:generateContent?key=" + apiKey;
        String json = "{ \"contents\": [{ \"parts\": [{ \"text\": \"" + prompt.replace("\"", "\\\"") + "\" }] }] }";
        RequestBody body = RequestBody.create(json, MediaType.get("application/json; charset=utf-8"));
        Request request = new Request.Builder().url(url).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "Gemini API call failed", e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "Gemini API response: " + responseBody);
                    String answer = parseGeminiAnswer(responseBody);
                    if (answer != null) {
                        Log.d(TAG, "Gemini parsed answer: " + answer);
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(() -> clickAnswerByText(answer));
                    } else {
                        Log.d(TAG, "Gemini answer was null");
                    }
                } else {
                    Log.e(TAG, "Gemini API call failed: " + response.code() + " " + response.message());
                    String errorBody = response.body() != null ? response.body().string() : null;
                    if (errorBody != null) {
                        Log.e(TAG, "Gemini API error body: " + errorBody);
                    }
                }
            }
        });
    }

    // 4. Parse Gemini's response
    private String parseGeminiAnswer(String responseBody) {
        try {
            JSONObject obj = new JSONObject(responseBody);
            JSONArray candidates = obj.getJSONArray("candidates");
            if (candidates.length() > 0) {
                JSONObject content = candidates.getJSONObject(0).getJSONObject("content");
                JSONArray parts = content.getJSONArray("parts");
                if (parts.length() > 0) {
                    String text = parts.getJSONObject(0).getString("text");
                    // Only take the first line/word (in case Gemini adds extra)
                    return text.split("\\s")[0].replaceAll("[^\\dA-Za-z]", "");
                }
            }
        } catch (Exception e) {
            Log.e("Gemini", "Parse error: " + e.getMessage());
        }
        return null;
    }

    private long lastClickTime = 0;

    private void clickAnswerByText(String answerText) {
        long now = System.currentTimeMillis();
        if (now - lastClickTime < 1000) { // 1 second debounce
            Log.d(TAG, "Click ignored to prevent mis-touch");
            return;
        }
        lastClickTime = now;

        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) {
            Log.d(TAG, "Root node is null in clickAnswerByText");
            return;
        }
        AccessibilityNodeInfo answerNode = findNodeWithExactText(root, answerText);
        if (answerNode != null) {
            Log.d(TAG, "Clicking node with exact text: " + answerText);
            answerNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            answerNode.recycle();
            // Wait 5 seconds before clicking Next/Submit
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                clickNextQuestionButton();
            }, 5000); // 5000ms = 5 seconds
        } else {
            Log.d(TAG, "No node found with exact text: " + answerText);
        }
    }

    private AccessibilityNodeInfo findNodeWithExactText(AccessibilityNodeInfo node, String target) {
        if (node == null) return null;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (node.isClickable() && node.isVisibleToUser()) {
            if ((text != null && text.toString().trim().equalsIgnoreCase(target.trim())) ||
                    (desc != null && desc.toString().trim().equalsIgnoreCase(target.trim()))) {
                return node;
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findNodeWithExactText(child, target);
            if (result != null) return result;
        }
        return null;
    }

    // Replace Gemini/HuggingFace integration with OpenRouter AI
    private void sendToOpenRouterAI(String questionText, List<String> optionLabels) {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        DocumentReference docRef = db.collection("config").document("openrouter");
        docRef.get().addOnSuccessListener(documentSnapshot -> {
            if (documentSnapshot.exists()) {
                String apiKey = documentSnapshot.getString("apiKey");
                if (apiKey != null && !apiKey.isEmpty()) {
                    callOpenRouterApi(questionText, optionLabels, apiKey);
                } else {
                    Log.e(TAG, "OpenRouter API key is missing in Firestore document.");
                    selectRandomOption(optionLabels);
                }
            } else {
                Log.e(TAG, "OpenRouter Firestore document does not exist.");
                selectRandomOption(optionLabels);
            }
        }).addOnFailureListener(e -> {
            Log.e(TAG, "Failed to fetch OpenRouter API key from Firestore", e);
            selectRandomOption(optionLabels);
        });
    }

    private void callOpenRouterApi(String questionText, List<String> optionLabels, String apiKey) {
        OkHttpClient client = new OkHttpClient();
        String apiUrl = "https://openrouter.ai/api/v1/chat/completions";
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an expert at multiple choice questions.\n")
                .append("Choose the correct answer from the options below.\n")
                .append("Reply ONLY with the exact correct option text, and nothing else.\n")
                .append("If you don't know, reply with the most likely option.\n")
                .append("Question: ").append(questionText).append("\nOptions:\n");
        for (String opt : optionLabels) prompt.append(opt).append("\n");
        prompt.append("Correct answer:");

        JSONObject body = new JSONObject();
        try {
            body.put("model", "openai/gpt-3.5-turbo");
            JSONArray messages = new JSONArray();
            JSONObject userMsg = new JSONObject();
            userMsg.put("role", "user");
            userMsg.put("content", prompt.toString());
            messages.put(userMsg);
            body.put("messages", messages);
        } catch (JSONException e) {
            Log.e(TAG, "JSON error", e);
            selectRandomOption(optionLabels);
            return;
        }

        Request request = new Request.Builder()
                .url(apiUrl)
                .post(RequestBody.create(body.toString(), MediaType.get("application/json")))
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .build();

        Handler handler = new Handler(Looper.getMainLooper());
        Runnable timeoutRunnable = () -> {
            // Fallback logic if AI is too slow
            selectRandomOption(optionLabels);
        };
        handler.postDelayed(timeoutRunnable, 30000); // 15 seconds for content generation

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e(TAG, "OpenRouter API call failed", e);
                handler.removeCallbacks(timeoutRunnable); // Ensure timeout is removed on failure
                selectRandomOption(optionLabels);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                handler.removeCallbacks(timeoutRunnable); // Ensure timeout is removed on success
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    Log.d(TAG, "OpenRouter API response: " + responseBody);
                    String aiAnswer = parseOpenRouterAnswer(responseBody);
                    if (aiAnswer != null) {
                        Handler mainHandler = new Handler(Looper.getMainLooper());
                        mainHandler.post(() -> {
                            boolean matched = false;
                            for (String opt : optionLabels) {
                                if (opt.equalsIgnoreCase(aiAnswer.trim())) {
                                    clickAnswerByText(opt);
                                    matched = true;
                                    break;
                                }
                            }
                            if (!matched) selectRandomOption(optionLabels);
                        });
                    } else {
                        selectRandomOption(optionLabels);
                    }
                } else {
                    Log.e(TAG, "OpenRouter API error: " + response.code());
                    handler.removeCallbacks(timeoutRunnable); // Ensure timeout is removed on error
                    selectRandomOption(optionLabels);
                }
            }
        });
    }

    private String parseOpenRouterAnswer(String responseBody) {
        try {
            JSONObject obj = new JSONObject(responseBody);
            JSONArray choices = obj.getJSONArray("choices");
            if (choices.length() > 0) {
                JSONObject message = choices.getJSONObject(0).getJSONObject("message");
                return message.getString("content").trim();
            }
        } catch (Exception e) {
            Log.e(TAG, "OpenRouter parse error: " + e.getMessage());
        }
        return null;
    }

    private void selectRandomOption(List<String> optionLabels) {
        if (optionLabels == null || optionLabels.isEmpty()) return;
        int randomIndex = (int) (Math.random() * optionLabels.size());
        String randomOption = optionLabels.get(randomIndex);
        Handler mainHandler = new Handler(Looper.getMainLooper());
        mainHandler.post(() -> clickAnswerByText(randomOption));
    }

    // Log all clickable and visible nodes for debugging
    private void logClickableVisibleNodes(AccessibilityNodeInfo node) {
        if (node == null) return;
        if (node.isClickable() && node.isVisibleToUser()) {
            StringBuilder sb = new StringBuilder();
            sb.append("[Clickable Node] ");
            if (node.getText() != null) sb.append("text: ").append(node.getText()).append(" ");
            if (node.getContentDescription() != null) sb.append("desc: ").append(node.getContentDescription()).append(" ");
            sb.append("class: ").append(node.getClassName());
            Log.d(TAG, sb.toString());
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            logClickableVisibleNodes(child);
            if (child != null) child.recycle();
        }
    }

    // Update solveCurrentQuestion to use OpenRouter
    public void solveCurrentQuestion() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        dumpNodeTree(root, 0); // Dump the tree for debugging
        FeedbackProvider.speakAndToast(this, "Solving current question.");

        String question = null;
        List<String> options = new ArrayList<>();
        int maxQuestionLength = 0;
        Set<String> optionSet = new HashSet<>();
        List<String> ignoreList = Arrays.asList( "Submit", "Open the home page", "New tab", "See 4 tabs", "Customize and control Google Chrome", "HRS", "Mins", "Secs");

        if (root != null) {
            List<AccessibilityNodeInfo> allNodes = new ArrayList<>();
            collectAllNodes(root, allNodes);
            for (AccessibilityNodeInfo node : allNodes) {
                CharSequence text = node.getText();
                CharSequence desc = node.getContentDescription();
                String t = (text != null && text.length() > 0) ? text.toString().trim() : null;
                String d = (desc != null && desc.length() > 0) ? desc.toString().trim() : null;

                // Use contentDescription if text is null/empty
                String value = (t != null && !t.isEmpty()) ? t : (d != null && !d.isEmpty() ? d : null);
                if (value == null || value.isEmpty() || ignoreList.contains(value)) continue;

                // Find the question: longest visible TextView or View with non-empty text/desc
                if (node.getClassName() != null &&
                        (node.getClassName().toString().contains("TextView") || node.getClassName().toString().contains("View")) &&
                        value.length() > maxQuestionLength && node.isVisibleToUser() && !value.matches("^[A-D]\\).*")) {
                    question = value;
                    maxQuestionLength = value.length();
                }
                // Find options: clickable and visible RadioButton or TextView with non-empty text/desc, not in ignoreList, and not duplicate
                if (node.isClickable() && node.isVisibleToUser() && value.length() > 0 &&
                        (node.getClassName() != null && (node.getClassName().toString().contains("RadioButton") || node.getClassName().toString().contains("TextView"))) &&
                        !optionSet.contains(value) && !ignoreList.contains(value)) {
                    options.add(value);
                    optionSet.add(value);
                }
            }
            root.recycle();
        }

        Log.d(TAG, "Extracted question: " + question);
        Log.d(TAG, "Extracted options: " + options);

        if (question != null && !options.isEmpty()) {
            sendToOpenRouterAI(question, options);
        } else {
            Log.d(TAG, "Could not extract question/options, picking random.");
            selectRandomOption(options);
        }
    }

    // Helper to collect all nodes recursively
    private void collectAllNodes(AccessibilityNodeInfo node, List<AccessibilityNodeInfo> out) {
        if (node == null) return;
        out.add(node);
        for (int i = 0; i < node.getChildCount(); i++) {
            collectAllNodes(node.getChild(i), out);
        }
    }

    private int currentQuestionNumber = 1;
    private boolean timerDialogShown = false;
    private int testDurationMinutes = 0;
    private Handler submitHandler = new Handler(Looper.getMainLooper());
    private Runnable submitRunnable = new Runnable() {
        @Override
        public void run() {
            performSubmit();
        }
    };

    // Full automation loop: answer, next, submit
    private void automateTestFlow() {
        Log.d(TAG, "Starting automateTestFlow");
        if (!timerDialogShown) {
            timerDialogShown = true;
            Intent dialogIntent = new Intent(this, TestTimerDialogActivity.class);
            dialogIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(dialogIntent);
            // Wait for timer to be set before proceeding
            return;
        }
        String ordinal = getOrdinal(currentQuestionNumber);
        FeedbackProvider.speakAndToast(this, "Solving " + ordinal + " question.");
        solveCurrentQuestion();
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                AccessibilityNodeInfo nextNode = findNodeByTextOrDesc(root, "Next Question");
                if (nextNode == null) {
                    nextNode = findNodeByTextOrDesc(root, "Next");
                }
                if (nextNode != null && nextNode.isVisibleToUser()) {
                    Log.d(TAG, "Next node found: " + nextNode.getClassName() + " clickable: " + nextNode.isClickable() + " visible: " + nextNode.isVisibleToUser());
                    AccessibilityNodeInfo clickableNode = nextNode.isClickable() ? nextNode : findClickableAncestor(nextNode);
                    if (clickableNode != null && clickableNode.isVisibleToUser()) {
                        clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        clickableNode.recycle();
                        Log.d(TAG, "Clicked Next Question node");
                        currentQuestionNumber++;
                        new Handler(Looper.getMainLooper()).postDelayed(this::automateTestFlow, 10000);
                    } else {
                        Log.d(TAG, "No clickable ancestor found for Next Question");
                    }
                } else {
                    Log.d(TAG, "No Next Question node found, checking for Submit");
                    // Only click Submit if Next Question is not present
                    AccessibilityNodeInfo submitNode = findNodeByTextOrDesc(root, "Submit");
                    if (submitNode != null && submitNode.isClickable() && submitNode.isVisibleToUser()) {
                        Log.d(TAG, "Found Submit node, clicking");
                        submitNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                        submitNode.recycle();
                        FeedbackProvider.speakAndToast(this, "Test submitted. Automation complete.");
                        Log.d(TAG, "Test automation complete: submitted.");
                        currentQuestionNumber = 1;
                    } else {
                        Log.d(TAG, "No Submit node found");
                    }
                }
                root.recycle();
            } else {
                Log.d(TAG, "Root node is null in automateTestFlow");
            }
        }, 2000);
    }

    private void scheduleSubmitBeforeEnd() {
        // Schedule submit 1 minute before testDurationMinutes
        long millis = (testDurationMinutes - 1) * 60 * 1000L;
        submitHandler.postDelayed(submitRunnable, millis);
    }

    // Helper to get ordinal string for a number (1st, 2nd, 3rd, 4th, etc.)
    private String getOrdinal(int number) {
        if (number % 100 >= 11 && number % 100 <= 13) {
            return number + "th";
        }
        switch (number % 10) {
            case 1: return number + "st";
            case 2: return number + "nd";
            case 3: return number + "rd";
            default: return number + "th";
        }
    }

    private AccessibilityNodeInfo findNodeByPartialTextOrDesc(AccessibilityNodeInfo node, String target) {
        if (node == null) return null;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if ((text != null && text.toString().toLowerCase().contains(target.toLowerCase())) ||
                (desc != null && desc.toString().toLowerCase().contains(target.toLowerCase()))) {
            return node;
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            AccessibilityNodeInfo result = findNodeByPartialTextOrDesc(child, target);
            if (result != null) return result;
        }
        return null;
    }

    private void clickNextQuestionButton() {
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root == null) return;
        AccessibilityNodeInfo nextNode = findNodeByPartialTextOrDesc(root, "Next");
        if (nextNode != null && nextNode.isVisibleToUser()) {
            AccessibilityNodeInfo clickableNode = nextNode.isClickable() ? nextNode : findClickableAncestor(nextNode);
            if (clickableNode != null && clickableNode.isVisibleToUser()) {
                clickableNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                clickableNode.recycle();
                Log.d(TAG, "Clicked Next Question node");
                currentQuestionNumber++;
                // Schedule the next question automation after a short delay
                new Handler(Looper.getMainLooper()).postDelayed(this::automateTestFlow, 1000); // 1s delay, adjust as needed
            } else {
                Log.d(TAG, "No clickable ancestor found for Next Question");
            }
        } else {
            Log.d(TAG, "No Next Question node found");
        }
        root.recycle();
    }

    // Improved method to find Next/Submit/Continue button by text or contentDescription
    private AccessibilityNodeInfo findNextButton(AccessibilityNodeInfo root) {
        if (root == null) return null;
        String[] possibleLabels = {"Next Question", "Next", "Continue", "Proceed", "Submit"};
        for (String label : possibleLabels) {
            AccessibilityNodeInfo node = findNodeByTextOrDesc(root, label);
            if (node != null && (node.isClickable() || findClickableAncestor(node) != null)) {
                return node.isClickable() ? node : findClickableAncestor(node);
            }
        }
        return null;
    }

    // --- Debug and Utility Methods ---
    private void debugQuickSettings() {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                dumpAccessibilityTree(root, 0);
                root.recycle();
            }
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
            }
        }, 1000);
    }

    private void listQuickSettingsTiles() {
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                listAvailableTiles(root);
                root.recycle();
            }
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
            }
        }, 700);
    }

    private void listAvailableTiles(AccessibilityNodeInfo node) {
        if (node == null) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (node.isClickable() && node.isVisibleToUser()) {
            if (text != null && text.length() > 0) {
                Log.d(TAG, "Available Quick Settings Tile: '" + text + "'");
                FeedbackProvider.speakAndToast(this, "Found tile: " + text);
                storeTileInfo(text.toString(), "");
            } else if (desc != null && desc.length() > 0) {
                Log.d(TAG, "Available Quick Settings Tile: '" + desc + "'");
                FeedbackProvider.speakAndToast(this, "Found tile: " + desc);
                storeTileInfo("", desc.toString());
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            listAvailableTiles(child);
        }
    }

    private void storeTileInfo(String text, String description) {
        SharedPreferences prefs = getSharedPreferences("QuickSettingsPrefs", Context.MODE_PRIVATE);
        Set<String> availableTiles = prefs.getStringSet("available_tiles", new HashSet<>());
        Set<String> newTiles = new HashSet<>(availableTiles);
        
        if (!text.isEmpty()) newTiles.add(text.toLowerCase());
        if (!description.isEmpty()) newTiles.add(description.toLowerCase());
        
        prefs.edit().putStringSet("available_tiles", newTiles).apply();
    }

    private void discoverQuickSettingsTiles() {
        Log.d(TAG, "Auto-discovering quick settings tiles");
        performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            AccessibilityNodeInfo root = getRootInActiveWindow();
            if (root != null) {
                listAvailableTiles(root);
                root.recycle();
                Log.d(TAG, "Quick settings tiles discovery completed");
            }
            if (android.os.Build.VERSION.SDK_INT >= 28) {
                performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
            }
        }, 1000);
    }

    // --- Auto Photo Capture ---
    private void takePhotoAuto() {
        FeedbackProvider.speakAndToast(this, "Opening camera and taking photo");
        
        // Find camera apps dynamically from installed apps using shared utility
        String cameraPackage = AppUtils.findCameraAppPackage(this);
        if (cameraPackage != null) {
            Intent cameraIntent = new Intent(Intent.ACTION_MAIN);
            cameraIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            cameraIntent.setPackage(cameraPackage);
            cameraIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            try {
                startActivity(cameraIntent);
            } catch (Exception e) {
                Log.e(TAG, "Failed to open camera app: " + cameraPackage, e);
                FeedbackProvider.speakAndToast(this, "Could not open camera app");
                return;
            }
        } else {
            FeedbackProvider.speakAndToast(this, "No camera app found on device");
            return;
        }
        
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            attemptPhotoCapture(1);
        }, 3000);
    }

    private void takePhotoOnly() {
        Log.d(TAG, "Taking photo only (camera already open)");
        attemptPhotoCapture(1);
    }

    private void takePhotoWithTimer(int seconds, boolean isSelfie) {
        Log.d(TAG, "takePhotoWithTimer called with " + seconds + " seconds, selfie: " + isSelfie);
        
        // Start countdown from seconds down to 1
        for (int i = seconds; i > 0; i--) {
            final int currentSecond = i;
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                if (currentSecond > 3) {
                    // For longer countdowns, only announce at 5, 3, 2, 1
                    if (currentSecond == 5 || currentSecond <= 3) {
                        FeedbackProvider.speakAndToast(this, currentSecond + "");
                    }
                } else {
                    // For shorter countdowns, announce every second
                    FeedbackProvider.speakAndToast(this, currentSecond + "");
                }
            }, (seconds - currentSecond) * 1000L);
        }
        
        // Take photo after countdown completes
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            FeedbackProvider.speakAndToast(this, "Taking " + (isSelfie ? "selfie" : "photo") + " now!");
            takePhotoOnly();
        }, seconds * 1000L);
    }

    private void attemptPhotoCapture(int attempt) {
        Log.d(TAG, "Attempting photo capture, attempt " + attempt);
        AccessibilityNodeInfo root = getRootInActiveWindow();
        if (root != null) {
            boolean captured = findAndClickCameraCaptureButton(root, "capture");
            if (captured) {
                Log.d(TAG, "Photo capture button found and clicked on attempt " + attempt);
                FeedbackProvider.speakAndToast(this, "Photo captured successfully");
                // Wait longer for photo to be processed and saved, then go back to close camera
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    performGlobalAction(GLOBAL_ACTION_BACK);
                }, 5000); // Increased from 2000ms to 5000ms to ensure photo is saved
            } else if (attempt < 3) {
                Log.d(TAG, "Photo capture button not found on attempt " + attempt + ", retrying...");
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    attemptPhotoCapture(attempt + 1);
                }, 1000);
            } else {
                Log.w(TAG, "Could not find camera capture button after " + attempt + " attempts");
                //FeedbackProvider.speakAndToast(this, "Could not find camera capture button");
            }
            root.recycle();
        } else {
            Log.e(TAG, "Root node is null on attempt " + attempt);
            if (attempt < 3) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    attemptPhotoCapture(attempt + 1);
                }, 1000);
            } else {
                //FeedbackProvider.speakAndToast(this, "Cannot access camera interface");
            }
        }
    }

    private void logAllClickableButtons(AccessibilityNodeInfo node) {
        if (node == null) return;
        
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String className = node.getClassName() != null ? node.getClassName().toString() : "";
        String resourceId = node.getViewIdResourceName();
        
        if (node.isClickable() && node.isVisibleToUser()) {
            Log.d(TAG, "📱 CLICKABLE BUTTON: text='" + text + "', desc='" + desc + "', resourceId='" + resourceId + "', className='" + className + "'");
        }
        
        // Recursively search children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            logAllClickableButtons(child);
        }
    }

    private boolean findAndClickCameraCaptureButton(AccessibilityNodeInfo node, String buttonText) {
        if (node == null) return false;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        String className = node.getClassName() != null ? node.getClassName().toString() : "";
        String resourceId = node.getViewIdResourceName();

        // Debug: Log all clickable nodes to help identify the capture button
        if (node.isClickable() && node.isVisibleToUser()) {
            Log.d(TAG, "Found clickable node: text='" + text + "', desc='" + desc + "', resourceId='" + resourceId + "', className='" + className + "'");
        }

        if (node.isClickable() && node.isVisibleToUser()) {
            // Look for capture-related labels and classes
            boolean isCaptureButton = false;
            String matchReason = "";

            // Priority 1: Exact match with the provided button text
            if (text != null && text.toString().equalsIgnoreCase(buttonText)) {
                isCaptureButton = true;
                matchReason = "exact text match: '" + text + "'";
            }
            // Priority 2: Content description exact match
            else if (desc != null && desc.toString().equalsIgnoreCase(buttonText)) {
                isCaptureButton = true;
                matchReason = "exact description match: '" + desc + "'";
            }
            // Priority 3: Check text content for capture-related keywords
            else if (text != null) {
                String lowerText = text.toString().toLowerCase();
                if (lowerText.equals("capture") || lowerText.equals("photo") ||
                    lowerText.equals("shoot") || lowerText.equals("take") ||
                    lowerText.contains("capture") || lowerText.contains("photo")) {
                    isCaptureButton = true;
                    matchReason = "text contains capture keyword: '" + text + "'";
                }
            }
            // Priority 4: Check content description for capture-related keywords
            else if (desc != null) {
                String lowerDesc = desc.toString().toLowerCase();
                if (lowerDesc.equals("capture") || lowerDesc.equals("photo") ||
                    lowerDesc.equals("shoot") || lowerDesc.equals("take") ||
                    lowerDesc.contains("capture") || lowerDesc.contains("photo")) {
                    isCaptureButton = true;
                    matchReason = "description contains capture keyword: '" + desc + "'";
                }
            }
            // Priority 5: Check resource ID for capture-related patterns
            else if (resourceId != null) {
                String lowerResourceId = resourceId.toLowerCase();
                if (lowerResourceId.contains("capture") || lowerResourceId.contains("photo") ||
                    lowerResourceId.contains("shutter") || lowerResourceId.contains("shoot")) {
                    isCaptureButton = true;
                    matchReason = "resource ID contains capture keyword: '" + resourceId + "'";
                }
            }
            // Priority 6: Check class name for camera-specific classes
            else if (className.toLowerCase().contains("shutter") ||
                    className.toLowerCase().contains("capture") ||
                    className.toLowerCase().contains("camera")) {
                isCaptureButton = true;
                matchReason = "class name contains camera keyword: '" + className + "'";
            }

            if (isCaptureButton) {
                Log.d(TAG, "🎯 FOUND CAMERA CAPTURE BUTTON! " + matchReason);
                Log.d(TAG, "Button details: text='" + text + "', desc='" + desc + "', resourceId='" + resourceId + "', className='" + className + "'");
                node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                return true;
            }
        }

        // Recursively search children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            boolean found = findAndClickCameraCaptureButton(child, buttonText);
            if (found) return true;
        }
        return false;
    }

    private final BroadcastReceiver quickSettingsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if ("com.mvp.sarah.ACTION_TOGGLE_QUICK_SETTINGS_TILE".equals(intent.getAction())) {
                String tileLabel = intent.getStringExtra("tile_label");
                boolean hasState = intent.hasExtra("should_turn_on");
                boolean shouldTurnOn = intent.getBooleanExtra("should_turn_on", true);
                if (tileLabel != null) {
                    if (hasState) {
                        triggerQuickSettingsTileWithState(tileLabel, shouldTurnOn);
                    } else {
                        triggerQuickSettingsTile(tileLabel);
                    }
                }
            } else if ("com.mvp.sarah.ACTION_LIST_QUICK_SETTINGS_TILES".equals(intent.getAction())) {
                performGlobalAction(GLOBAL_ACTION_QUICK_SETTINGS);
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    AccessibilityNodeInfo root = getRootInActiveWindow();
                    if (root != null) {
                        listAndSpeakAvailableTiles(root);
                        root.recycle();
                    } else {
                        FeedbackProvider.speakAndToast(ClickAccessibilityService.this, "Could not access quick settings panel");
                    }
                    if (android.os.Build.VERSION.SDK_INT >= 28) {
                        performGlobalAction(GLOBAL_ACTION_DISMISS_NOTIFICATION_SHADE);
                    }
                }, 200);
            }
        }
    };

    private void listAndSpeakAvailableTiles(AccessibilityNodeInfo node) {
        if (node == null) return;
        CharSequence text = node.getText();
        CharSequence desc = node.getContentDescription();
        if (node.isClickable() && node.isVisibleToUser()) {
            if (text != null && text.length() > 0) {
                Log.d(TAG, "Available Quick Settings Tile: '" + text + "'");
                FeedbackProvider.speakAndToast(this, "Tile: " + text);
            } else if (desc != null && desc.length() > 0) {
                Log.d(TAG, "Available Quick Settings Tile: '" + desc + "'");
                FeedbackProvider.speakAndToast(this, "Tile: " + desc);
            }
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            listAndSpeakAvailableTiles(child);
        }
    }
}