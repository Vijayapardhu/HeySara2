package com.mvp.sarah;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.ParcelFileDescriptor;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import fi.iki.elonen.NanoHTTPD;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import java.net.BindException;
import android.os.Environment;
import android.content.pm.PackageManager;

public class FileShareService extends Service {
    public static final String ACTION_STOP = "com.mvp.sarah.action.STOP_FILE_SHARE";
    public static final String ACTION_SHARE_MORE = "com.mvp.sarah.action.SHARE_MORE_FILES";
    public static final String ACTION_START = "com.mvp.sarah.action.START_FILE_SHARE";
    public static final String EXTRA_URIS = "uris";
    public static final String EXTRA_NAMES = "names";
    public static final String CHANNEL_ID = "file_share_channel";
    public static final int NOTIF_ID = 2024;
    private NanoHTTPD server;
    private final ArrayList<Uri> fileUris = new ArrayList<>();
    private final ArrayList<String> fileNames = new ArrayList<>();
    private final Map<String, Uri> nameToUri = new HashMap<>();
    private String serverUrl = "";
    private final ArrayList<File> receivedFiles = new ArrayList<>();
    private static final int PORT_START = 8080;
    private static final int PORT_END = 8090;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            String action = intent.getAction();
            if (ACTION_STOP.equals(action)) {
                stopSelf();
                return START_NOT_STICKY;
            } else if (ACTION_SHARE_MORE.equals(action)) {
                // Launch MainActivity to pick more files
                Intent pickIntent = new Intent(this, MainActivity.class);
                pickIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                pickIntent.putExtra("share_file", true);
                startActivity(pickIntent);
                return START_STICKY;
            } else if (ACTION_START.equals(action)) {
                // Get files from intent
                fileUris.clear();
                fileNames.clear();
                nameToUri.clear();
                ArrayList<String> uriStrings = intent.getStringArrayListExtra(EXTRA_URIS);
                ArrayList<String> names = intent.getStringArrayListExtra(EXTRA_NAMES);
                if (uriStrings != null && names != null && uriStrings.size() == names.size()) {
                    for (int i = 0; i < uriStrings.size(); i++) {
                        Uri uri = Uri.parse(uriStrings.get(i));
                        String name = names.get(i);
                        fileUris.add(uri);
                        fileNames.add(name);
                        nameToUri.put(name, uri);
                    }
                    startServer();
                } else {
                    Toast.makeText(this, "No files to share.", Toast.LENGTH_LONG).show();
                    stopSelf();
                }
            }
        }
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        stopServer();
        showDisconnectedNotification();
        super.onDestroy();
    }

    private void startServer() {
        stopServer();
        createNotificationChannel(); // Ensure channel exists before notification
        showNotification(); // Call startForeground() immediately
        int port = PORT_START;
        boolean started = false;
        while (port <= PORT_END && !started) {
            try {
                server = new NanoHTTPD(port) {
            @Override
            public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
                String uri = session.getUri();
                Map<String, String> params = session.getParms();
                        if (NanoHTTPD.Method.POST.equals(session.getMethod())) {
                            // Handle file upload
                            Map<String, String> files = new HashMap<>();
                            try {
                                session.parseBody(files);
                                String tmpFilePath = files.get("file");
                                String fileName = session.getParms().get("file");
                                if (tmpFilePath != null && fileName != null) {
                                    File uploadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS); // Store in Downloads
                                    if (!uploadDir.exists()) uploadDir.mkdirs();
                                    File dest = new File(uploadDir, fileName);
                                    try (java.io.InputStream in = new java.io.FileInputStream(tmpFilePath);
                                         java.io.OutputStream out = new java.io.FileOutputStream(dest)) {
                                        byte[] buf = new byte[1048576]; // 1 MB buffer for high speed
                                        long total = new File(tmpFilePath).length();
                                        long copied = 0;
                                        int len;
                                        int lastProgress = 0;
                                        while ((len = in.read(buf)) > 0) {
                                            out.write(buf, 0, len);
                                            copied += len;
                                            int progress = (int) (copied * 100 / total);
                                            if (progress != lastProgress) {
                                                sendProgress("upload", fileName, progress, false);
                                                lastProgress = progress;
                                            }
                                        }
                                        sendProgress("upload", fileName, 100, true);
                                    }
                                    receivedFiles.add(dest);
                                    return NanoHTTPD.newFixedLengthResponse("File uploaded successfully: " + fileName + "<br><a href='/'>Back</a>");
                                } else {
                                    return NanoHTTPD.newFixedLengthResponse("Upload failed: file missing");
                                }
                            } catch (Exception e) {
                                return NanoHTTPD.newFixedLengthResponse("Upload failed: " + e.getMessage() + "<br><a href='/'>Back</a>");
                            }
                        } else if ("/download_received".equals(uri) && params.containsKey("name")) {
                            // Serve received file for download
                            String reqName = params.get("name");
                            File uploadDir = new File(getExternalFilesDir(null), "ReceivedFiles");
                            File file = new File(uploadDir, reqName);
                            if (!file.exists()) {
                                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "File not found");
                            }
                            try {
                                FileInputStream fis = new FileInputStream(file);
                                java.io.PipedOutputStream pos = new java.io.PipedOutputStream();
                                java.io.PipedInputStream pis = new java.io.PipedInputStream(pos);
                                new Thread(() -> {
                                    try (FileInputStream fis2 = new FileInputStream(file)) {
                                        byte[] buf = new byte[1048576]; // 1 MB buffer for high speed
                                        long total = file.length();
                                        long sent = 0;
                                        int len;
                                        int lastProgress = 0;
                                        while ((len = fis2.read(buf)) > 0) {
                                            pos.write(buf, 0, len);
                                            sent += len;
                                            int progress = (int) (sent * 100 / total);
                                            if (progress != lastProgress) {
                                                sendProgress("download", file.getName(), progress, false);
                                                lastProgress = progress;
                                            }
                                        }
                                        sendProgress("download", file.getName(), 100, true);
                                        pos.close();
                                    } catch (Exception e) { try { pos.close(); } catch (Exception ignore) {} }
                                }).start();
                                NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream", pis, file.length());
                                res.addHeader("Content-Disposition", "attachment; filename=\"" + reqName + "\"");
                                return res;
                            } catch (Exception e) {
                                return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
                            }
                        } else if ("/".equals(uri)) {
                            // Serve index page
                            StringBuilder html = new StringBuilder();
                            html.append("<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'>");
                            html.append("<title>Sara File Share</title>");
                            html.append("<style>");
                            html.append("body{background:#fff;font-family:sans-serif;margin:0;padding:0;}");
                            html.append(".container{display:flex;flex-direction:column;align-items:center;justify-content:center;min-height:100vh;padding:24px;}");
                            html.append(".button{display:inline-block;padding:12px 32px;margin:8px 0;background:#ff9800;color:#fff;border:none;border-radius:32px;font-size:18px;cursor:pointer;box-shadow:0 2px 8px rgba(0,0,0,0.08);transition:background 0.2s;}");
                            html.append(".button:hover{background:#ffb74d;}");
                            html.append("h1{color:#ff9800;margin-bottom:16px;}");
                            html.append("ul{list-style:none;padding:0;margin:0 0 24px 0;width:100%;max-width:400px;}");
                            html.append("li{margin:12px 0;display:flex;align-items:center;}");
                            html.append("a{color:#ff9800;text-decoration:none;font-weight:bold;}");
                            html.append("a:hover{text-decoration:underline;}");
                            html.append(".section-title{margin-top:32px;margin-bottom:8px;color:#ff9800;font-size:20px;}");
                            html.append(".progress-container{width:100%;max-width:400px;margin:12px 0;}");
                            html.append(".progress-bar{width:0;height:16px;background:#ff9800;border-radius:8px;transition:width 0.3s;}");
                            html.append(".progress-label{font-size:14px;color:#888;margin-bottom:4px;}");
                            html.append("</style></head><body>");
                            // Before generating the HTML, get the device name
                            String deviceName = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
                            html.append("<div class='container'>");
                            html.append("<h1>Sara File Share</h1>");
                            html.append("<div style='margin-bottom:16px;font-size:16px;color:#2196f3;font-weight:bold;'>This device: " + deviceName + "</div>");
                            html.append("<form id='uploadForm' method='POST' enctype='multipart/form-data' onsubmit='return startUpload();'>");
                            html.append("<input id='fileInput' type='file' name='file' required style='margin-bottom:12px;'>");
                            html.append("<br><button class='button' type='submit'>Upload to this device</button>");
                            html.append("</form>");
                            // Upload Progress Section
                            html.append("<div class='progress-section'><div style='display:flex;align-items:center;gap:8px;'><span style='font-size:20px;'>⬆️</span><span style='font-weight:bold;color:#ff9800;'>Upload Progress</span></div><div style='font-size:13px;color:#888;margin-bottom:4px;'>Shows the progress of your file uploads to this device.</div><div class='progress-container'><div id='uploadProgressLabel' class='progress-label'></div><div id='uploadProgressBar' class='progress-bar upload-bar'></div></div></div>");
                            html.append("<div>");
                            html.append("<button class='button' onclick=\"window.location.href='?sharemore=1'\">Share More Files</button>");
                            html.append("</div>");
                            // Shared files
                            html.append("<div class='section-title'>Shared Files</div><ul>");
                            int i = 1;
                            for (String name : fileNames) {
                                Uri fileUri = nameToUri.get(name);
                                long size = 0;
                                if (fileUri != null) {
                                    try (ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(fileUri, "r")) {
                                        if (pfd != null) size = pfd.getStatSize();
                                    } catch (Exception ignore) {}
                                }
                                html.append("<li><span style='font-size:24px;margin-right:8px;'>📄</span><a href='download?name=")
                                    .append(name).append("' download='").append(name).append("' onclick='return startDownload(\"").append(name).append("\")'>").append(name).append("</a>");
                                if (size > 0) html.append(" <span style='color:#888;font-size:13px;'>[" + formatSize(size) + "]</span>");
                                html.append("</li>");
                            }
                            html.append("</ul>");
                            // Received files
                            File uploadDir = new File(getExternalFilesDir(null), "ReceivedFiles");
                            File[] received = uploadDir.listFiles();
                            if (received != null && received.length > 0) {
                                html.append("<div class='section-title'>Received Files</div><ul>");
                                for (File f : received) {
                                    long size = f.length();
                                    html.append("<li><span style='font-size:24px;margin-right:8px;'>⬇️</span><a href='download_received?name=")
                                        .append(f.getName()).append("' download='").append(f.getName()).append("' onclick='return startDownload(\"").append(f.getName()).append("\")'>").append(f.getName()).append("</a>");
                                    if (size > 0) html.append(" <span style='color:#888;font-size:13px;'>[" + formatSize(size) + "]</span>");
                                    html.append("</li>");
                                }
                                html.append("</ul>");
                            }
                            // Download Progress Section
                            html.append("<div class='progress-section'><div style='display:flex;align-items:center;gap:8px;'><span style='font-size:20px;'>⬇️</span><span style='font-weight:bold;color:#2196f3;'>Download Progress</span></div><div style='font-size:13px;color:#888;margin-bottom:4px;'>Shows the progress of your file downloads from this device.</div><div class='progress-container'><div id='downloadProgressLabel' class='progress-label'></div><div id='downloadProgressBar' class='progress-bar download-bar'></div></div></div>");
                            html.append("</div><footer style='margin-top:32px;color:#aaa;font-size:14px;'>Powered by HeySara File Share</footer>");
                            html.append("</div>");
                            // Add JavaScript for upload and download progress
                            html.append("<script>\n" +
                                    "function formatSpeed(bytesPerSec) {\n" +
                                    "  if (bytesPerSec < 1024) return bytesPerSec + ' B/s';\n" +
                                    "  var kb = bytesPerSec / 1024;\n" +
                                    "  if (kb < 1024) return kb.toFixed(1) + ' KB/s';\n" +
                                    "  var mb = kb / 1024;\n" +
                                    "  return mb.toFixed(2) + ' MB/s';\n" +
                                    "}\n" +
                                    "function startUpload() {\n" +
                                    "  var form = document.getElementById('uploadForm');\n" +
                                    "  var fileInput = document.getElementById('fileInput');\n" +
                                    "  var progressBar = document.getElementById('uploadProgressBar');\n" +
                                    "  var progressLabel = document.getElementById('uploadProgressLabel');\n" +
                                    "  if (!fileInput.files.length) return true;\n" +
                                    "  var xhr = new XMLHttpRequest();\n" +
                                    "  xhr.open('POST', '/', true);\n" +
                                    "  var startTime = Date.now();\n" +
                                    "  var lastLoaded = 0;\n" +
                                    "  xhr.upload.onprogress = function(e) {\n" +
                                    "    if (e.lengthComputable) {\n" +
                                    "      var percent = Math.round((e.loaded / e.total) * 100);\n" +
                                    "      var now = Date.now();\n" +
                                    "      var elapsed = (now - startTime) / 1000;\n" +
                                    "      var speed = elapsed > 0 ? (e.loaded / elapsed) : 0;\n" +
                                    "      progressBar.style.width = percent + '%';\n" +
                                    "      progressLabel.textContent = 'Uploading: ' + percent + '% (' + formatSpeed(speed) + ')';\n" +
                                    "    }\n" +
                                    "  };\n" +
                                    "  xhr.onload = function() {\n" +
                                    "    progressBar.style.width = '0%';\n" +
                                    "    progressLabel.textContent = xhr.status == 200 ? 'Upload complete!' : 'Upload failed.';\n" +
                                    "    if (xhr.status == 200) setTimeout(function(){ location.reload(); }, 3000);\n" +
                                    "  };\n" +
                                    "  var formData = new FormData(form);\n" +
                                    "  xhr.send(formData);\n" +
                                    "  return false;\n" +
                                    "}\n" +
                                    "function startDownload(name) {\n" +
                                    "  var progressBar = document.getElementById('downloadProgressBar');\n" +
                                    "  var progressLabel = document.getElementById('downloadProgressLabel');\n" +
                                    "  progressBar.style.width = '0%';\n" +
                                    "  progressLabel.textContent = 'Downloading: 0%';\n" +
                                    "  var xhr = new XMLHttpRequest();\n" +
                                    "  xhr.open('GET', 'download?name=' + encodeURIComponent(name), true);\n" +
                                    "  xhr.responseType = 'blob';\n" +
                                    "  var startTime = Date.now();\n" +
                                    "  xhr.onprogress = function(e) {\n" +
                                    "    if (e.lengthComputable) {\n" +
                                    "      var percent = Math.round((e.loaded / e.total) * 100);\n" +
                                    "      var now = Date.now();\n" +
                                    "      var elapsed = (now - startTime) / 1000;\n" +
                                    "      var speed = elapsed > 0 ? (e.loaded / elapsed) : 0;\n" +
                                    "      progressBar.style.width = percent + '%';\n" +
                                    "      progressLabel.textContent = 'Downloading: ' + percent + '% (' + formatSpeed(speed) + ')';\n" +
                                    "    }\n" +
                                    "  };\n" +
                                    "  xhr.onload = function() {\n" +
                                    "    if (xhr.status == 200) {\n" +
                                    "      var url = window.URL.createObjectURL(xhr.response);\n" +
                                    "      var a = document.createElement('a');\n" +
                                    "      a.href = url;\n" +
                                    "      a.download = name;\n" +
                                    "      document.body.appendChild(a);\n" +
                                    "      a.click();\n" +
                                    "      setTimeout(function(){ document.body.removeChild(a); window.URL.revokeObjectURL(url); }, 100);\n" +
                                    "      progressLabel.textContent = 'Download complete!';\n" +
                                    "      setTimeout(function(){ progressBar.style.width = '0%'; progressLabel.textContent = ''; }, 3000);\n" +
                                    "    } else {\n" +
                                    "      progressLabel.textContent = 'Download failed.';\n" +
                                    "      setTimeout(function(){ progressBar.style.width = '0%'; progressLabel.textContent = ''; }, 3000);\n" +
                                    "    }\n" +
                                    "  };\n" +
                                    "  xhr.send();\n" +
                                    "  return false;\n" +
                                    "}\n" +
                                    "</script>");
                            // Add CSS for colored progress bars
                            html.append("<style>.upload-bar{background:#ff9800;}.download-bar{background:#2196f3;}</style>");
                            html.append("</body></html>");
                            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", html.toString());
                        } else if ("/download".equals(uri) && params.containsKey("name")) {
                    String reqName = params.get("name");
                    Uri fileUri = nameToUri.get(reqName);
                    if (fileUri == null) {
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "File not found");
                    }
                    try {
                        ParcelFileDescriptor pfd = getContentResolver().openFileDescriptor(fileUri, "r");
                        if (pfd == null) return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "File not found");
                                java.io.PipedOutputStream pos2 = new java.io.PipedOutputStream();
                                java.io.PipedInputStream pis2 = new java.io.PipedInputStream(pos2);
                                new Thread(() -> {
                                    try (FileInputStream fis2 = new FileInputStream(pfd.getFileDescriptor())) {
                                        byte[] buf = new byte[1048576]; // 1 MB buffer for high speed
                                        long total = pfd.getStatSize();
                                        long sent = 0;
                                        int len;
                                        int lastProgress = 0;
                                        while ((len = fis2.read(buf)) > 0) {
                                            pos2.write(buf, 0, len);
                                            sent += len;
                                            int progress = (int) (sent * 100 / total);
                                            if (progress != lastProgress) {
                                                sendProgress("download", reqName, progress, false);
                                                lastProgress = progress;
                                            }
                                        }
                                        sendProgress("download", reqName, 100, true);
                                        pos2.close();
                                    } catch (Exception e) { try { pos2.close(); } catch (Exception ignore) {} }
                                }).start();
                                NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream", pis2, pfd.getStatSize());
                        res.addHeader("Content-Disposition", "attachment; filename=\"" + reqName + "\"");
                        showDownloadSuccessNotification(reqName);
                        return res;
                    } catch (Exception e) {
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
                    }
                } else if ("/qr.png".equals(uri)) {
                    // Serve QR code image
                    try {
                        Bitmap qr = generateQrCode(serverUrl);
                        java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
                        qr.compress(Bitmap.CompressFormat.PNG, 100, baos);
                        byte[] bytes = baos.toByteArray();
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "image/png", new java.io.ByteArrayInputStream(bytes), bytes.length);
                    } catch (Exception e) {
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "QR error");
                    }
                } else {
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "Not found");
                }
            }
        };
            server.start();
            String ip = getDeviceIpAddress();
                serverUrl = "http://" + ip + ":" + port + "/";
            Toast.makeText(this, "File server started at: " + serverUrl, Toast.LENGTH_LONG).show();
                // Send broadcast to notify activity to show QR code
                Intent serverStarted = new Intent("com.mvp.sarah.FILE_SHARE_SERVER_STARTED");
                serverStarted.putExtra("serverUrl", serverUrl);
                LocalBroadcastManager.getInstance(this).sendBroadcast(serverStarted);
                started = true;
            } catch (BindException e) {
                port++;
                if (server != null) { try { server.stop(); } catch (Exception ignore) {} server = null; }
        } catch (IOException e) {
            Toast.makeText(this, "Failed to start server: " + e.getMessage(), Toast.LENGTH_LONG).show();
                stopSelf();
                return;
            }
        }
        if (!started) {
            Toast.makeText(this, "File sharing failed: All ports in use (" + PORT_START + "-" + PORT_END + "). Please close other sharing sessions and try again.", Toast.LENGTH_LONG).show();
            stopSelf();
        }
    }

    private void stopServer() {
        if (server != null) {
            try { server.stop(); } catch (Exception ignore) {}
            server = null;
        }
        stopForeground(true);
    }

    private void showNotification() {
        Intent stopIntent = new Intent(this, FileShareService.class);
        stopIntent.setAction(ACTION_STOP);
        PendingIntent stopPending = PendingIntent.getService(this, 1, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent shareMoreIntent = new Intent(this, FileShareService.class);
        shareMoreIntent.setAction(ACTION_SHARE_MORE);
        PendingIntent shareMorePending = PendingIntent.getService(this, 2, shareMoreIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Intent openIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse(serverUrl));
        PendingIntent openPending = PendingIntent.getActivity(this, 3, openIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Sara File Share")
                .setContentText("Tap to open sharing page. Server running at " + serverUrl)
                .setSmallIcon(android.R.drawable.stat_sys_upload)
                .setContentIntent(openPending)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPending)
                .addAction(android.R.drawable.ic_menu_share, "Share More", shareMorePending)
                .setOngoing(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build();
        if (android.os.Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
        } else {
            startForeground(NOTIF_ID, notif);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "File Share", NotificationManager.IMPORTANCE_HIGH);
            channel.setDescription("Sara file sharing server");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) manager.createNotificationChannel(channel);
        }
    }

    private String getDeviceIpAddress() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> en = java.net.NetworkInterface.getNetworkInterfaces();
            while (en.hasMoreElements()) {
                java.net.NetworkInterface intf = en.nextElement();
                java.util.Enumeration<java.net.InetAddress> enumIpAddr = intf.getInetAddresses();
                while (enumIpAddr.hasMoreElements()) {
                    java.net.InetAddress inetAddress = enumIpAddr.nextElement();
                    if (!inetAddress.isLoopbackAddress() && inetAddress.isSiteLocalAddress()) {
                        return inetAddress.getHostAddress();
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return "0.0.0.0";
    }

    private Bitmap generateQrCode(String text) throws WriterException {
        QRCodeWriter writer = new QRCodeWriter();
        BarcodeEncoder encoder = new BarcodeEncoder();
        return encoder.createBitmap(writer.encode(text, BarcodeFormat.QR_CODE, 600, 600));
    }

    private void showDownloadSuccessNotification(String fileName) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("File Downloaded")
                .setContentText(fileName + " was downloaded successfully.")
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        if (android.os.Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void showDisconnectedNotification() {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("File Share Stopped")
                .setContentText("The file sharing server has been stopped or disconnected.")
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH);
        if (android.os.Build.VERSION.SDK_INT < 33 || checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            NotificationManagerCompat.from(this).notify((int) System.currentTimeMillis(), builder.build());
        }
    }

    private void sendProgress(String direction, String fileName, int progress, boolean done) {
        Intent intent = new Intent("com.mvp.sarah.FILE_TRANSFER_PROGRESS");
        intent.putExtra("direction", direction);
        intent.putExtra("fileName", fileName);
        intent.putExtra("progress", progress);
        intent.putExtra("done", done);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    // Helper to format file size
    private static String formatSize(long size) {
        if (size < 1024) return size + " B";
        int exp = (int) (Math.log(size) / Math.log(1024));
        String pre = "KMGTPE".charAt(exp-1) + "";
        return String.format("%.1f %sB", size / Math.pow(1024, exp), pre);
    }
} 