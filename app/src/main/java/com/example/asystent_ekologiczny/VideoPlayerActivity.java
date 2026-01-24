package com.example.asystent_ekologiczny;

import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import android.content.res.Configuration;
import android.os.Build;
import androidx.annotation.RequiresApi;


import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;

// Importy dla YouTube Player
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants; // Import błędów

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL = "extra_video_url";

    private ExoPlayer exoPlayer;
    private PlayerView exoPlayerView;
    private YouTubePlayer currentYouTubePlayer;


    private YouTubePlayerView youTubePlayerView;
    private boolean isYoutubeMode = false;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_video_player);

        // Inicjalizacja widoków
        exoPlayerView = findViewById(R.id.playerView);
        youTubePlayerView = findViewById(R.id.youtubePlayerView);

        // Pobranie URL
        String url = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        if (url == null || url.isEmpty()) {
            Toast.makeText(this, "Brak adresu wideo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        if (isYoutubeUrl(url)) {
            setupYoutubePlayer(url);
        } else {
            setupExoPlayer(url);
        }
    }

    private void setupExoPlayer(String url) {
        isYoutubeMode = false;

        // Pokaż ExoPlayer, ukryj YouTube
        exoPlayerView.setVisibility(View.VISIBLE);
        youTubePlayerView.setVisibility(View.GONE);

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayerView.setPlayer(exoPlayer);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
        exoPlayer.setMediaItem(mediaItem);
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void setupYoutubePlayer(String url) {
        isYoutubeMode = true;
        youTubePlayerView.setVisibility(View.VISIBLE);
        exoPlayerView.setVisibility(View.GONE);


        String videoId = extractVideoId(url);

        Toast.makeText(this, "ID: " + videoId, Toast.LENGTH_LONG).show();

        AbstractYouTubePlayerListener listener = new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                currentYouTubePlayer = youTubePlayer;
                if (videoId != null && !videoId.isEmpty()) {
                    youTubePlayer.loadVideo(videoId, 0);
                }
            }


            @Override
            public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                super.onError(youTubePlayer, error);
                Toast.makeText(VideoPlayerActivity.this, "Błąd YT: " + error.name(), Toast.LENGTH_LONG).show();
            }
        };

        // Konfiguracja Opcji (Origin)
        IFramePlayerOptions options = new IFramePlayerOptions.Builder()
                .controls(1)
                .origin("http://localhost")
                .build();

        // Ręczna inicjalizacja (tylko raz!)
        youTubePlayerView.initialize(listener, options);
    }

    private boolean isYoutubeUrl(String url) {
        if (url == null) return false;
        String lower = url.toLowerCase();
        return lower.contains("youtube.com") || lower.contains("youtu.be");
    }

    private String extractVideoId(String url) {
        if (url == null) return "";
        url = url.trim();
        String videoId = "";

        try {
            // Obsługa standardowych linków (youtube.com/watch?v=ID)
            if (url.contains("v=")) {
                videoId = url.substring(url.indexOf("v=") + 2);
                int ampIndex = videoId.indexOf("&");
                if (ampIndex != -1) {
                    videoId = videoId.substring(0, ampIndex);
                }
            }
            // Obsługa skróconych linków (youtu.be/ID)
            else if (url.contains("youtu.be/")) {
                videoId = url.substring(url.lastIndexOf("/") + 1);
                int qIndex = videoId.indexOf("?");
                if (qIndex != -1) {
                    videoId = videoId.substring(0, qIndex);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return videoId;
    }
    @Override
    protected void onPause() {
        super.onPause();

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            if (isInPictureInPictureMode()) {
                return;
            }
        }

        if (exoPlayer != null) {
            exoPlayer.pause();
        }
        if (currentYouTubePlayer != null) {
            currentYouTubePlayer.pause();
        }
    }




    @Override
    protected void onStop() {
        super.onStop();
        if (exoPlayer != null) exoPlayer.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
    }

    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        if (isYoutubeMode) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                enterPictureInPictureMode();
            }
        } else {
            if (exoPlayer != null && exoPlayer.isPlaying()) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                    enterPictureInPictureMode();
                }
            }
        }
    }
    @RequiresApi(api = Build.VERSION_CODES.O)
    @Override
    public void onPictureInPictureModeChanged(boolean isInPictureInPictureMode, Configuration newConfig) {
        // Ważne: W nowszych wersjach Androida (API 26+) sygnatura wymaga Configuration
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig);

        if (isInPictureInPictureMode) {
            // Jesteśmy w PiP - ukrywamy zbędne elementy UI
            // Np. jeśli masz jakiś przycisk "Zamknij" lub pasek tytułu, ukryj go tutaj
            // if (closeButton != null) closeButton.setVisibility(View.GONE);

            // Wymuszamy playera żeby grał (jeśli system go spauzował)
            if (currentYouTubePlayer != null) {
                currentYouTubePlayer.play();
            }
            if (exoPlayer != null && !exoPlayer.isPlaying()) {
                exoPlayer.play();
            }
        } else {
            // Wychodzimy z PiP - przywróć widoczność
            // if (closeButton != null) closeButton.setVisibility(View.VISIBLE);
        }
    }

}
