package com.mvp.sarah.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.PixelFormat;
import android.hardware.display.DisplayManager;
import android.hardware.display.VirtualDisplay;
import android.media.Image;
import android.media.ImageReader;
import android.media.projection.MediaProjection;
import android.media.projection.MediaProjectionManager;
import android.os.Handler;
import android.os.Looper;
import android.view.WindowManager;
import android.util.DisplayMetrics;

public class ScreenCapture {

    private static final ScreenCapture INSTANCE = new ScreenCapture();
    private MediaProjectionManager mediaProjectionManager;
    private MediaProjection mediaProjection;
    private VirtualDisplay virtualDisplay;
    private ImageReader imageReader;
    private Handler handler = new Handler(Looper.getMainLooper());

    private ScreenCapture() {}

    public static ScreenCapture getInstance() {
        return INSTANCE;
    }

    public void setup(Context context) {
        mediaProjectionManager = (MediaProjectionManager) context.getSystemService(Context.MEDIA_PROJECTION_SERVICE);
    }

    public void startProjection(int resultCode, Intent data) {
        mediaProjection = mediaProjectionManager.getMediaProjection(resultCode, data);
    }

    public void captureScreen(ScreenCaptureCallback callback) {
        if (mediaProjection == null) {
            callback.onScreenCaptured(null);
            return;
        }

        WindowManager windowManager = (WindowManager) App.getContext().getSystemService(Context.WINDOW_SERVICE);
        int width = windowManager.getDefaultDisplay().getWidth();
        int height = windowManager.getDefaultDisplay().getHeight();

        imageReader = ImageReader.newInstance(width, height, PixelFormat.RGBA_8888, 2);
        DisplayMetrics metrics = new DisplayMetrics();
        windowManager.getDefaultDisplay().getMetrics(metrics);
        int densityDpi = metrics.densityDpi;
        virtualDisplay = mediaProjection.createVirtualDisplay("ScreenCapture",
                width, height, densityDpi,
                DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
                imageReader.getSurface(), null, handler);

        imageReader.setOnImageAvailableListener(reader -> {
            Image image = null;
            Bitmap bitmap = null;
            try {
                image = reader.acquireLatestImage();
                if (image != null) {
                    Image.Plane[] planes = image.getPlanes();
                    // ... (code to convert image to bitmap)
                    callback.onScreenCaptured(bitmap);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                if (image != null) {
                    image.close();
                }
                if (virtualDisplay != null) {
                    virtualDisplay.release();
                }
            }
        }, handler);
    }

    public interface ScreenCaptureCallback {
        void onScreenCaptured(Bitmap bitmap);
    }
}
