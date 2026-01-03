package com.example.asystent_ekologiczny.education.ui;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView; // 1. Import ImageView
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide; // 2. Import Glide
import com.example.asystent_ekologiczny.R;
import com.example.asystent_ekologiczny.education.model.EducationItem;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.ui.PlayerView;

import java.util.ArrayList;
import java.util.List;

public class EducationAdapter extends RecyclerView.Adapter<EducationAdapter.ViewHolder> {

    public interface FullscreenListener {
        void onFullscreenRequested(String videoUrl);
    }

    private final List<EducationItem> items = new ArrayList<>();
    private ExoPlayer currentPlayer;
    private ViewHolder currentHolder;
    private final FullscreenListener fullscreenListener;

    public EducationAdapter(FullscreenListener fullscreenListener) {
        this.fullscreenListener = fullscreenListener;
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
        return new ViewHolder(view, this);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        EducationItem item = items.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    private void stopCurrentPlayer() {
        if (currentHolder != null) {
            currentHolder.hidePlayerOnly();
        }
        if (currentPlayer != null) {
            currentPlayer.stop();
            currentPlayer.clearMediaItems();
            currentPlayer.release();
            currentPlayer = null;
        }
        currentHolder = null;
    }

    private void startNewPlayer(ViewHolder holder, ExoPlayer player) {
        stopCurrentPlayer();
        currentHolder = holder;
        currentPlayer = player;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView descriptionView;
        private final ImageView thumbnailView; // 3. Nowe pole miniatury
        private final View playerContainer;
        private final TextView textNowPlaying;
        private final TextView buttonFullscreen;
        private final TextView buttonClosePlayer;
        private final PlayerView playerView;
        private final EducationAdapter adapter;
        private ExoPlayer player;
        private String lastUrl;

        ViewHolder(@NonNull View itemView, EducationAdapter adapter) {
            super(itemView);
            this.adapter = adapter;

            // Pamiętaj: musisz dodać ImageView o id 'imageThumbnail' w item_education.xml
            thumbnailView = itemView.findViewById(R.id.imageThumbnail);

            titleView = itemView.findViewById(R.id.textTitle);
            descriptionView = itemView.findViewById(R.id.textDescription);
            playerContainer = itemView.findViewById(R.id.playerContainer);
            textNowPlaying = itemView.findViewById(R.id.textNowPlaying);
            buttonFullscreen = itemView.findViewById(R.id.buttonFullscreen);
            buttonClosePlayer = itemView.findViewById(R.id.buttonClosePlayer);
            playerView = itemView.findViewById(R.id.playerView);

            buttonClosePlayer.setOnClickListener(v -> stopAndHidePlayer());
            buttonFullscreen.setOnClickListener(v -> {
                if (lastUrl != null && !lastUrl.isEmpty() && adapter.fullscreenListener != null) {
                    if (isYoutubeUrl(lastUrl)) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(lastUrl));
                        itemView.getContext().startActivity(intent);
                    } else {
                        adapter.fullscreenListener.onFullscreenRequested(lastUrl);
                    }
                }
            });
        }

        void bind(EducationItem item) {
            titleView.setText(item.getTitle());
            descriptionView.setText(item.getDescription());

            // --- 4. Dodana logika Miniatur z Glide ---
            String thumbUrl = item.getThumbnailUrl(); // Upewnij się, że masz taką metodę w EducationItem
            String videoUrl = item.getVideoUrl();

            // Automatyczne generowanie miniatury dla YouTube jeśli brak w JSON
            if (thumbUrl == null || thumbUrl.isEmpty()) {
                if (isYoutubeUrl(videoUrl)) {
                    try {
                        String videoId = "";
                        if (videoUrl.contains("v=")) {
                            videoId = videoUrl.substring(videoUrl.indexOf("v=") + 2);
                            if (videoId.contains("&")) videoId = videoId.substring(0, videoId.indexOf("&"));
                        } else if (videoUrl.contains("youtu.be/")) {
                            videoId = videoUrl.substring(videoUrl.lastIndexOf("/") + 1);
                        }
                        if (!videoId.isEmpty()) {
                            thumbUrl = "https://img.youtube.com/vi/" + videoId + "/0.jpg";
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }

            // Ładowanie przez Glide
            if (thumbnailView != null) {
                Glide.with(itemView.getContext())
                        .load(thumbUrl != null ? thumbUrl : R.drawable.ic_launcher_background) // Placeholder
                        .centerCrop()
                        .into(thumbnailView);
            }

            itemView.setOnClickListener(v -> openVideo(item));
        }

        // ... (reszta metod bez zmian: openVideo, isYoutubeUrl, stopAndHidePlayer, hidePlayerOnly)
        // Wklejam je dla kompletności, abyś mógł skopiować cały plik

        private void openVideo(EducationItem item) {
            String url = item.getVideoUrl();
            if (url == null || url.isEmpty()) {
                Toast.makeText(itemView.getContext(), "Brak adresu wideo", Toast.LENGTH_SHORT).show();
                return;
            }
            lastUrl = url;
            if (isYoutubeUrl(url)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                itemView.getContext().startActivity(intent);
                return;
            }
            if (player == null) {
                player = new ExoPlayer.Builder(itemView.getContext()).build();
                playerView.setPlayer(player);
            }
            adapter.startNewPlayer(this, player);
            textNowPlaying.setText(item.getTitle());
            playerContainer.setVisibility(View.VISIBLE);
            // Ukrywamy miniaturę, gdy player gra (opcjonalne, ale ładne)
            if (thumbnailView != null) thumbnailView.setVisibility(View.GONE);

            MediaItem mediaItem = MediaItem.fromUri(url);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }

        private boolean isYoutubeUrl(String url) {
            if (url == null) return false;
            String lower = url.toLowerCase();
            return lower.contains("youtube.com") || lower.contains("youtu.be");
        }

        private void stopAndHidePlayer() {
            if (player != null) {
                player.stop();
                player.clearMediaItems();
            }
            playerContainer.setVisibility(View.GONE);
            // Przywracamy miniaturę po zamknięciu playera
            if (thumbnailView != null) thumbnailView.setVisibility(View.VISIBLE);

            if (adapter.currentHolder == this) {
                adapter.currentHolder = null;
                if (adapter.currentPlayer != null) {
                    adapter.currentPlayer.release();
                    adapter.currentPlayer = null;
                }
            }
        }

        private void hidePlayerOnly() {
            playerContainer.setVisibility(View.GONE);
            if (thumbnailView != null) thumbnailView.setVisibility(View.VISIBLE);
        }
    }
}
