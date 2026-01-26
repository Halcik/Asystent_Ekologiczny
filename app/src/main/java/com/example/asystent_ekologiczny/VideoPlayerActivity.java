package com.example.asystent_ekologiczny;

import android.annotation.SuppressLint;
import android.app.PendingIntent;
import android.content.Intent;
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
import com.google.android.exoplayer2.source.MediaSource;
import com.google.android.exoplayer2.source.ProgressiveMediaSource;
import com.google.android.exoplayer2.source.MergingMediaSource;
import com.google.android.exoplayer2.source.SingleSampleMediaSource;
import com.google.android.exoplayer2.upstream.DefaultDataSource;
import com.google.android.exoplayer2.util.MimeTypes;
import com.google.android.exoplayer2.C;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

// Importy dla YouTube Player
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants; // Import błędów

import android.os.Handler;
import android.os.Looper;
import android.widget.TextView;
import android.app.AlertDialog;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import android.text.InputFilter;
import android.text.InputType;
import android.widget.EditText;
import android.content.pm.PackageManager;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class VideoPlayerActivity extends AppCompatActivity {

    public static final String EXTRA_VIDEO_URL = "extra_video_url";
    public static final String EXTRA_PLAYLIST = "extra_playlist";
    public static final String EXTRA_PLAYLIST_INDEX = "extra_playlist_index";

    private ExoPlayer exoPlayer;
    private PlayerView exoPlayerView;
    private YouTubePlayer currentYouTubePlayer;
    private YouTubePlayerView youTubePlayerView;
    private boolean isYoutubeMode = false;

    private VideoDatabaseHelper videoDbHelper;
    private String currentUrl;
    private TextView subtitlesFullView;
    private Handler subtitlesHandler = new Handler(Looper.getMainLooper());
    private Runnable subtitlesRunnable;

    private ArrayList<String> playlistUrls;
    private int playlistIndex = -1;

    private PlayerNotificationManager notificationManager;
    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "video_playback_channel";

    private TextView buttonSleepTimer;
    private Handler sleepHandler = new Handler(Looper.getMainLooper());
    private Runnable sleepRunnable;

    private static final int REQ_POST_NOTIFICATIONS = 2001;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        setContentView(R.layout.activity_video_player);

        exoPlayerView = findViewById(R.id.playerView);
        youTubePlayerView = findViewById(R.id.youtubePlayerView);
        subtitlesFullView = findViewById(R.id.textSubtitlesFull);
        buttonSleepTimer = findViewById(R.id.buttonSleepTimer);

        videoDbHelper = new VideoDatabaseHelper(this);

        // Podstawowy URL (gdy nie ma playlisty)
        currentUrl = getIntent().getStringExtra(EXTRA_VIDEO_URL);
        playlistUrls = getIntent().getStringArrayListExtra(EXTRA_PLAYLIST);
        playlistIndex = getIntent().getIntExtra(EXTRA_PLAYLIST_INDEX, -1);

        if (playlistUrls != null && playlistIndex >= 0 && playlistIndex < playlistUrls.size()) {
            // Jeśli przyszliśmy z kolejką, to aktualny URL bierzemy z niej
            currentUrl = playlistUrls.get(playlistIndex);
        }

        if (currentUrl == null || currentUrl.isEmpty()) {
            Toast.makeText(this, "Brak adresu wideo", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        buttonSleepTimer.setOnClickListener(v -> showSleepTimerDialog());

        // Runtime permission na Androidzie 13+ (SDK 33) dla powiadomień
        if (Build.VERSION.SDK_INT >= 33) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.POST_NOTIFICATIONS}, REQ_POST_NOTIFICATIONS);
            }
        }

        if (isYoutubeUrl(currentUrl)) {
            setupYoutubePlayer(currentUrl);
        } else {
            setupExoPlayer(currentUrl);
            // Uruchom serwis foreground, aby zapewnić powiadomienie z kontrolkami w tle
            Intent serviceIntent = new Intent(this, VideoPlaybackService.class);
            serviceIntent.setAction(VideoPlaybackService.ACTION_START);
            serviceIntent.putExtra(VideoPlaybackService.EXTRA_VIDEO_URL, currentUrl);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        }
    }

    private void showSleepTimerDialog() {
        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("minuty, np. 10");
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(3)}); // max 999 min

        new AlertDialog.Builder(this)
                .setTitle("Sleep Timer (minuty)")
                .setView(input)
                .setPositiveButton("Ustaw", (dialog, which) -> {
                    cancelSleepTimer();
                    String text = input.getText().toString().trim();
                    if (text.isEmpty()) {
                        Toast.makeText(this, "Brak wartości — timer wyłączony", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    try {
                        int minutes = Integer.parseInt(text);
                        if (minutes <= 0) {
                            Toast.makeText(this, "Czas musi być > 0", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        startSleepTimer(minutes);
                    } catch (NumberFormatException e) {
                        Toast.makeText(this, "Nieprawidłowa liczba", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton("Wyłącz", (dialog, which) -> {
                    cancelSleepTimer();
                    Toast.makeText(this, "Sleep timer wyłączony", Toast.LENGTH_SHORT).show();
                })
                .show();
    }

    private void startSleepTimer(int minutes) {
        long delayMs = minutes * 60L * 1000L;
        sleepRunnable = () -> {
            // Zatrzymaj Exo lub YouTube w zależności od trybu
            if (exoPlayer != null) {
                exoPlayer.pause();
            }
            if (currentYouTubePlayer != null) {
                currentYouTubePlayer.pause();
            }
            Toast.makeText(this, "Odtwarzanie zatrzymane przez Sleep Timer", Toast.LENGTH_SHORT).show();
        };
        sleepHandler.postDelayed(sleepRunnable, delayMs);
        Toast.makeText(this, "Sleep timer: " + minutes + " min", Toast.LENGTH_SHORT).show();
    }

    private void cancelSleepTimer() {
        if (sleepRunnable != null) {
            sleepHandler.removeCallbacks(sleepRunnable);
            sleepRunnable = null;
        }
    }

    private void setupExoPlayer(String url) {
        isYoutubeMode = false;
        exoPlayerView.setVisibility(View.VISIBLE);
        youTubePlayerView.setVisibility(View.GONE);

        exoPlayer = new ExoPlayer.Builder(this).build();
        exoPlayerView.setPlayer(exoPlayer);

        Uri videoUri = Uri.parse(url);
        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this);
        MediaSource videoSource = new ProgressiveMediaSource.Factory(dataSourceFactory)
                .createMediaSource(MediaItem.fromUri(videoUri));

        exoPlayer.setMediaSource(videoSource);
        exoPlayer.prepare();
        exoPlayer.play();

        // Usuwamy lokalne powiadomienie z aktywności; powiadomienia obsługuje serwis
        // initPlaybackNotification();

        // Listener końca odtwarzania — przejście do następnego elementu w kolejce
        exoPlayer.addListener(new com.google.android.exoplayer2.Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int playbackState) {
                if (playbackState == com.google.android.exoplayer2.Player.STATE_ENDED) {
                    onCurrentItemFinished();
                }
            }
        });

        startDummySubtitles();

        // Zapisz historię oglądania (ostatnie odtworzenie)
        videoDbHelper.updateHistory(url);
    }

    private void onCurrentItemFinished() {
        // Jeśli mamy playlistę i są kolejne elementy, przechodzimy do następnego
        if (playlistUrls != null && playlistIndex >= 0) {
            int nextIndex = playlistIndex + 1;
            if (nextIndex < playlistUrls.size()) {
                String nextUrl = playlistUrls.get(nextIndex);
                Intent intent = new Intent(this, VideoPlayerActivity.class);
                intent.putExtra(EXTRA_VIDEO_URL, nextUrl);
                intent.putStringArrayListExtra(EXTRA_PLAYLIST, playlistUrls);
                intent.putExtra(EXTRA_PLAYLIST_INDEX, nextIndex);
                startActivity(intent);
            }
        }
        // Zamykamy bieżącą aktywność (wracamy do listy lub kolejnego elementu)
        finish();
    }

    private void startDummySubtitles() {
        if (subtitlesFullView == null) return;

        List<String> lines = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(getAssets().open("dummy_subtitles.srt"), StandardCharsets.UTF_8))) {
            String line;
            while ((line = br.readLine()) != null) {
                // Pomijamy numer i linie z czasami, bierzemy tylko teksty
                if (line.isEmpty()) continue;
                if (line.matches("\\d+")) continue;
                if (line.contains("-->")) continue;
                lines.add(line);
            }
        } catch (Exception e) {
            return; // brak pliku lub błąd – nie pokazujemy nic
        }

        if (lines.isEmpty()) return;

        subtitlesFullView.setVisibility(View.VISIBLE);

        subtitlesRunnable = new Runnable() {
            int index = 0;

            @Override
            public void run() {
                if (exoPlayer == null) return;

                if (!exoPlayer.isPlaying()) {
                    subtitlesHandler.postDelayed(this, 1000);
                    return;
                }

                subtitlesFullView.setText(lines.get(index % lines.size()));
                index++;

                subtitlesHandler.postDelayed(this, 3000); // co 3 sekundy kolejna linia
            }
        };

        subtitlesHandler.post(subtitlesRunnable);
    }

    private void setupYoutubePlayer(String url) {
        isYoutubeMode = true;
        youTubePlayerView.setVisibility(View.VISIBLE);
        exoPlayerView.setVisibility(View.GONE);

        String videoId = extractVideoId(url);

        AbstractYouTubePlayerListener listener = new AbstractYouTubePlayerListener() {
            @Override
            public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                currentYouTubePlayer = youTubePlayer;
                if (videoId != null && !videoId.isEmpty()) {
                    youTubePlayer.loadVideo(videoId, 0);
                    // Zapisz historię oglądania dla linku YouTube
                    videoDbHelper.updateHistory(url);
                }
            }


            @Override
            public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                super.onError(youTubePlayer, error);
                Toast.makeText(VideoPlayerActivity.this, "Błąd YT: " + error.name(), Toast.LENGTH_LONG).show();
            }
        };

        IFramePlayerOptions options = new IFramePlayerOptions.Builder()
                .controls(1)
                .origin("http://localhost")
                .build();

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
        if (notificationManager != null) {
            notificationManager.setPlayer(null);
        }
        if (exoPlayer != null) {
            exoPlayer.release();
        }
        if (youTubePlayerView != null) {
            youTubePlayerView.release();
        }
        if (videoDbHelper != null) {
            videoDbHelper.close();
        }
        if (subtitlesHandler != null && subtitlesRunnable != null) {
            subtitlesHandler.removeCallbacks(subtitlesRunnable);
        }
        cancelSleepTimer();
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
