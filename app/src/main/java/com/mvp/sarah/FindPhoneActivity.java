package com.mvp.sarah;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.widget.TextView;

public class FindPhoneActivity extends Activity {
    private MediaPlayer mediaPlayer;
    private Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView tv = new TextView(this);
        tv.setText("Phone is ringing! Tap anywhere or press a volume button to stop.");
        tv.setTextSize(24);
        tv.setPadding(40, 200, 40, 200);
        setContentView(tv);

        // Set ringer volume to max
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_RING);
        audioManager.setStreamVolume(AudioManager.STREAM_RING, maxVolume, AudioManager.FLAG_SHOW_UI);

        // Play default ringtone
        mediaPlayer = MediaPlayer.create(this, Settings.System.DEFAULT_RINGTONE_URI);
        mediaPlayer.setLooping(true);
        mediaPlayer.start();

        // Auto-stop after 30 seconds
        handler.postDelayed(this::stopRinging, 30000);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        stopRinging();
        return super.onTouchEvent(event);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_VOLUME_UP || keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
            stopRinging();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    private void stopRinging() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        handler.removeCallbacksAndMessages(null);
        finish();
    }

    @Override
    protected void onDestroy() {
        stopRinging();
        super.onDestroy();
    }
}
