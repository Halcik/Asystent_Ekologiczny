package com.example.asystent_ekologiczny;

import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

public class AddVideoFragment extends DialogFragment {

    public static final String TAG = "AddVideoFragment";

    public interface OnVideoAddedListener {
        void onVideoAdded();
    }

    private OnVideoAddedListener listener;

    public void setOnVideoAddedListener(OnVideoAddedListener listener) {
        this.listener = listener;
    }

    private EditText editTitle;
    private EditText editUrl;
    private VideoDatabaseHelper dbHelper;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add_video, container, false);

        editTitle = view.findViewById(R.id.editTitle);
        editUrl = view.findViewById(R.id.editUrl);
        Button buttonSave = view.findViewById(R.id.buttonSave);
        Button buttonCancel = view.findViewById(R.id.buttonCancel);

        dbHelper = new VideoDatabaseHelper(requireContext());

        buttonSave.setOnClickListener(v -> onSaveClicked());
        buttonCancel.setOnClickListener(v -> dismiss());

        return view;
    }

    private void onSaveClicked() {
        String title = editTitle.getText().toString().trim();
        String url = editUrl.getText().toString().trim();

        if (TextUtils.isEmpty(title)) {
            editTitle.setError("Tytuł jest wymagany");
            return;
        }

        if (TextUtils.isEmpty(url)) {
            editUrl.setError("Link jest wymagany");
            return;
        }

        // Prosta walidacja formatu URL - sprawdzenie prefiksu
        if (!(url.startsWith("http://") || url.startsWith("https://"))) {
            editUrl.setError("Podaj poprawny adres URL (http/https)");
            return;
        }

        dbHelper.addVideo(title, url);
        // Proste logowanie: policz, ile jest teraz materiałów usera
        dbHelper.countUserVideos();
        Toast.makeText(requireContext(), "Materiał zapisany", Toast.LENGTH_SHORT).show();

        if (listener != null) {
            listener.onVideoAdded();
        }

        dismiss();
    }
}
