package com.example.asystent_ekologiczny;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.exoplayer2.audio.AudioAttributes;
import com.google.android.exoplayer2.ui.PlayerNotificationManager;

/**
 * Prosty serwis odtwarzania wideo w tle z powiadomieniem i kontrolkami.
 * Na potrzeby projektu: startuje ExoPlayera i pokazuje PlayerNotificationManager,
 * aby w pasku powiadomień były przyciski play/pause, next/prev.
 */
public class VideoPlaybackService extends Service {

    public static final String ACTION_START = "com.example.asystent_ekologiczny.action.START";
    public static final String EXTRA_VIDEO_URL = "extra_video_url";

    private static final int NOTIFICATION_ID = 1001;
    private static final String CHANNEL_ID = "video_playback_channel";

    private ExoPlayer exoPlayer;
    private PlayerNotificationManager notificationManager;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null; // Nie wspieramy bind w tej wersji
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String url = intent.getStringExtra(EXTRA_VIDEO_URL);
        if (url == null || url.isEmpty()) {
            stopSelf();
            return START_NOT_STICKY;
        }

        createNotificationChannel();
        initPlayer(this, url);
        initNotification(this);

        return START_STICKY;
    }

    private void initPlayer(Context context, String url) {
        if (exoPlayer == null) {
            exoPlayer = new ExoPlayer.Builder(context).build();
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setContentType(com.google.android.exoplayer2.C.AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(com.google.android.exoplayer2.C.USAGE_MEDIA)
                    .build();
            exoPlayer.setAudioAttributes(audioAttributes, true);
        }
        exoPlayer.setMediaItem(MediaItem.fromUri(url));
        exoPlayer.prepare();
        exoPlayer.play();
    }

    private void initNotification(Context context) {
        // Intent powrotu do VideoPlayerActivity po kliknięciu w powiadomienie
        Intent activityIntent = new Intent(context, VideoPlayerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                0,
                activityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0)
        );

        notificationManager = new PlayerNotificationManager.Builder(
                context,
                NOTIFICATION_ID,
                CHANNEL_ID
        ).setMediaDescriptionAdapter(new PlayerNotificationManager.MediaDescriptionAdapter() {
            @Override
            public CharSequence getCurrentContentTitle(Player player) {
                return "Odtwarzanie wideo";
            }

            @Nullable
            @Override
            public PendingIntent createCurrentContentIntent(Player player) {
                return contentIntent;
            }

            @Nullable
            @Override
            public CharSequence getCurrentContentText(Player player) {
                return null;
            }

            @Nullable
            @Override
            public android.graphics.Bitmap getCurrentLargeIcon(Player player, PlayerNotificationManager.BitmapCallback callback) {
                return null;
            }
        }).build();

        notificationManager.setPlayer(exoPlayer);
        notificationManager.setUseNextAction(false);
        notificationManager.setUsePreviousAction(false);

        // Stwórz prostą notyfikację startową dla foreground service
        Notification notification = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setContentTitle("Odtwarzanie wideo")
                .setContentText("Wideo odtwarzane w tle")
                .setSmallIcon(R.drawable.ic_launcher_foreground)
                .setContentIntent(contentIntent)
                .setOngoing(true)
                .build();

        startForeground(NOTIFICATION_ID, notification);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Odtwarzanie wideo",
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (notificationManager != null) {
            notificationManager.setPlayer(null);
        }
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
