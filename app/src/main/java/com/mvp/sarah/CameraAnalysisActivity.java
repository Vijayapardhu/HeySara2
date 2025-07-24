package com.mvp.sarah;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.camera.core.CameraSelector;
import androidx.camera.core.ImageCapture;
import androidx.camera.core.ImageCaptureException;
import androidx.camera.core.Preview;
import androidx.camera.lifecycle.ProcessCameraProvider;
import androidx.camera.view.PreviewView;
import androidx.core.content.ContextCompat;
import com.google.common.util.concurrent.ListenableFuture;
import java.io.File;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.Manifest;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;

public class CameraAnalysisActivity extends AppCompatActivity {
    private ExecutorService cameraExecutor;
    private PreviewView previewView;
    private ImageView imagePreview;
    private Button captureButton, retakeButton, confirmButton;
    private LinearLayout confirmationLayout;
    private ImageCapture imageCapture;
    private File outputDirectory;
    private Uri savedUri;
    private String mode;
    private String targetLang;
    private static final int REQUEST_PERMISSIONS = 1001;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera_analysis);

        previewView = findViewById(R.id.camera_preview);
        imagePreview = findViewById(R.id.image_preview);
        captureButton = findViewById(R.id.capture_button);
        retakeButton = findViewById(R.id.retake_button);
        confirmButton = findViewById(R.id.confirm_button);
        confirmationLayout = findViewById(R.id.confirmation_layout);
        
        mode = getIntent().getStringExtra("mode");
        targetLang = getIntent().getStringExtra("target_lang");

        outputDirectory = getOutputDirectory();
        cameraExecutor = Executors.newSingleThreadExecutor();

        if (hasAllPermissions()) {
            startCamera();
        } else {
            ActivityCompat.requestPermissions(this, getRequiredPermissions(), REQUEST_PERMISSIONS);
        }

        captureButton.setOnClickListener(v -> takePhoto());
        retakeButton.setOnClickListener(v -> retakePhoto());
        confirmButton.setOnClickListener(v -> confirmPhoto());
    }

    private boolean hasAllPermissions() {
        for (String perm : getRequiredPermissions()) {
            if (ActivityCompat.checkSelfPermission(this, perm) != PackageManager.PERMISSION_GRANTED) {
                return false;
            }
        }
        return true;
    }

    private String[] getRequiredPermissions() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            return new String[]{Manifest.permission.CAMERA};
        } else {
            return new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_PERMISSIONS) {
            if (hasAllPermissions()) {
                startCamera();
            } else {
                Toast.makeText(this, "Camera and storage permissions are required.", Toast.LENGTH_LONG).show();
                FeedbackProvider.speakAndToast(this, "Camera and storage permissions are required.");
                finish();
            }
        }
    }

    private void startCamera() {
        ListenableFuture<ProcessCameraProvider> cameraProviderFuture = ProcessCameraProvider.getInstance(this);
        cameraProviderFuture.addListener(() -> {
            try {
                ProcessCameraProvider cameraProvider = cameraProviderFuture.get();
                Preview preview = new Preview.Builder().build();
                preview.setSurfaceProvider(previewView.getSurfaceProvider());
                imageCapture = new ImageCapture.Builder().build();
                CameraSelector cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA;
                cameraProvider.unbindAll();
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture);
            } catch (Exception e) {
                Toast.makeText(this, "Failed to start camera.", Toast.LENGTH_SHORT).show();
            }
        }, ContextCompat.getMainExecutor(this));
    }

    private void takePhoto() {
        File photoFile = new File(outputDirectory, "photo.jpg");
        ImageCapture.OutputFileOptions outputOptions = new ImageCapture.OutputFileOptions.Builder(photoFile).build();

        imageCapture.takePicture(outputOptions, cameraExecutor, new ImageCapture.OnImageSavedCallback() {
            @Override
            public void onImageSaved(@NonNull ImageCapture.OutputFileResults outputFileResults) {
                savedUri = Uri.fromFile(photoFile);
                runOnUiThread(() -> {
                    previewView.setVisibility(View.GONE);
                    imagePreview.setVisibility(View.VISIBLE);
                    imagePreview.setImageURI(savedUri);
                    captureButton.setVisibility(View.GONE);
                    confirmationLayout.setVisibility(View.VISIBLE);
                });
            }
            @Override
            public void onError(@NonNull ImageCaptureException exception) {
                runOnUiThread(() -> Toast.makeText(CameraAnalysisActivity.this, "Failed to take picture.", Toast.LENGTH_SHORT).show());
            }
        });
    }

    private void retakePhoto() {
        imagePreview.setVisibility(View.GONE);
        previewView.setVisibility(View.VISIBLE);
        confirmationLayout.setVisibility(View.GONE);
        captureButton.setVisibility(View.VISIBLE);
    }

    private void confirmPhoto() {
        if ("analyze".equals(mode)) {
            FeedbackProvider.speakAndToast(this, "Analyzing image...");
            Intent intent = new Intent(this, GeminiAnalysisService.class);
            intent.putExtra("image_path", new File(outputDirectory, "photo.jpg").getAbsolutePath());
            startService(intent);
        } else if ("translate".equals(mode)) {
            FeedbackProvider.speakAndToast(this, "Recognizing text to translate...");
            Intent intent = new Intent(this, CameraTranslateService.class);
            intent.putExtra("image_path", new File(outputDirectory, "photo.jpg").getAbsolutePath());
            intent.putExtra("target_lang", targetLang);
            startService(intent);
        }
        finish();
    }
    
    private File getOutputDirectory() {
        File mediaDir = getExternalMediaDirs()[0];
        File appDir = new File(mediaDir, "Sara");
        if (!appDir.exists()) {
            appDir.mkdir();
        }
        return appDir;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        cameraExecutor.shutdown();
    }
} 