package com.example.asystent_ekologiczny.education.data;

import android.content.Context;

import com.example.asystent_ekologiczny.education.model.EducationItem;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
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
            return ResultWrapper.success(items);
        } catch (IOException | RuntimeException e) {
            return ResultWrapper.error(e);
        }
    }
}
