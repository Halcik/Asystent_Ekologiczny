package com.example.asystent_ekologiczny.education.data;

import android.content.Context;

import com.example.asystent_ekologiczny.VideoDatabaseHelper;
import com.example.asystent_ekologiczny.education.model.EducationItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class EducationRepository {

    private static final String FILE_NAME = "education_videos.json";

    private final Context appContext;

    public EducationRepository(Context context) {
        this.appContext = context.getApplicationContext();
    }

    public ResultWrapper<List<EducationItem>> getEducationItems() {
        try {
            List<EducationItem> allItems = new ArrayList<>();

            InputStream is = appContext.getAssets().open(FILE_NAME);
            StringBuilder builder = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    builder.append(line);
                }
            }
            String json = builder.toString();
            Gson gson = new Gson();
            Type listType = new TypeToken<List<EducationItem>>(){}.getType();
            List<EducationItem> items = gson.fromJson(json, listType);
            if (items == null) {
                items = Collections.emptyList();
            }

            // Jedna instancja helpera na całą metodę
            VideoDatabaseHelper dbHelper = new VideoDatabaseHelper(appContext);

            // Zarejestruj wszystkie materiały z JSON-a w bazie historii (jeśli jeszcze ich tam nie ma)
            for (EducationItem ei : items) {
                if (ei.getVideoUrl() != null && !ei.getVideoUrl().isEmpty()) {
                    dbHelper.upsertVideoIfMissing(ei.getTitle(), ei.getVideoUrl());
                }
            }

            allItems.addAll(items);

            // 2. Dołącz materiały dodane przez użytkownika z SQLite (jak wcześniej)
            List<VideoDatabaseHelper.VideoEntry> userVideos = dbHelper.getAllVideos();
            if (userVideos != null && !userVideos.isEmpty()) {
                for (VideoDatabaseHelper.VideoEntry entry : userVideos) {
                    if (!entry.isUser) continue; // tylko materiały dodane przez użytkownika
                    EducationItem userItem = new EducationItem(
                            entry.title,
                            "Materiał dodany przez Ciebie",
                            entry.url,
                            null
                    );
                    allItems.add(userItem);
                }
            }

            dbHelper.close();

            return ResultWrapper.success(allItems);
        } catch (IOException | RuntimeException e) {
            return ResultWrapper.error(e);
        }
    }
}
