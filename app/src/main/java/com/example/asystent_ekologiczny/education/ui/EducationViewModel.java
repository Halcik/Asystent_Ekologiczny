package com.example.asystent_ekologiczny.education.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.asystent_ekologiczny.education.data.EducationRepository;
import com.example.asystent_ekologiczny.education.model.EducationItem;

import java.util.Collections;
import java.util.List;

public class EducationViewModel extends ViewModel {

    public static class UiState {
        public final boolean isLoading;
        public final List<EducationItem> items;
        public final String errorMessage;

        public UiState(boolean isLoading, List<EducationItem> items, String errorMessage) {
            this.isLoading = isLoading;
            this.items = items;
            this.errorMessage = errorMessage;
        }
    }

    private final EducationRepository repository;
    private final MutableLiveData<UiState> uiState = new MutableLiveData<>(new UiState(true, Collections.emptyList(), null));

    public EducationViewModel(EducationRepository repository) {
        this.repository = repository;
    }

    public LiveData<UiState> getUiState() {
        return uiState;
    }

    public void load() {
        uiState.setValue(new UiState(true, Collections.emptyList(), null));
        new Thread(() -> {
            com.example.asystent_ekologiczny.education.data.ResultWrapper<List<EducationItem>> result = repository.getEducationItems();
            UiState newState;
            if (result.isSuccess()) {
                List<EducationItem> items = result.getData();
                newState = new UiState(false, items != null ? items : Collections.emptyList(), null);
            } else {
                newState = new UiState(false, Collections.emptyList(), "Nie udało się wczytać materiałów edukacyjnych");
            }
            uiState.postValue(newState);
        }).start();
    }
}
