package com.mvp.sara;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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

import com.mvp.sara.handlers.ClickLabelHandler;
import com.mvp.sara.handlers.TypeTextHandler;
import com.mvp.sara.handlers.WhatsAppHandler;

import java.util.List;
import java.util.Locale;

public class ClickAccessibilityService extends AccessibilityService {

    private static final String TAG = "ClickAccessibilitySvc";

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
                        performDoubleTapOnLike();
                    } else if (label.equalsIgnoreCase("switch_camera")) {
                        Log.d(TAG, "About to switch camera");
                        switchCamera();
                    }
                    else {
                        clickNodeWithText(label);
                    }
                } else {
                    Log.w(TAG, "Received broadcast but label was null or empty");
                }
            } else if ("com.mvp.sara.ACTION_ANSWER_CALL".equals(intent.getAction())) {
                Log.d(TAG, "About to answer call");
                answerCall();
            } else if ("com.mvp.sara.ACTION_REJECT_CALL".equals(intent.getAction())) {
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
            } else if ("com.mvp.sara.ACTION_SEND_WHATSAPP".equals(intent.getAction())) {
                Log.d(TAG, "Received SEND_WHATSAPP action");
                String contactName = intent.getStringExtra("contact_name");
                String message = intent.getStringExtra("message");
                performWhatsAppSend(contactName, message);
            }
        }
    };

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
        setServiceInfo(info);

        // Register broadcast receiver for click commands and call actions
        IntentFilter filter = new IntentFilter();
        filter.addAction(ClickLabelHandler.ACTION_CLICK_LABEL);
        filter.addAction("com.mvp.sara.ACTION_ANSWER_CALL");
        filter.addAction("com.mvp.sara.ACTION_REJECT_CALL");
        filter.addAction(TypeTextHandler.ACTION_TYPE_TEXT);
        filter.addAction(TypeTextHandler.ACTION_NEXT_LINE);
        filter.addAction(TypeTextHandler.ACTION_SELECT_ALL);
        filter.addAction(TypeTextHandler.ACTION_COPY);
        filter.addAction(TypeTextHandler.ACTION_CUT);
        filter.addAction(TypeTextHandler.ACTION_PASTE);
        filter.addAction("com.mvp.sara.ACTION_SEND_WHATSAPP");
        registerReceiver(clickReceiver, filter, RECEIVER_EXPORTED);
    }

    private void clickNodeWithText(String text) {
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode == null) {
            Log.e(TAG, "Root node is null. Cannot perform click.");
            FeedbackProvider.speakAndToast(this, "I can't see the screen right now.");
            return;
        }

        List<AccessibilityNodeInfo> nodes = rootNode.findAccessibilityNodeInfosByText(text);
        rootNode.recycle();

        if (nodes != null && !nodes.isEmpty()) {
            // Attempt to click the first visible and clickable node
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null && node.isVisibleToUser() && node.isClickable()) {
                    Log.d(TAG, "Found a clickable node with text: " + text);
                    node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    node.recycle();
                    return; // Clicked the first one, so we are done
                }
                if (node != null) {
                    node.recycle();
                }
            }
        }

        // If no direct match, try a more lenient search
        findAndClickPartialText(getRootInActiveWindow(), text);
    }

    private void findAndClickPartialText(AccessibilityNodeInfo parentNode, String text) {
        if (parentNode == null) return;

        for (int i = 0; i < parentNode.getChildCount(); i++) {
            AccessibilityNodeInfo childNode = parentNode.getChild(i);
            if (childNode == null) continue;

            CharSequence nodeText = childNode.getText();
            if (nodeText != null && nodeText.toString().toLowerCase(Locale.ROOT).contains(text.toLowerCase(Locale.ROOT))) {
                if (childNode.isClickable() && childNode.isVisibleToUser()) {
                    Log.d(TAG, "Found a clickable node with partial text: " + text);
                    childNode.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                    childNode.recycle();
                    parentNode.recycle();
                    return;
                }
            }
            // Recurse
            findAndClickPartialText(childNode, text);
            childNode.recycle();
        }
    }

    private void performDoubleTapOnLike() {
        Log.d(TAG, "performDoubleTapOnLike called");
        
        // Get screen dimensions
        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;
        
        // Calculate center coordinates
        int centerX = screenWidth / 2;
        int centerY = screenHeight / 2;
        
        Log.d(TAG, "Screen dimensions: " + screenWidth + "x" + screenHeight);
        Log.d(TAG, "Performing double-tap at center of screen: (" + centerX + ", " + centerY + ")");
        
        // Perform first tap
        Log.d(TAG, "Performing first tap");
        performTapGesture(centerX, centerY);
        
        // Small delay to mimic human double-tap
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // Perform second tap
            Log.d(TAG, "Performing second tap");
            performTapGesture(centerX, centerY);
            Log.d(TAG, "Double-tap completed");
        }, 200); // 200 ms delay
    }
    
    private void performTapGesture(int x, int y) {
        Log.d(TAG, "performTapGesture called at (" + x + ", " + y + ")");
        Path path = new Path();
        path.moveTo(x, y);
        
        GestureDescription.Builder builder = new GestureDescription.Builder();
        builder.addStroke(new GestureDescription.StrokeDescription(path, 0, 100));
        
        boolean result = dispatchGesture(builder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                Log.d(TAG, "Tap gesture completed successfully");
            }
            
            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                Log.e(TAG, "Tap gesture was cancelled");
            }
        }, null);
        
        Log.d(TAG, "dispatchGesture result: " + result);
    }

    private void performScroll() {
        // Get the screen dimensions
        int height = getResources().getDisplayMetrics().heightPixels;
        int width = getResources().getDisplayMetrics().widthPixels;

        // Define the scroll path
        Path swipePath = new Path();
        swipePath.moveTo(width / 2, height * 0.8f); // Start near the bottom
        swipePath.lineTo(width / 2, height * 0.2f); // End near the top

        // Create the gesture
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 200)); // 200ms duration

        // Dispatch the gesture
        dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Scroll gesture completed.");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Scroll gesture cancelled.");
            }
        }, null);
    }

    private void performScrollUp() {
        // Get the screen dimensions
        int height = getResources().getDisplayMetrics().heightPixels;
        int width = getResources().getDisplayMetrics().widthPixels;

        // Define the scroll path (swipe down to scroll up)
        Path swipePath = new Path();
        swipePath.moveTo(width / 2, height * 0.2f); // Start near the top
        swipePath.lineTo(width / 2, height * 0.8f); // End near the bottom

        // Create the gesture
        GestureDescription.Builder gestureBuilder = new GestureDescription.Builder();
        gestureBuilder.addStroke(new GestureDescription.StrokeDescription(swipePath, 0, 200)); // 200ms duration

        // Dispatch the gesture
        dispatchGesture(gestureBuilder.build(), new GestureResultCallback() {
            @Override
            public void onCompleted(GestureDescription gestureDescription) {
                super.onCompleted(gestureDescription);
                Log.d(TAG, "Scroll up gesture completed.");
            }

            @Override
            public void onCancelled(GestureDescription gestureDescription) {
                super.onCancelled(gestureDescription);
                Log.w(TAG, "Scroll up gesture cancelled.");
            }
        }, null);
    }

    private void switchCamera() {
        Log.d(TAG, "switchCamera called");
        
        // First try to find the camera switch button using accessibility
        AccessibilityNodeInfo rootNode = getRootInActiveWindow();
        if (rootNode != null) {
            Log.d(TAG, "Root node found, searching for camera switch button");
            AccessibilityNodeInfo switchButton = findCameraSwitchButton(rootNode);
            if (switchButton != null) {
                Log.d(TAG, "Found camera switch button using accessibility, clicking it");
                switchButton.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                switchButton.recycle();
                rootNode.recycle();
                FeedbackProvider.speakAndToast(this, "Switching camera.");
                return;
            } else {
                Log.d(TAG, "No camera switch button found using accessibility");
            }
            rootNode.recycle();
        } else {
            Log.d(TAG, "Root node is null, cannot search for camera switch button");
        }
        
        // Fallback: use position-based method
        Log.d(TAG, "Camera switch button not found, using fallback position method");
        performFallbackCameraSwitch();
    }
    
    private void performFallbackCameraSwitch() {
        // Get screen dimensions to calculate relative position
        android.util.DisplayMetrics displayMetrics = getResources().getDisplayMetrics();
        int screenWidth = displayMetrics.widthPixels;
        int screenHeight = displayMetrics.heightPixels;

        // Based on the user's screenshot, the button is in the bottom-right corner,
        // in line with the shutter button.
        // X: To the right of the shutter button, around 88% of screen width.
        // Y: Inside the bottom control bar, around 93% of screen height.
        int x = (int) (screenWidth * 0.88f);
        int y = (int) (screenHeight * 0.93f);

        Log.d(TAG, "Screen dimensions: " + screenWidth + "x" + screenHeight);
        Log.d(TAG, "Tapping camera switch button at refined fallback position: (" + x + ", " + y + ")");

        performTapAtPosition(x, y);
        FeedbackProvider.speakAndToast(this, "Switching camera.");
    }
    
    private AccessibilityNodeInfo findCameraSwitchButton(AccessibilityNodeInfo node) {
        if (node == null) return null;
        
        // Log all clickable nodes for debugging
        if (node.isClickable()) {
            CharSequence text = node.getText();
            String resourceId = node.getViewIdResourceName();
            CharSequence contentDesc = node.getContentDescription();
            Log.d(TAG, "Found clickable node - Text: '" + text + "', ResourceID: '" + resourceId + "', ContentDesc: '" + contentDesc + "'");
        }
        
        // Try to find by text content (more specific patterns)
        CharSequence text = node.getText();
        if (text != null) {
            String lowerText = text.toString().toLowerCase();
            Log.d(TAG, "Checking text: '" + text + "' (lowercase: '" + lowerText + "')");
            
            if (lowerText.contains("switch") || lowerText.contains("camera") || 
                lowerText.contains("flip") || lowerText.contains("front") || 
                lowerText.contains("back") || lowerText.contains("switch camera") ||
                lowerText.contains("camera switch") || lowerText.contains("flip camera") ||
                lowerText.contains("camera flip") || lowerText.equals("switch") ||
                lowerText.equals("flip") || lowerText.equals("camera")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found camera switch button by text: '" + text + "'");
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }
        
        // Try to find by resource ID (more specific patterns)
        String resourceId = node.getViewIdResourceName();
        if (resourceId != null) {
            String lowerResourceId = resourceId.toLowerCase();
            Log.d(TAG, "Checking resource ID: '" + resourceId + "' (lowercase: '" + lowerResourceId + "')");
            
            if (lowerResourceId.contains("switch") || 
                lowerResourceId.contains("camera") ||
                lowerResourceId.contains("flip") ||
                lowerResourceId.contains("switch_camera") ||
                lowerResourceId.contains("camera_switch") ||
                lowerResourceId.contains("flip_camera") ||
                lowerResourceId.contains("camera_flip") ||
                lowerResourceId.contains("switchcamera") ||
                lowerResourceId.contains("camerabutton") ||
                lowerResourceId.contains("flipbutton")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found camera switch button by resource ID: '" + resourceId + "'");
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }
        
        // Try to find by content description
        CharSequence contentDesc = node.getContentDescription();
        if (contentDesc != null) {
            String lowerContentDesc = contentDesc.toString().toLowerCase();
            Log.d(TAG, "Checking content description: '" + contentDesc + "' (lowercase: '" + lowerContentDesc + "')");
            
            if (lowerContentDesc.contains("switch") || 
                lowerContentDesc.contains("camera") ||
                lowerContentDesc.contains("flip") ||
                lowerContentDesc.contains("switch camera") ||
                lowerContentDesc.contains("camera switch") ||
                lowerContentDesc.contains("flip camera") ||
                lowerContentDesc.contains("camera flip")) {
                if (node.isClickable()) {
                    Log.d(TAG, "Found camera switch button by content description: '" + contentDesc + "'");
                    return AccessibilityNodeInfo.obtain(node);
                }
            }
        }
        
        // Recursively search children
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                AccessibilityNodeInfo result = findCameraSwitchButton(child);
                if (result != null) {
                    child.recycle();
                    return result;
                }
                child.recycle();
            }
        }
        
        return null;
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
                telecomManager.endCall();
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
        if (node != null && node.isEditable()) {
            Log.d(TAG, "Performing TYPE_TEXT: " + text);
            Bundle args = new Bundle();
            args.putCharSequence(AccessibilityNodeInfo.ACTION_ARGUMENT_SET_TEXT_CHARSEQUENCE, text);
            node.performAction(AccessibilityNodeInfo.ACTION_SET_TEXT, args);
            node.recycle();
        } else {
            Log.w(TAG, "No editable text field focused for TYPE_TEXT");
            FeedbackProvider.speakAndToast(this, "No text field is focused");
        }
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
            AccessibilityNodeInfo result = findFocusedEditableNode(node.getChild(i));
            if (result != null) return result;
        }
        return null;
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // Not used for this implementation
    }

    @Override
    public void onInterrupt() {
        // Not used
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(clickReceiver);
    }
} 