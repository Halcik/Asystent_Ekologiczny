package com.example.asystent_ekologiczny.education.ui;

import android.content.Context;
import android.content.Intent;
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
import com.example.asystent_ekologiczny.VideoPlayerActivity; // Upewnij się, że ten import pasuje do pakietu VideoPlayerActivity
import com.example.asystent_ekologiczny.education.model.EducationItem;

import java.util.ArrayList;
import java.util.List;

public class EducationAdapter extends RecyclerView.Adapter<EducationAdapter.ViewHolder> {

    // Listener nie jest już potrzebny, bo kliknięcie obsługujemy bezpośrednio w onBind,
    // ale zostawiam interfejs, gdybyś go używał gdzieś w Activity (choć teraz jest zbędny).
    public interface FullscreenListener {
        void onFullscreenRequested(String videoUrl);
    }

    private final List<EducationItem> items = new ArrayList<>();

    // Konstruktor pusty, bo logika kliknięcia jest teraz wewnątrz adaptera
    public EducationAdapter(FullscreenListener listener) {
        // Listener ignorowany w nowym podejściu, ale zachowany dla kompatybilności wstecznej
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
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    // --- ViewHolder ---
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView descriptionView;
        private final ImageView thumbnailView;
        // Elementy playera z XML mogą zostać (by nie zmieniać layoutu),
        // ale będziemy je trzymać ukryte (GONE).
        private final View playerContainer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailView = itemView.findViewById(R.id.imageThumbnail);
            titleView = itemView.findViewById(R.id.textTitle);
            descriptionView = itemView.findViewById(R.id.textDescription);

            // Pobieramy kontener, żeby upewnić się, że jest ukryty
            playerContainer = itemView.findViewById(R.id.playerContainer);
        }

        void bind(EducationItem item) {
            titleView.setText(item.getTitle());
            descriptionView.setText(item.getDescription());

            // Zawsze ukrywamy inline player, bo otwieramy w nowym oknie
            if (playerContainer != null) {
                playerContainer.setVisibility(View.GONE);
            }

            // --- 1. Obsługa Miniatury (Glide) ---
            String thumbUrl = item.getThumbnailUrl();
            String videoUrl = item.getVideoUrl();

            // Jeśli brak miniatury w JSON, próbujemy wygenerować dla YouTube
            if (thumbUrl == null || thumbUrl.isEmpty()) {
                if (isYoutubeUrl(videoUrl)) {
                    String videoId = extractYoutubeId(videoUrl);
                    if (!videoId.isEmpty()) {
                        thumbUrl = "https://img.youtube.com/vi/" + videoId + "/0.jpg";
                    }
                }
            }

            // Ładowanie obrazka
            if (thumbnailView != null) {
                Glide.with(itemView.getContext())
                        .load(thumbUrl != null ? thumbUrl : R.drawable.ic_launcher_background) // Domyślna ikonka
                        .centerCrop()
                        .into(thumbnailView);
            }

            // --- 2. KLUCZOWE: Kliknięcie otwiera VideoPlayerActivity ---
            itemView.setOnClickListener(v -> {
                if (videoUrl == null || videoUrl.isEmpty()) {
                    Toast.makeText(v.getContext(), "Brak linku do wideo", Toast.LENGTH_SHORT).show();
                    return;
                }

                Context context = v.getContext();
                // Otwieramy nasze własne Activity, a nie systemowe
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl);
                context.startActivity(intent);
            });
        }

        // Pomocnicze metody do miniatur
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
