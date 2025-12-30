package com.example.asystent_ekologiczny;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.asystent_ekologiczny.education.data.EducationRepository;
import com.example.asystent_ekologiczny.education.model.EducationItem;
import com.example.asystent_ekologiczny.education.ui.EducationAdapter;
import com.example.asystent_ekologiczny.education.ui.EducationViewModel;

import java.util.List;

public class EducationFragment extends Fragment implements EducationAdapter.FullscreenListener {

    public static final String TAG = "EducationFragment";

    private EducationViewModel viewModel;
    private EducationAdapter adapter;
    private ProgressBar progressBar;
    private TextView textError;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_education, container, false);

        RecyclerView recyclerView = view.findViewById(R.id.recyclerView);
        progressBar = view.findViewById(R.id.progressBar);
        textError = view.findViewById(R.id.textError);

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

        return view;
    }

    @Override
    public void onFullscreenRequested(String videoUrl) {
        Intent intent = new Intent(requireContext(), VideoPlayerActivity.class);
        intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, videoUrl);
        startActivity(intent);
    }
}
