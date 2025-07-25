package com.mvp.sarah;

import android.app.DownloadManager;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.google.firebase.firestore.FirebaseFirestore;
import java.io.File;

public class UpdateCheckWorker extends Worker {
    private static final String CHANNEL_ID = "update_channel";
    private static final int NOTIF_ID_UPDATE = 2001;
    private static final int NOTIF_ID_INSTALL = 2002;
    private static final String APK_FILE_NAME = "Sara.apk";

    public UpdateCheckWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public Result doWork() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        db.collection("app_update").document("latest").get()
            .addOnSuccessListener(doc -> {
                if (doc.exists() && doc.contains("version_code") && doc.contains("apk_url")) {
                    long remoteVersion = doc.getLong("version_code");
                    String apkUrl = doc.getString("apk_url");
                    int currentVersion = BuildConfig.VERSION_CODE;
                    if (remoteVersion > currentVersion) {
                        showUpdateNotification(apkUrl);
                    }
                }
            });
        return Result.success();
    }

    private void showUpdateNotification(String apkUrl) {
        createNotificationChannel();
        Intent downloadIntent = new Intent(getApplicationContext(), UpdateDownloadReceiver.class);
        downloadIntent.putExtra("apk_url", apkUrl);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(getApplicationContext(), 0, downloadIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder builder = new NotificationCompat.Builder(getApplicationContext(), CHANNEL_ID)
            .setContentTitle("Sara update available!")
            .setContentText("Tap to download and install the latest version.")
            .setSmallIcon(R.drawable.ic_download_orange_24dp)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .addAction(R.drawable.ic_download_orange_24dp, "Download", pendingIntent);
        NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
        manager.notify(NOTIF_ID_UPDATE, builder.build());
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "App Updates", NotificationManager.IMPORTANCE_HIGH);
            NotificationManager manager = (NotificationManager) getApplicationContext().getSystemService(Context.NOTIFICATION_SERVICE);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    // BroadcastReceiver to handle download and install
    public static class UpdateDownloadReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String apkUrl = intent.getStringExtra("apk_url");
            DownloadManager.Request request = new DownloadManager.Request(Uri.parse(apkUrl));
            request.setTitle("Sara Update");
            request.setDescription("Downloading update...");
            request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, APK_FILE_NAME);
            request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            long downloadId = manager.enqueue(request);
            // Register receiver for download complete
            context.getApplicationContext().registerReceiver(new BroadcastReceiver() {
                @Override
                public void onReceive(Context ctx, Intent i) {
                    long id = i.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1);
                    if (id == downloadId) {
                        showInstallNotification(ctx);
                    }
                }
            }, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        }
        private void showInstallNotification(Context context) {
            Intent installIntent = new Intent(Intent.ACTION_VIEW);
            File apkFile = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), APK_FILE_NAME);
            Uri apkUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", apkFile);
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive");
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, installIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Sara update downloaded!")
                .setContentText("Tap to install the update.")
                .setSmallIcon(R.drawable.ic_download_orange_24dp)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .addAction(R.drawable.ic_download_orange_24dp, "Install", pendingIntent);
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            manager.notify(NOTIF_ID_INSTALL, builder.build());
        }
    }
} 