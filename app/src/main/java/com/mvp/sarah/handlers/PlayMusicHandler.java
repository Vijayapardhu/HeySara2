package com.mvp.sarah.handlers;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.provider.MediaStore;
import android.text.TextUtils;
import com.mvp.sarah.CommandHandler;
import com.mvp.sarah.CommandRegistry;
import com.mvp.sarah.FeedbackProvider;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public class PlayMusicHandler implements CommandHandler, CommandRegistry.SuggestionProvider {
    @Override
    public boolean canHandle(String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        return lower.startsWith("play ") || lower.contains("play song") || lower.contains("play music");
    }

    @Override
    public void handle(Context context, String command) {
        String lower = command.toLowerCase(Locale.ROOT);
        String song = null;
        String app = null;

        // Try to extract app name
        if (lower.contains("on spotify")) {
            app = "spotify";
            song = extractSong(command, "on spotify");
        } else if (lower.contains("in spotify")) {
            app = "spotify";
            song = extractSong(command, "in spotify");
        } else if (lower.contains("on youtube music")) {
            app = "ytmusic";
            song = extractSong(command, "on youtube music");
        } else if (lower.contains("in youtube music")) {
            app = "ytmusic";
            song = extractSong(command, "in youtube music");
        } else if (lower.contains("on youtube")) {
            app = "youtube";
            song = extractSong(command, "on youtube");
        } else if (lower.contains("in youtube")) {
            app = "youtube";
            song = extractSong(command, "in youtube");
        } else if (lower.contains("on music")) {
            app = "default";
            song = extractSong(command, "on music");
        } else if (lower.contains("in music")) {
            app = "default";
            song = extractSong(command, "in music");
        } else if (lower.contains("in amazon music")) {
            app = "amazon music";
            song = extractSong(command, "in amazon music");
        } else if (lower.contains("on amazon music")) {
            app = "amazon music";
            song = extractSong(command, "on amazon music");
        } else if (lower.contains("on amazon")) {
            app = "amazon music";
            song = extractSong(command, "on amazon");
        } else if (lower.contains("in amazon")) {
            app = "amazon music";
            song = extractSong(command, "in amazon");
        } else if (lower.contains("on apple music")) {
            app = "apple music";
            song = extractSong(command, "on apple music");
        } else if (lower.contains("in apple music")) {
            app = "apple music";
            song = extractSong(command, "in apple music");
        } else if (lower.contains("on apple")) {
            app = "apple music";
            song = extractSong(command, "on apple");
        } else if (lower.contains("in apple")) {
            app = "apple music";
            song = extractSong(command, "in apple");
        } else {
            // Default: try to extract after "play"
            int idx = lower.indexOf("play ");
            if (idx != -1) {
                song = command.substring(idx + 5).trim();
            }
        }

        if (TextUtils.isEmpty(song)) {
            FeedbackProvider.speakAndToast(context, "What song or artist do you want to play?");
            return;
        }

        if (app == null || app.equals("default")) {
            // Try YouTube (with accessibility automation) first, then Spotify if not installed, then fallback
            if (isAppInstalled(context, "com.google.android.youtube")) {
                launchYouTube(context, song);
                return;
            } else if (isAppInstalled(context, "com.spotify.music")) {
                launchSpotify(context, song);
                return;
            } else {
                // Try default music player as last resort
                Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
                intent.putExtra(SearchManager.QUERY, song);
                try {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                    context.startActivity(intent);
                    FeedbackProvider.speakAndToast(context, "Playing " + song + ".");
                    return;
                } catch (Exception e) {
                    FeedbackProvider.speakAndToast(context, "Couldn't find YouTube or Spotify to play your song.");
                    return;
                }
            }
        } else if (app.equals("spotify")) {
            if (isAppInstalled(context, "com.spotify.music")) {
                launchSpotify(context, song);
            } else {
                FeedbackProvider.speakAndToast(context, "Spotify is not installed.");
            }
        } else if (app.equals("ytmusic")) {
            if (isAppInstalled(context, "com.google.android.apps.youtube.music")) {
                launchYouTubeMusic(context, song);
            } else {
                FeedbackProvider.speakAndToast(context, "YouTube Music is not installed.");
            }
        } else if (app.equals("youtube")) {
            if (isAppInstalled(context, "com.google.android.youtube")) {
                launchYouTube(context, song);
            } else {
                FeedbackProvider.speakAndToast(context, "YouTube is not installed.");
            }
        } else {
            FeedbackProvider.speakAndToast(context, "Sorry, I don't know how to play music in that app yet.");
        }
    }

    private String extractSong(String command, String keyword) {
        int idx = command.toLowerCase(Locale.ROOT).indexOf("play ");
        if (idx != -1) {
            String afterPlay = command.substring(idx + 5);
            int appIdx = afterPlay.toLowerCase(Locale.ROOT).indexOf(keyword);
            if (appIdx != -1) {
                return afterPlay.substring(0, appIdx).trim();
            } else {
                return afterPlay.trim();
            }
        }
        return null;
    }

    private boolean isAppInstalled(Context context, String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private void launchSpotify(Context context, String song) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(Uri.parse("spotify:search:" + Uri.encode(song)));
            intent.putExtra(Intent.EXTRA_REFERRER, Uri.parse("android-app://" + context.getPackageName()));
            intent.setPackage("com.spotify.music");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Searching for " + song + " in Spotify.");
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Couldn't open Spotify.");
        }
    }

    private void launchYouTube(Context context, String song) {
        try {
            Intent intent = new Intent(Intent.ACTION_SEARCH);
            intent.setPackage("com.google.android.youtube");
            intent.putExtra("query", song);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            context.startActivity(intent);
            FeedbackProvider.speakAndToast(context, "Searching for " + song + " on YouTube.");

            // --- Accessibility automation: type song name and click play, but do NOT click the search button (to avoid microphone) ---
            new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                // 1. Type the song name (after a delay)
                Intent typeSong = new Intent("com.mvp.sarah.ACTION_TYPE_TEXT");
                typeSong.putExtra("text", song);
                context.sendBroadcast(typeSong);

                // 2. Click the top result's play button (after another delay)
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    Intent clickPlay = new Intent("com.mvp.sarah.ACTION_CLICK_LABEL");
                    clickPlay.putExtra("com.mvp.sarah.EXTRA_LABEL", "Play");
                    context.sendBroadcast(clickPlay);
                }, 2000);
            }, 2500);
            // --- End accessibility automation ---
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Couldn't open YouTube.");
        }
    }

    private void launchYouTubeMusic(Context context, String song) {
        try {
            Intent intent = context.getPackageManager().getLaunchIntentForPackage("com.google.android.apps.youtube.music");
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

                context.startActivity(intent);
                FeedbackProvider.speakAndToast(context, "Opening YouTube Music for " + song + ".");

                // --- Accessibility automation: type song name and click play, do NOT click search ---
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    Intent typeSong = new Intent("com.mvp.sarah.ACTION_TYPE_TEXT");
                    typeSong.putExtra("text", song);
                    context.sendBroadcast(typeSong);

                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                        Intent clickPlay = new Intent("com.mvp.sarah.ACTION_CLICK_LABEL");
                        clickPlay.putExtra("com.mvp.sarah.EXTRA_LABEL", "Play");
                        context.sendBroadcast(clickPlay);
                    }, 2000);
                }, 2500);
                // --- End accessibility automation ---
            } else {
                FeedbackProvider.speakAndToast(context, "YouTube Music is not installed.");
            }
        } catch (Exception e) {
            FeedbackProvider.speakAndToast(context, "Couldn't open YouTube Music.");
        }
    }

    @Override
    public List<String> getSuggestions() {
        return Arrays.asList(
                "Play Shape of You on Spotify",
                "Play Imagine Dragons",
                "Play album Thriller",
                "Play Bohemian Rhapsody in YouTube Music"
        );
    }
} 