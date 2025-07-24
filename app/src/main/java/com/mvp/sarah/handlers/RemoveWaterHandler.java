package com.mvp.sarah.handlers;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.os.Build;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;

public class RemoveWaterHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    private static final List<String> COMMANDS = Arrays.asList(
        "remove water",
        "eject water",
        "water out"
    );

    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase();
        return lower.contains("remove water") ||
               lower.contains("eject water") ||
               lower.contains("water out");
    }

    @Override
    public void handle(Context context, String command) {
        FeedbackProvider.speakAndToast(context, "Playing sound to remove water from the speaker.");
        playLowFrequencySound();
    }

    private void playLowFrequencySound() {
        final int durationMs = 10000; // 5 seconds
        final int sampleRate = 44100;
        final double freq = 165.0; // Hz, similar to Apple Watch eject
        final int numSamples = durationMs * sampleRate / 1000;
        final double[] sample = new double[numSamples];
        final byte[] generatedSnd = new byte[2 * numSamples];
        for (int i = 0; i < numSamples; ++i) {
            sample[i] = Math.sin(2 * Math.PI * i / (sampleRate / freq));
        }
        int idx = 0;
        for (final double dVal : sample) {
            final short val = (short) ((dVal * 32767));
            generatedSnd[idx++] = (byte) (val & 0x00ff);
            generatedSnd[idx++] = (byte) ((val & 0xff00) >>> 8);
        }
        final AudioTrack audioTrack = new AudioTrack(
                AudioManager.STREAM_MUSIC,
                sampleRate,
                android.media.AudioFormat.CHANNEL_OUT_MONO,
                android.media.AudioFormat.ENCODING_PCM_16BIT,
                generatedSnd.length,
                AudioTrack.MODE_STATIC);
        audioTrack.write(generatedSnd, 0, generatedSnd.length);
        audioTrack.play();
    }

    @Override
    public List<String> getSuggestions() {
        return COMMANDS;
    }
} 