package com.example.asystent_ekologiczny;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    public static final String TAG = "SettingsFragment";
    private static final String PREFS = "app_prefs";
    private static final String KEY_THEME = "theme_mode"; // system | light | dark

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_settings, container, false);
        setupThemeOptions(v);
        return v;
    }

    private void setupThemeOptions(View root) {
        RadioGroup group = root.findViewById(R.id.radio_theme_group);
        if (group == null) return;
        RadioButton rbSystem = root.findViewById(R.id.radio_theme_system);
        RadioButton rbLight = root.findViewById(R.id.radio_theme_light);
        RadioButton rbDark = root.findViewById(R.id.radio_theme_dark);

        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String current = prefs.getString(KEY_THEME, "system");
        if ("light".equals(current)) rbLight.setChecked(true); else if ("dark".equals(current)) rbDark.setChecked(true); else rbSystem.setChecked(true);

        group.setOnCheckedChangeListener((g, checkedId) -> {
            String sel;
            if (checkedId == R.id.radio_theme_light) sel = "light"; else if (checkedId == R.id.radio_theme_dark) sel = "dark"; else sel = "system";
            String stored = prefs.getString(KEY_THEME, "system");
            if (stored.equals(sel)) return; // brak zmiany
            prefs.edit().putString(KEY_THEME, sel).apply();
            applyTheme(sel);
        });
    }

    public static void applyTheme(String value) {
        int desired;
        switch (value) {
            case "light": desired = AppCompatDelegate.MODE_NIGHT_NO; break;
            case "dark": desired = AppCompatDelegate.MODE_NIGHT_YES; break;
            default: desired = AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM; break;
        }
        if (AppCompatDelegate.getDefaultNightMode() == desired) return; // uniknij zbędnej rekreacji
        AppCompatDelegate.setDefaultNightMode(desired);
    }

    public static void applySavedTheme(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String value = prefs.getString(KEY_THEME, "system");
        applyTheme(value);
    }
}
