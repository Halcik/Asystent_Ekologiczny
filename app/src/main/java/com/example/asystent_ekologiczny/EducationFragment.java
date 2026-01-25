package com.example.asystent_ekologiczny;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asystent_ekologiczny.education.data.EducationRepository;
import com.example.asystent_ekologiczny.education.model.EducationItem;
import com.example.asystent_ekologiczny.education.ui.EducationAdapter;
import com.example.asystent_ekologiczny.education.ui.EducationViewModel;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.List;

public class EducationFragment extends Fragment implements EducationAdapter.FullscreenListener {

    public static final String TAG = "EducationFragment";

    private EducationViewModel viewModel;
    private EducationAdapter adapter;
    private ProgressBar progressBar;
    private TextView textError;
    private TextView buttonPlayQueue;
    private final List<EducationItem> playQueue = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_education, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        textError = view.findViewById(R.id.textError);
        FloatingActionButton fabAddVideo = view.findViewById(R.id.fab_add_video);
        buttonPlayQueue = view.findViewById(R.id.buttonPlayQueue);

        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new EducationAdapter(this);
        recyclerView.setAdapter(adapter);

        EducationRepository repository = new EducationRepository(requireContext());
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @NonNull
            @Override
            public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
                if (modelClass.isAssignableFrom(EducationViewModel.class)) {
                    //noinspection unchecked
                    return (T) new EducationViewModel(repository);
                }
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
        }).get(EducationViewModel.class);

        viewModel.getUiState().observe(getViewLifecycleOwner(), uiState -> {
            if (uiState == null) return;
            progressBar.setVisibility(uiState.isLoading ? View.VISIBLE : View.GONE);

            if (uiState.errorMessage != null) {
                textError.setVisibility(View.VISIBLE);
                textError.setText(uiState.errorMessage);
            } else {
                textError.setVisibility(View.GONE);
            }

            List<EducationItem> items = uiState.items;
            adapter.setItems(items);
        });

        viewModel.load();

        fabAddVideo.setOnClickListener(v -> openAddVideoFragment());

        buttonPlayQueue.setOnClickListener(v -> {
            if (playQueue.isEmpty()) {
                Toast.makeText(requireContext(), "Kolejka jest pusta", Toast.LENGTH_SHORT).show();
            } else {
                // Budujemy listę URL-i z kolejki
                ArrayList<String> urls = new ArrayList<>();
                for (EducationItem item : playQueue) {
                    if (item.getVideoUrl() != null && !item.getVideoUrl().isEmpty()) {
                        urls.add(item.getVideoUrl());
                    }
                }
                if (urls.isEmpty()) {
                    Toast.makeText(requireContext(), "Brak poprawnych linków w kolejce", Toast.LENGTH_SHORT).show();
                    return;
                }
                Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, urls.get(0));
                intent.putStringArrayListExtra(VideoPlayerActivity.EXTRA_PLAYLIST, urls);
                intent.putExtra(VideoPlayerActivity.EXTRA_PLAYLIST_INDEX, 0);
                startActivity(intent);
            }
        });

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // Nie wywołujemy tu ponownie load(), bo teraz odświeżamy listę bezpośrednio po dodaniu.
    }

    private void openAddVideoFragment() {
        AddVideoFragment dialog = new AddVideoFragment();
        dialog.setOnVideoAddedListener(() -> {
            if (viewModel != null) {
                viewModel.load();
            }
        });
        dialog.show(getChildFragmentManager(), AddVideoFragment.TAG);
    }

    @Override
    public void onFullscreenRequested(String videoUrl) {
        Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl);
        startActivity(intent);
    }

    @Override
    public void onAddToQueueRequested(EducationItem item) {
        playQueue.add(item);
        // Na razie tylko dodajemy do kolejki; można później dodać osobny ekran/ikonę do odtwarzania całej kolejki.
        // Przykład prostego logowania lub przyszłej rozbudowy.
    }
}
