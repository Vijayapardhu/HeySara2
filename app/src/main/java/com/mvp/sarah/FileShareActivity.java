
package com.mvp.sarah;

import android.app.Activity;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.view.View;
import android.widget.LinearLayout;
import android.media.MediaPlayer;
import android.animation.ValueAnimator;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.view.ViewGroup;
import java.util.Random;
import android.util.Log;
import android.widget.FrameLayout;
import android.view.LayoutInflater;
import android.graphics.drawable.Drawable;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.LinearLayoutManager;
import java.util.LinkedHashMap;

public class FileShareActivity extends AppCompatActivity {
    public static final String EXTRA_SERVER_URL = "server_url";
    private ImageView qrImageView;
    private Button selectFileButton;
    private Button shareMoreButton;
    private Button disconnectButton;
    private ActivityResultLauncher<Intent> filePickerLauncher;
    // Store selected files
    private final List<Uri> selectedUris = new ArrayList<>();
    private final List<String> selectedNames = new ArrayList<>();
    private RecyclerView fileListRecyclerView;
    private FileTransferAdapter fileTransferAdapter;
    private final LinkedHashMap<String, FileTransferItem> transferItems = new LinkedHashMap<>();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private MediaPlayer currentPlayer;
    private void playSound(int resId) {
        try {
            if (currentPlayer != null) {
                currentPlayer.stop();
                currentPlayer.release();
            }
            currentPlayer = MediaPlayer.create(this, resId);
            if (currentPlayer != null) {
                currentPlayer.setOnCompletionListener(mp -> {
                    mp.release();
                    if (currentPlayer == mp) currentPlayer = null;
                });
                currentPlayer.start();
            }
        } catch (Exception ignore) {}
    }
    private void playSystemError() {
        try {
            MediaPlayer mp = MediaPlayer.create(this, android.provider.Settings.System.DEFAULT_NOTIFICATION_URI);
            if (mp != null) {
                mp.setOnCompletionListener(MediaPlayer::release);
                mp.start();
            }
        } catch (Exception ignore) {}
    }
    private final BroadcastReceiver transferReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String direction = intent.getStringExtra("direction");
            String fileName = intent.getStringExtra("fileName");
            int progress = intent.getIntExtra("progress", 0);
            boolean done = intent.getBooleanExtra("done", false);
            boolean canceled = intent.getBooleanExtra("canceled", false);
            mainHandler.post(() -> {
                updateTransferRow(fileName, direction, progress, done, canceled);
            });
        }
    };
    private ProgressBar serverLoadingSpinner;
    private TextView serverLoadingText;
    private TextView funFactText;
    private final String[] funFacts = new String[] {
        "🎉 Did you know? You can share any file, not just photos!",
        "📱 Tip: Scan the QR code from any device on the same Wi-Fi.",
        "🔒 Sara keeps your transfers private—no cloud, just local!",
        "🚀 Pro tip: You can upload files back to this device from the web page!",
        "🌐 Fun fact: Sara's file sharing works even without the internet!",
        "🤖 Sara loves helping you share with a smile!",
        "✨ Try sharing a folder full of surprises!",
        "🦄 Unleash the magic of hands-free sharing!",
        "🎶 Did you hear the chime? That means you’re ready to share!",
        "🎈 File sharing made fun and easy—just for you!"
    };
    private void showFunFact() {
        int idx = (int) (Math.random() * funFacts.length);
        funFactText.setText(funFacts[idx]);
        funFactText.setVisibility(View.VISIBLE);
    }
    private void playChime() {
        // Play custom QR ready sound (replace with your own if desired)
        playSound(R.raw.sara_beep);
    }
    private void playFileComplete() {
        // Play custom file complete sound (replace with your own if desired)
        playSound(R.raw.sara_beep);
    }
    private void showConfetti() {
        final ViewGroup root = (ViewGroup) findViewById(android.R.id.content);
        final ConfettiView confetti = new ConfettiView(this);
        root.addView(confetti, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        confetti.startConfetti(() -> root.removeView(confetti));
    }
    private static class ConfettiView extends View {
        private static final int NUM_CONFETTI = 32;
        private final Paint paint = new Paint();
        private final float[] x = new float[NUM_CONFETTI];
        private final float[] y = new float[NUM_CONFETTI];
        private final float[] speed = new float[NUM_CONFETTI];
        private final int[] color = new int[NUM_CONFETTI];
        private final Random rand = new Random();
        private ValueAnimator animator;
        public ConfettiView(Context ctx) {
            super(ctx);
            for (int i = 0; i < NUM_CONFETTI; i++) {
                x[i] = rand.nextFloat() * ctx.getResources().getDisplayMetrics().widthPixels;
                y[i] = -rand.nextFloat() * 400;
                speed[i] = 4 + rand.nextFloat() * 6;
                color[i] = Color.rgb(rand.nextInt(256), rand.nextInt(256), rand.nextInt(256));
            }
        }
        public void startConfetti(Runnable onEnd) {
            animator = ValueAnimator.ofFloat(0, 1);
            animator.setDuration(1200);
            animator.addUpdateListener(a -> {
                for (int i = 0; i < NUM_CONFETTI; i++) {
                    y[i] += speed[i];
                }
                invalidate();
            });
            animator.addListener(new android.animation.AnimatorListenerAdapter() {
                @Override public void onAnimationEnd(android.animation.Animator animation) {
                    if (onEnd != null) onEnd.run();
                }
            });
            animator.start();
        }
        @Override
        protected void onDraw(Canvas canvas) {
            super.onDraw(canvas);
            for (int i = 0; i < NUM_CONFETTI; i++) {
                paint.setColor(color[i]);
                canvas.drawCircle(x[i], y[i], 18, paint);
            }
        }
    }
    private final BroadcastReceiver serverStartedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String url = intent.getStringExtra("serverUrl");
            Log.d(TAG, "serverStartedReceiver: received url=" + url);
            if (url != null && !url.isEmpty()) {
                showQrCode(url);
                qrImageView.setAlpha(0f);
                qrImageView.setVisibility(View.VISIBLE);
                qrImageView.animate().alpha(1f).setDuration(600).start();
                serverLoadingSpinner.setVisibility(View.GONE);
                serverLoadingText.setVisibility(View.GONE);
                showFunFact();
                playChime();
                showConfetti();
                Log.d(TAG, "serverStartedReceiver: QR shown, spinner hidden");
            } else {
                Log.d(TAG, "serverStartedReceiver: url missing");
            }
        }
    };

    private static final String TAG = "FileShareActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_share);
        Log.d(TAG, "onCreate: called");

        qrImageView = findViewById(R.id.qrImageView);
        disconnectButton = findViewById(R.id.disconnectButton); // Correct assignment
        shareMoreButton = findViewById(R.id.shareMoreButton);
        fileListRecyclerView = findViewById(R.id.fileListRecyclerView);
        fileListRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        fileTransferAdapter = new FileTransferAdapter(new ArrayList<>());
        fileListRecyclerView.setAdapter(fileTransferAdapter);
        serverLoadingSpinner = findViewById(R.id.serverLoadingSpinner);
        serverLoadingText = findViewById(R.id.serverLoadingText);
        funFactText = findViewById(R.id.funFactText);
        TextView deviceNameText = findViewById(R.id.deviceNameText);
        String deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        deviceNameText.setText("This device: " + deviceName);
        disconnectButton.setOnClickListener(v -> {
            // Stop the file share service and close the activity
            Intent stopIntent = new Intent(this, FileShareService.class);
            stopIntent.setAction(FileShareService.ACTION_STOP);
            startService(stopIntent);
            finish();
        });
        Log.d(TAG, "Views initialized");

        filePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                Log.d(TAG, "filePickerLauncher result: " + result.getResultCode());
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    handleFilePickerResult(result.getData());
                }
            }
        );

        // If launched with files, start the service
        ArrayList<String> uriStrings = getIntent().getStringArrayListExtra("uris");
        ArrayList<String> names = getIntent().getStringArrayListExtra("names");
        if (uriStrings != null && names != null && uriStrings.size() == names.size() && !uriStrings.isEmpty()) {
            Intent serviceIntent = new Intent(this, FileShareService.class);
            serviceIntent.setAction(FileShareService.ACTION_START);
            serviceIntent.putStringArrayListExtra(FileShareService.EXTRA_URIS, uriStrings);
            serviceIntent.putStringArrayListExtra(FileShareService.EXTRA_NAMES, names);
            serviceIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } else {
            // If no files, prompt for selection as before
            selectFiles();
        }

        shareMoreButton.setOnClickListener(v -> selectFiles());
    }

    private void selectFiles() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        filePickerLauncher.launch(intent);
    }

    private void handleFilePickerResult(@Nullable Intent data) {
        Log.d(TAG, "handleFilePickerResult: called");
        if (data == null) {
            playSystemError();
            Log.d(TAG, "handleFilePickerResult: data is null");
            return;
        }
        List<Uri> newUris = new ArrayList<>();
        List<String> newNames = new ArrayList<>();
        if (data.getClipData() != null) {
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                Uri uri = data.getClipData().getItemAt(i).getUri();
                newUris.add(uri);
                newNames.add(getFileName(uri));
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            newUris.add(uri);
            newNames.add(getFileName(uri));
        }
        if (newUris.isEmpty()) {
            Toast.makeText(this, "No files selected", Toast.LENGTH_SHORT).show();
            playSystemError();
            Log.d(TAG, "handleFilePickerResult: no files selected");
            return;
        }
        // Merge new files with existing, avoiding duplicates
        Set<String> uriStringsSet = new HashSet<>();
        for (Uri uri : selectedUris) uriStringsSet.add(uri.toString());
        for (int i = 0; i < newUris.size(); i++) {
            Uri uri = newUris.get(i);
            String uriStr = uri.toString();
            if (!uriStringsSet.contains(uriStr)) {
                selectedUris.add(uri);
                selectedNames.add(newNames.get(i));
                uriStringsSet.add(uriStr);
            }
        }
        // Grant URI permissions for all selected files
        for (Uri uri : selectedUris) {
            grantUriPermission(getPackageName(), uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
        }
        // Start the service with the full list
        Intent serviceIntent = new Intent(this, FileShareService.class);
        serviceIntent.setAction(FileShareService.ACTION_START);
        ArrayList<String> uriStrings = new ArrayList<>();
        for (Uri uri : selectedUris) uriStrings.add(uri.toString());
        serviceIntent.putStringArrayListExtra(FileShareService.EXTRA_URIS, uriStrings);
        serviceIntent.putStringArrayListExtra(FileShareService.EXTRA_NAMES, new ArrayList<>(selectedNames));
        serviceIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(serviceIntent);
        } else {
            startService(serviceIntent);
        }
        // Do NOT finish the activity; wait for server started broadcast
        Log.d(TAG, "handleFilePickerResult: starting service, showing spinner");
        serverLoadingSpinner.setVisibility(View.VISIBLE);
        serverLoadingText.setVisibility(View.VISIBLE);
        qrImageView.setVisibility(View.INVISIBLE);
        funFactText.setVisibility(View.GONE);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) result = cursor.getString(idx);
                }
            }
        }
        if (result == null) {
            String path = uri.getPath();
            int cut = path != null ? path.lastIndexOf('/') : -1;
            if (cut != -1) result = path.substring(cut + 1);
        }
        return result != null ? result : "file";
    }

    private void showQrCode(String serverUrl) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            com.google.zxing.common.BitMatrix bitMatrix = writer.encode(serverUrl, BarcodeFormat.QR_CODE, 600, 600);
            int width = bitMatrix.getWidth();
            int height = bitMatrix.getHeight();
            Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
            int white = 0xFFFFFFFF; // White color
            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    bmp.setPixel(x, y, bitMatrix.get(x, y) ? white : 0x00FFFFFF); // white or transparent
                }
            }
            qrImageView.setImageBitmap(bmp);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    private void updateTransferRow(String fileName, String direction, int progress, boolean done, boolean canceled) {
        FileTransferItem item = transferItems.get(fileName);
        long now = System.currentTimeMillis();
        if (item == null) {
            // Try to get totalBytes for this file
            long totalBytes = 0;
            // For uploads, check received files; for downloads, check shared files
            // (You may need to adjust this logic based on your app's data)
            item = new FileTransferItem(fileName, direction, progress, done, totalBytes);
            item.canceled = canceled;
            transferItems.put(fileName, item);
            fileTransferAdapter.setItems(new ArrayList<>(transferItems.values()));
        } else {
            int delta = progress - item.progress;
            long timeDelta = now - item.lastUpdateTime;
            if (delta > 0 && timeDelta > 0 && item.totalBytes > 0) {
                double bytesTransferred = (delta / 100.0) * item.totalBytes;
                double mbps = (bytesTransferred / (timeDelta / 1000.0)) / (1024 * 1024);
                item.speed = mbps;
            }
            item.progress = progress;
            item.done = done;
            item.direction = direction;
            item.lastUpdateTime = now;
            item.canceled = canceled;
            fileTransferAdapter.notifyItemChanged(new ArrayList<>(transferItems.keySet()).indexOf(fileName));
        }
    }
    private View createTransferRow(String fileName, String direction) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(8, 8, 8, 8);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ImageView icon = new ImageView(this);
        icon.setId(View.generateViewId());
        icon.setLayoutParams(new LinearLayout.LayoutParams(64, 64));
        icon.setImageResource("upload".equals(direction) ? R.drawable.ic_upload_orange_24dp : R.drawable.ic_download_orange_24dp);
        row.addView(icon);
        TextView name = new TextView(this);
        name.setText(fileName);
        name.setTextColor(0xFF333333);
        name.setTextSize(16);
        name.setPadding(16, 0, 8, 0);
        row.addView(name);
        ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setId(View.generateViewId());
        bar.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        bar.setMax(100);
        bar.setProgress(0);
        row.addView(bar);
        TextView status = new TextView(this);
        status.setId(View.generateViewId());
        status.setText("0%");
        status.setTextColor(0xFFFF9800);
        status.setTextSize(15);
        status.setPadding(16, 0, 0, 0);
        row.addView(status);
        return row;
    }

    @Override
    protected void onResume() {
        super.onResume();
        LocalBroadcastManager.getInstance(this).registerReceiver(transferReceiver, new IntentFilter("com.mvp.sarah.FILE_TRANSFER_PROGRESS"));
        LocalBroadcastManager.getInstance(this).registerReceiver(serverStartedReceiver, new IntentFilter("com.mvp.sarah.FILE_SHARE_SERVER_STARTED"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(transferReceiver);
        LocalBroadcastManager.getInstance(this).unregisterReceiver(serverStartedReceiver);
        if (isFinishing()) {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_HOME);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}

// Adapter and item class
class FileTransferItem {
    String fileName;
    String direction;
    int progress;
    boolean done;
    long lastBytes;
    long lastUpdateTime;
    double speed;
    long totalBytes;
    boolean canceled;
    FileTransferItem(String fileName, String direction, int progress, boolean done, long totalBytes) {
        this.fileName = fileName;
        this.direction = direction;
        this.progress = progress;
        this.done = done;
        this.lastBytes = 0;
        this.lastUpdateTime = System.currentTimeMillis();
        this.speed = 0;
        this.totalBytes = totalBytes;
        this.canceled = false;
    }
}
class FileTransferAdapter extends RecyclerView.Adapter<FileTransferAdapter.ViewHolder> {
    private List<FileTransferItem> items;
    FileTransferAdapter(List<FileTransferItem> items) { this.items = items; }
    void setItems(List<FileTransferItem> newItems) {
        this.items = newItems;
        notifyDataSetChanged();
    }
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_file_transfer, parent, false);
        return new ViewHolder(view);
    }
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        FileTransferItem item = items.get(position);
        holder.fileName.setText(item.fileName);
        String statusText;
        if (item.canceled) {
            statusText = "Canceled";
        } else if (item.done) {
            statusText = "Done";
        } else {
            String percentText = ("upload".equals(item.direction) ? "Uploading " : "Downloading ") + item.progress + "%";
            if (item.speed > 0 && item.progress < 100) {
                statusText = percentText + String.format(" (%.2f MB/s)", item.speed);
            } else {
                statusText = percentText;
            }
        }
        holder.fileStatus.setText(statusText);
        holder.fileProgressBar.setProgress(item.progress);
        if (item.done) {
            holder.fileIcon.setImageResource(R.drawable.ic_check_circle_orange_24dp);
        } else if (item.canceled) {
            holder.fileIcon.setImageResource(R.drawable.ic_close);
        } else {
            holder.fileIcon.setImageResource("upload".equals(item.direction) ? R.drawable.ic_upload_orange_24dp : R.drawable.ic_download_orange_24dp);
        }
    }
    @Override
    public int getItemCount() { return items.size(); }
    class ViewHolder extends RecyclerView.ViewHolder {
        ImageView fileIcon;
        TextView fileName, fileStatus;
        ProgressBar fileProgressBar;
        ViewHolder(View itemView) {
            super(itemView);
            fileIcon = itemView.findViewById(R.id.fileIcon);
            fileName = itemView.findViewById(R.id.fileName);
            fileStatus = itemView.findViewById(R.id.fileStatus);
            fileProgressBar = itemView.findViewById(R.id.fileProgressBar);
        }
    }
} 