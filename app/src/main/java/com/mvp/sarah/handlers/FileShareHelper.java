package com.mvp.sarah.handlers;

import android.app.Activity;
import android.content.ClipData;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.Uri;
import android.net.wifi.WifiManager;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;
import android.speech.tts.TextToSpeech;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.qrcode.QRCodeWriter;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import fi.iki.elonen.NanoHTTPD;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class FileShareHelper {
    private static final int PORT = 8080;
    private NanoHTTPD server;
    private TextToSpeech tts;
    private final ArrayList<Uri> fileUris = new ArrayList<>();
    private final ArrayList<String> fileNames = new ArrayList<>();
    private final Map<String, Uri> nameToUri = new HashMap<>();

    public void pickFiles(Activity activity, ActivityResultLauncher<Intent> launcher) {
        Log.d("FileShareHelper", "Launching multi-file picker");
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("*/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        launcher.launch(intent);
    }

    public void handleFilePickerResult(Context context, Intent data) {
        fileUris.clear();
        fileNames.clear();
        nameToUri.clear();
        if (data == null) {
            Toast.makeText(context, "No files selected.", Toast.LENGTH_LONG).show();
            return;
        }
        if (data.getClipData() != null) {
            ClipData clipData = data.getClipData();
            for (int i = 0; i < clipData.getItemCount(); i++) {
                Uri uri = clipData.getItemAt(i).getUri();
                String name = getFileName(context, uri);
                fileUris.add(uri);
                fileNames.add(name);
                nameToUri.put(name, uri);
            }
        } else if (data.getData() != null) {
            Uri uri = data.getData();
            String name = getFileName(context, uri);
            fileUris.add(uri);
            fileNames.add(name);
            nameToUri.put(name, uri);
        }
    }

    public void startFileServer(Context context, ImageView qrImageView) {
        Log.d("FileShareHelper", "startFileServer called with files: " + fileNames);
        stopServer(); // Stop any previous server

        if (!isWifiOrHotspotConnected(context)) {
            Toast.makeText(context, "You are not connected to Wi-Fi or Hotspot. Please connect to the receiver's hotspot/WLAN to proceed with transfer.", Toast.LENGTH_LONG).show();
            Log.e("FileShareHelper", "Not connected to Wi-Fi or Hotspot");
            return;
        }

        if (fileUris.isEmpty() || fileNames.isEmpty()) {
            Toast.makeText(context, "No files selected to share.", Toast.LENGTH_LONG).show();
            Log.e("FileShareHelper", "No files selected to share");
            return;
        }

        server = new NanoHTTPD(PORT) {
            @Override
            public NanoHTTPD.Response serve(NanoHTTPD.IHTTPSession session) {
                String uri = session.getUri();
                Map<String, String> params = session.getParms();
                if ("/download".equals(uri) && params.containsKey("name")) {
                    String reqName = params.get("name");
                    Uri fileUri = nameToUri.get(reqName);
                    if (fileUri == null) {
                        Log.e("FileShareHelper", "Requested file not found: " + reqName);
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "File not found");
                    }
                    try {
                        ParcelFileDescriptor pfd = context.getContentResolver().openFileDescriptor(fileUri, "r");
                        if (pfd == null) {
                            Log.e("FileShareHelper", "File not found for uri: " + fileUri);
                            return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "File not found");
                        }
                        FileInputStream fis = new FileInputStream(pfd.getFileDescriptor());
                        NanoHTTPD.Response res = NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "application/octet-stream", fis, pfd.getStatSize());
                        res.addHeader("Content-Disposition", "attachment; filename=\"" + reqName + "\"");
                        Log.d("FileShareHelper", "Serving file: " + reqName);
                        return res;
                    } catch (Exception e) {
                        Log.e("FileShareHelper", "Error serving file: " + e.getMessage());
                        return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.INTERNAL_ERROR, "text/plain", "Error: " + e.getMessage());
                    }
                } else if ("/".equals(uri)) {
                    // Serve index page
                    StringBuilder html = new StringBuilder();
                    html.append("<!DOCTYPE html><html><head><meta name='viewport' content='width=device-width, initial-scale=1'><title>File Share</title>");
                    html.append("<style>body{font-family:sans-serif;background:#f7f7f7;margin:0;padding:0;}h1{color:#ff6600;text-align:center;}ul{list-style:none;padding:0;}li{margin:16px 0;display:flex;align-items:center;opacity:0;animation:fadeIn 0.8s forwards;}li:nth-child(n){animation-delay:calc(0.1s * var(--i));}a{display:inline-block;padding:12px 24px;background:#ff6600;color:#fff;text-decoration:none;border-radius:8px;transition:background 0.2s;margin-left:12px;}a:hover{background:#ff8800;}div.container{max-width:400px;margin:40px auto;background:#fff;padding:24px 16px 32px 16px;border-radius:16px;box-shadow:0 2px 16px rgba(0,0,0,0.08);}footer{text-align:center;color:#aaa;margin-top:32px;font-size:14px;}.file-icon{font-size:28px;vertical-align:middle;animation:bounce 1.2s cubic-bezier(.68,-0.55,.27,1.55) 1;}@keyframes bounce{0%{transform:translateY(-30px);}50%{transform:translateY(10px);}70%{transform:translateY(-5px);}100%{transform:translateY(0);}}@keyframes fadeIn{to{opacity:1;}}</style>");
                    html.append("</head><body><div class='container'><h1>Shared Files</h1><ul>");
                    int i = 1;
                    for (String name : fileNames) {
                        html.append("<li style='--i:").append(i).append("'><span class='file-icon'>📄</span><a href='download?name=").append(name).append("'>").append(name).append("</a></li>");
                        i++;
                    }
                    html.append("</ul></div><footer>Powered by HeySara - File Share</footer></body></html>");
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.OK, "text/html", html.toString());
                } else {
                    return NanoHTTPD.newFixedLengthResponse(NanoHTTPD.Response.Status.NOT_FOUND, "text/plain", "Not found");
                }
            }
        };

        try {
            server.start();
            String ip = getDeviceIpAddress();
            String url = "http://" + ip + ":" + PORT + "/";
            Log.d("FileShareHelper", "Server started at: " + url);
            Bitmap qr = generateQrCode(url);
            if (qr != null) {
                qrImageView.setImageBitmap(qr);
                Log.d("FileShareHelper", "QR code generated and set to ImageView");
            } else {
                Toast.makeText(context, "Failed to generate QR code.", Toast.LENGTH_LONG).show();
                Log.e("FileShareHelper", "Failed to generate QR code for url: " + url);
            }
            speak(context, "File server started. Scan the QR code to download your files.");
            Toast.makeText(context, "File server started at: " + url, Toast.LENGTH_LONG).show();
        } catch (IOException e) {
            Toast.makeText(context, "Failed to start server: " + e.getMessage(), Toast.LENGTH_LONG).show();
            Log.e("FileShareHelper", "Failed to start server: " + e.getMessage());
        }
    }

    public void stopServer() {
        if (server != null) {
            server.stop();
            server = null;
        }
    }

    private String getDeviceIpAddress() {
        try {
            for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
                NetworkInterface intf = en.nextElement();
                for (Enumeration<InetAddress> enumIpAddr = intf.getInetAddresses(); enumIpAddr.hasMoreElements();) {
                    InetAddress inetAddress = enumIpAddr.nextElement();
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

    private Bitmap generateQrCode(String text) {
        try {
            QRCodeWriter writer = new QRCodeWriter();
            BarcodeEncoder encoder = new BarcodeEncoder();
            return encoder.createBitmap(writer.encode(text, BarcodeFormat.QR_CODE, 600, 600));
        } catch (WriterException e) {
            return null;
        }
    }

    private void speak(Context context, String message) {
        if (tts == null) {
            tts = new TextToSpeech(context.getApplicationContext(), status -> {
                if (status == TextToSpeech.SUCCESS) {
                    tts.setLanguage(Locale.getDefault());
                    tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
                }
            });
        } else {
            tts.speak(message, TextToSpeech.QUEUE_FLUSH, null, null);
        }
    }

    public String getFileName(Context context, Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (android.database.Cursor cursor = context.getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    result = cursor.getString(cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }

    private boolean isWifiOrHotspotConnected(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm != null) {
            Network activeNetwork = cm.getActiveNetwork();
            if (activeNetwork != null) {
                NetworkCapabilities nc = cm.getNetworkCapabilities(activeNetwork);
                if (nc != null) {
                    if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                        return true;
                    }
                    // Some devices may use TRANSPORT_WIFI for hotspot as well
                }
            }
        }
        // Try to check if hotspot is enabled (best effort, not always possible)
        try {
            WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            if (wifiManager != null) {
                // For Android O+ use reflection (not officially supported)
                int apState = (int) wifiManager.getClass().getMethod("getWifiApState").invoke(wifiManager);
                // 13 = WIFI_AP_STATE_ENABLED
                if (apState == 13) {
                    return true;
                }
            }
        } catch (Exception e) {
            Log.w("FileShareHelper", "Could not check hotspot state: " + e.getMessage());
        }
        return false;
    }

    public ArrayList<Uri> getFileUris() {
        return new ArrayList<>(fileUris);
    }
    public ArrayList<String> getFileNames() {
        return new ArrayList<>(fileNames);
    }
} 