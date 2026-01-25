package com.example.asystent_ekologiczny.education.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.asystent_ekologiczny.R;
import com.example.asystent_ekologiczny.VideoDatabaseHelper;
import com.example.asystent_ekologiczny.VideoPlayerActivity;
import com.example.asystent_ekologiczny.education.model.EducationItem;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants;
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class EducationAdapter extends RecyclerView.Adapter<EducationAdapter.ViewHolder> {

    public interface FullscreenListener {
        void onFullscreenRequested(String videoUrl);
    }

    private final List<EducationItem> items = new ArrayList<>();

    public EducationAdapter(FullscreenListener listener) {
        // listener zostawiony tylko dla kompatybilności z istniejącym kodem
    }

    public void setItems(List<EducationItem> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_education, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EducationItem item = items.get(position);
        holder.bind(item);

        // Długie kliknięcie — usuwanie materiałów użytkownika
        holder.itemView.setOnLongClickListener(v -> {
            String url = item.getVideoUrl();
            if (url == null || url.isEmpty()) {
                return false;
            }

            new AlertDialog.Builder(v.getContext())
                    .setTitle("Usuń materiał")
                    .setMessage("Czy na pewno chcesz usunąć ten materiał?")
                    .setPositiveButton("Usuń", (dialog, which) -> {
                        VideoDatabaseHelper dbHelper = new VideoDatabaseHelper(v.getContext());
                        int rows = dbHelper.deleteVideoByUrl(url);
                        if (rows > 0) {
                            int idx = holder.getBindingAdapterPosition();
                            if (idx != RecyclerView.NO_POSITION) {
                                items.remove(idx);
                                notifyItemRemoved(idx);
                            }
                        }
                    })
                    .setNegativeButton("Anuluj", null)
                    .show();
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // --- ViewHolder ---
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView descriptionView;
        private final TextView lastWatchedView;
        private final ImageView thumbnailView;
        private final View playerContainer;
        private final TextView textNowPlaying;
        private final TextView buttonFullscreen;
        private final TextView buttonClosePlayer;
        private final PlayerView playerView;
        private final YouTubePlayerView youtubePlayerView;
        private final TextView subtitlesInlineView;

        private ExoPlayer exoPlayer;
        private YouTubePlayer youTubePlayer;
        private Handler subtitlesHandler;
        private Runnable subtitlesRunnable;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailView = itemView.findViewById(R.id.imageThumbnail);
            titleView = itemView.findViewById(R.id.textTitle);
            descriptionView = itemView.findViewById(R.id.textDescription);
            lastWatchedView = itemView.findViewById(R.id.textLastWatched);
            playerContainer = itemView.findViewById(R.id.playerContainer);
            textNowPlaying = itemView.findViewById(R.id.textNowPlaying);
            buttonFullscreen = itemView.findViewById(R.id.buttonFullscreen);
            buttonClosePlayer = itemView.findViewById(R.id.buttonClosePlayer);
            playerView = itemView.findViewById(R.id.playerView);
            youtubePlayerView = itemView.findViewById(R.id.youtubePlayerView);
            subtitlesInlineView = itemView.findViewById(R.id.textSubtitlesInline);
            subtitlesHandler = new Handler(Looper.getMainLooper());
        }

        void bind(EducationItem item) {
            String videoUrl = item.getVideoUrl();

            titleView.setText(item.getTitle());
            descriptionView.setText(item.getDescription());

            // Domyślnie chowamy mini-player
            hideInlinePlayer();

            // Miniatura
            String thumbUrl = item.getThumbnailUrl();
            if (thumbUrl == null || thumbUrl.isEmpty()) {
                if (isYoutubeUrl(videoUrl)) {
                    String videoId = extractYoutubeId(videoUrl);
                    if (!videoId.isEmpty()) {
                        thumbUrl = "https://img.youtube.com/vi/" + videoId + "/0.jpg";
                    }
                }
            }

            Glide.with(itemView.getContext())
                    .load(thumbUrl != null ? thumbUrl : R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(thumbnailView);

            // Historia oglądania — zawsze coś pokazujemy (albo "-", albo datę)
            if (videoUrl != null && !videoUrl.isEmpty()) {
                VideoDatabaseHelper dbHelper = new VideoDatabaseHelper(itemView.getContext());
                String label = dbHelper.getLastWatchedLabel(itemView.getContext(), videoUrl);
                dbHelper.close();
                if (lastWatchedView != null) {
                    lastWatchedView.setText(label);
                    lastWatchedView.setVisibility(View.VISIBLE);
                }
            } else if (lastWatchedView != null) {
                lastWatchedView.setText("-");
                lastWatchedView.setVisibility(View.VISIBLE);
            }

            // Klik w miniaturę — odtwarzanie w okienku w liście + natychmiastowa aktualizacja historii w UI
            thumbnailView.setOnClickListener(v -> {
                if (videoUrl == null || videoUrl.isEmpty()) {
                    Toast.makeText(v.getContext(), "Brak linku do wideo", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Zapisz historię także przy odtwarzaniu inline
                VideoDatabaseHelper dbHelper = new VideoDatabaseHelper(itemView.getContext());
                dbHelper.updateHistory(videoUrl);
                String label = dbHelper.getLastWatchedLabel(itemView.getContext(), videoUrl);
                dbHelper.close();

                if (lastWatchedView != null) {
                    lastWatchedView.setText(label);
                    lastWatchedView.setVisibility(View.VISIBLE);
                }

                showInlinePlayer(videoUrl);

                // Dummy napisy tylko dla Exo (nie dla YouTube)
                if (!isYoutubeUrl(videoUrl)) {
                    startDummySubtitlesInline();
                }
            });

            // Klik w "Pełny ekran" — otwórz VideoPlayerActivity
            buttonFullscreen.setOnClickListener(v -> {
                if (videoUrl == null || videoUrl.isEmpty()) {
                    Toast.makeText(v.getContext(), "Brak linku do wideo", Toast.LENGTH_SHORT).show();
                    return;
                }
                Context context = v.getContext();
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl);
                context.startActivity(intent);
            });

            // Klik w "Zamknij" — schowaj inline player i zatrzymaj odtwarzanie
            buttonClosePlayer.setOnClickListener(v -> {
                hideInlinePlayer();
                stopDummySubtitlesInline();
            });
        }

        private void startDummySubtitlesInline() {
            if (subtitlesInlineView == null) return;

            java.util.List<String> lines = new java.util.ArrayList<>();
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(itemView.getContext().getAssets().open("dummy_subtitles.srt"), StandardCharsets.UTF_8))) {
                String line;
                while ((line = br.readLine()) != null) {
                    if (line.isEmpty()) continue;
                    if (line.matches("\\d+")) continue;
                    if (line.contains("-->")) continue;
                    lines.add(line);
                }
            } catch (Exception e) {
                return;
            }

            if (lines.isEmpty()) return;

            subtitlesInlineView.setVisibility(View.VISIBLE);

            subtitlesRunnable = new Runnable() {
                int index = 0;

                @Override
                public void run() {
                    // Jeśli nie mamy Exo albo jest spauzowany, nie przesuwamy napisów
                    if (exoPlayer == null || !exoPlayer.isPlaying()) {
                        subtitlesHandler.postDelayed(this, 500);
                        return;
                    }

                    subtitlesInlineView.setText(lines.get(index % lines.size()));
                    index++;
                    subtitlesHandler.postDelayed(this, 3000);
                }
            };

            subtitlesHandler.post(subtitlesRunnable);
        }

        private void stopDummySubtitlesInline() {
            if (subtitlesHandler != null && subtitlesRunnable != null) {
                subtitlesHandler.removeCallbacks(subtitlesRunnable);
            }
            if (subtitlesInlineView != null) {
                subtitlesInlineView.setVisibility(View.GONE);
            }
        }

        private void showInlinePlayer(String url) {
            playerContainer.setVisibility(View.VISIBLE);
            textNowPlaying.setText("Odtwarzanie w liście");

            if (isYoutubeUrl(url)) {
                // YouTube inline
                playerView.setVisibility(View.GONE);
                subtitlesInlineView.setVisibility(View.GONE); // brak dummy napisów dla YT

                String videoId = extractYoutubeId(url);

                youtubePlayerView.initialize(new AbstractYouTubePlayerListener() {
                    @Override
                    public void onReady(@NonNull YouTubePlayer youTubePlayer) {
                        EducationAdapter.ViewHolder.this.youTubePlayer = youTubePlayer;
                        if (videoId != null && !videoId.isEmpty()) {
                            youTubePlayer.loadVideo(videoId, 0);
                        }
                    }

                    @Override
                    public void onError(@NonNull YouTubePlayer youTubePlayer, @NonNull PlayerConstants.PlayerError error) {
                        super.onError(youTubePlayer, error);
                        Toast.makeText(itemView.getContext(), "Błąd YT: " + error.name(), Toast.LENGTH_LONG).show();
                    }
                }, new IFramePlayerOptions.Builder().controls(1).build());

            } else {
                // Zwykły link w ExoPlayerze
                youtubePlayerView.setVisibility(View.GONE);
                playerView.setVisibility(View.VISIBLE);

                exoPlayer = new ExoPlayer.Builder(itemView.getContext()).build();
                playerView.setPlayer(exoPlayer);
                MediaItem mediaItem = MediaItem.fromUri(Uri.parse(url));
                exoPlayer.setMediaItem(mediaItem);
                exoPlayer.prepare();
                exoPlayer.play();
            }
        }

        private void hideInlinePlayer() {
            playerContainer.setVisibility(View.GONE);
            if (exoPlayer != null) {
                exoPlayer.stop();
                exoPlayer.release();
                exoPlayer = null;
            }
            if (youTubePlayer != null) {
                youTubePlayer.pause();
                youTubePlayer = null;
            }
            playerView.setPlayer(null);
            subtitlesInlineView.setVisibility(View.GONE);
        }

        private boolean isYoutubeUrl(String url) {
            if (url == null) return false;
            String lower = url.toLowerCase();
            return lower.contains("youtube.com") || lower.contains("youtu.be");
        }

        private String extractYoutubeId(String url) {
            try {
                String videoId = "";
                if (url.contains("v=")) {
                    videoId = url.substring(url.indexOf("v=") + 2);
                    if (videoId.contains("&")) videoId = videoId.substring(0, videoId.indexOf("&"));
                } else if (url.contains("youtu.be/")) {
                    videoId = url.substring(url.lastIndexOf("/") + 1);
                }
                return videoId;
            } catch (Exception e) {
                return "";
            }
        }
    }
}
