package com.example.asystent_ekologiczny.education.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asystent_ekologiczny.R;
import com.example.asystent_ekologiczny.VideoPlayerActivity;
import com.example.asystent_ekologiczny.education.data.EducationRepository;
import com.example.asystent_ekologiczny.education.model.EducationItem;

import java.util.ArrayList;
import java.util.List;

public class EducationActivity extends AppCompatActivity implements EducationAdapter.FullscreenListener {

    private EducationViewModel viewModel;
    private EducationAdapter adapter;
    private ProgressBar progressBar;
    private TextView textError;
    private final List<EducationItem> playQueue = new ArrayList<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_education);

        RecyclerView recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        textError = findViewById(R.id.textError);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new EducationAdapter(this);
        recyclerView.setAdapter(adapter);

        EducationRepository repository = new EducationRepository(this);
        viewModel = new ViewModelProvider(this, new ViewModelProvider.Factory() {
            @Override
            public <T extends ViewModel> T create(Class<T> modelClass) {
                if (modelClass.isAssignableFrom(EducationViewModel.class)) {
                    //noinspection unchecked
                    return (T) new EducationViewModel(repository);
                }
                throw new IllegalArgumentException("Unknown ViewModel class");
            }
        }).get(EducationViewModel.class);

        viewModel.getUiState().observe(this, uiState -> {
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
    }

    @Override
    public void onFullscreenRequested(String videoUrl) {
        Intent intent = new Intent(this, VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl);
        // W pełnoekranowej aktywności wymusimy orientację poziomą
        startActivity(intent);
    }

    @Override
    public void onAddToQueueRequested(EducationItem item) {
        playQueue.add(item);
        // Na razie tylko dodajemy do kolejki; można później dodać przycisk/ekran do odtwarzania całej kolejki.
    }
}
