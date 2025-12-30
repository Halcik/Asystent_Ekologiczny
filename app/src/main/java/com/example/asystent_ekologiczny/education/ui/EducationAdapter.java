package com.example.asystent_ekologiczny.education.ui;

import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

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

    // globalnie aktywny player i ViewHolder
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

    /** Zatrzymuje i chowa aktualny player (jeśli jest). */
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

    /** Wywoływane przez ViewHolder przed startem nowego odtwarzacza. */
    private void startNewPlayer(ViewHolder holder, ExoPlayer player) {
        stopCurrentPlayer();
        currentHolder = holder;
        currentPlayer = player;
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView descriptionView;
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
                    // dla YouTube zamiast ExoPlayera otwieramy natywną apkę/przeglądarkę
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

            itemView.setOnClickListener(v -> openVideo(item));
        }

        private void openVideo(EducationItem item) {
            String url = item.getVideoUrl();
            if (url == null || url.isEmpty()) {
                Toast.makeText(itemView.getContext(), "Brak adresu wideo", Toast.LENGTH_SHORT).show();
                return;
            }

            lastUrl = url;

            if (isYoutubeUrl(url)) {
                // Dla YouTube od razu otwieramy zewnętrzną apkę/przeglądarkę, nie używamy ExoPlayera
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
                itemView.getContext().startActivity(intent);
                return;
            }

            if (player == null) {
                player = new ExoPlayer.Builder(itemView.getContext()).build();
                playerView.setPlayer(player);
            }

            // zgłoś do adaptera, że ten player staje się aktualnym
            adapter.startNewPlayer(this, player);

            textNowPlaying.setText(item.getTitle());
            playerContainer.setVisibility(View.VISIBLE);

            MediaItem mediaItem = MediaItem.fromUri(url);
            player.setMediaItem(mediaItem);
            player.prepare();
            player.play();
        }

        private boolean isYoutubeUrl(String url) {
            String lower = url.toLowerCase();
            return lower.contains("youtube.com") || lower.contains("youtu.be");
        }

        private void stopAndHidePlayer() {
            if (player != null) {
                player.stop();
                player.clearMediaItems();
            }
            playerContainer.setVisibility(View.GONE);

            // jeśli ten ViewHolder był aktualnym, wyczyść referencje w adapterze
            if (adapter.currentHolder == this) {
                adapter.currentHolder = null;
                if (adapter.currentPlayer != null) {
                    adapter.currentPlayer.release();
                    adapter.currentPlayer = null;
                }
            }
        }

        /** Używane przez adapter: chowa UI bez zwalniania referencji ViewHolder. */
        private void hidePlayerOnly() {
            playerContainer.setVisibility(View.GONE);
        }
    }
}
