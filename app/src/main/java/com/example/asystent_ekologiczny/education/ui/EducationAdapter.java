package com.example.asystent_ekologiczny.education.ui;

import android.app.AlertDialog;
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
import com.example.asystent_ekologiczny.VideoDatabaseHelper;
import com.example.asystent_ekologiczny.VideoPlayerActivity;
import com.example.asystent_ekologiczny.education.model.EducationItem;

import java.util.ArrayList;
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

        // Obsługa długiego kliknięcia (usuwanie materiałów użytkownika)
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
        private final ImageView thumbnailView;
        private final View playerContainer;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            thumbnailView = itemView.findViewById(R.id.imageThumbnail);
            titleView = itemView.findViewById(R.id.textTitle);
            descriptionView = itemView.findViewById(R.id.textDescription);
            playerContainer = itemView.findViewById(R.id.playerContainer);
        }

        void bind(EducationItem item) {
            titleView.setText(item.getTitle());
            descriptionView.setText(item.getDescription());

            if (playerContainer != null) {
                playerContainer.setVisibility(View.GONE);
            }

            String thumbUrl = item.getThumbnailUrl();
            String videoUrl = item.getVideoUrl();

            if (thumbUrl == null || thumbUrl.isEmpty()) {
                if (isYoutubeUrl(videoUrl)) {
                    String videoId = extractYoutubeId(videoUrl);
                    if (!videoId.isEmpty()) {
                        thumbUrl = "https://img.youtube.com/vi/" + videoId + "/0.jpg";
                    }
                }
            }

            if (thumbnailView != null) {
                Glide.with(itemView.getContext())
                        .load(thumbUrl != null ? thumbUrl : R.drawable.ic_launcher_background)
                        .centerCrop()
                        .into(thumbnailView);
            }

            itemView.setOnClickListener(v -> {
                if (videoUrl == null || videoUrl.isEmpty()) {
                    Toast.makeText(v.getContext(), "Brak linku do wideo", Toast.LENGTH_SHORT).show();
                    return;
                }
                Context context = v.getContext();
                Intent intent = new Intent(context, VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl);
                context.startActivity(intent);
            });
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
