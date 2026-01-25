package com.example.asystent_ekologiczny;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

public class VideoDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "VideoDb";
    private static final int DB_VERSION = 2; // podniesienie wersji bazy dla nowej kolumny
    private static final String TABLE_VIDEOS = "videos";

    private static final String TAG = "VideoDb";

    public VideoDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Tabela przechowująca: tytuł, url, datę ostatniego otwarcia (historia)
        db.execSQL("CREATE TABLE " + TABLE_VIDEOS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "title TEXT, " +
                "url TEXT, " +
                "last_watched LONG DEFAULT 0, " +
                "is_user INTEGER DEFAULT 0)" );
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_VIDEOS + " ADD COLUMN is_user INTEGER DEFAULT 0");
        }
    }

    // Dodanie własnego linku (Dodatkowe I)
    public void addVideo(String title, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("url", url);
        values.put("is_user", 1);
        long id = db.insert(TABLE_VIDEOS, null, values);
        Log.d(TAG, "addVideo: inserted id=" + id + " title=" + title + " url=" + url);
        db.close();
    }

    public int countUserVideos() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_VIDEOS + " WHERE is_user = 1", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        Log.d(TAG, "countUserVideos: " + count);
        return count;
    }

    // Aktualizacja historii oglądania: zapis daty ostatniego odtworzenia filmu o danym URL
    public void updateHistory(String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("last_watched", System.currentTimeMillis());
        // Aktualizujemy rekord o danym URL (zakładając unikalność URL dla uproszczenia)
        db.update(TABLE_VIDEOS, values, "url = ?", new String[]{url});
        db.close();
    }

    public int deleteVideoByUrl(String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        int rows = db.delete(TABLE_VIDEOS, "url = ? AND is_user = 1", new String[]{url});
        db.close();
        return rows;
    }

    public static class VideoEntry {
        public final String title;
        public final String url;
        public final boolean isUser;

        public VideoEntry(String title, String url, boolean isUser) {
            this.title = title;
            this.url = url;
            this.isUser = isUser;
        }
    }

    public List<VideoEntry> getAllVideos() {
        List<VideoEntry> result = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_VIDEOS,
                new String[]{"title", "url", "is_user"},
                null,
                null,
                null,
                null,
                "id ASC");
        if (cursor != null) {
            try {
                while (cursor.moveToNext()) {
                    String title = cursor.getString(cursor.getColumnIndexOrThrow("title"));
                    String url = cursor.getString(cursor.getColumnIndexOrThrow("url"));
                    boolean isUser = cursor.getInt(cursor.getColumnIndexOrThrow("is_user")) == 1;
                    result.add(new VideoEntry(title, url, isUser));
                }
            } finally {
                cursor.close();
            }
        }
        db.close();
        return result;
    }

    public long getLastWatched(String url) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_VIDEOS,
                new String[]{"last_watched"},
                "url = ?",
                new String[]{url},
                null,
                null,
                null);
        long value = 0;
        if (cursor.moveToFirst()) {
            value = cursor.getLong(cursor.getColumnIndexOrThrow("last_watched"));
        }
        cursor.close();
        db.close();
        return value;
    }

    public String getLastWatchedLabel(Context context, String url) {
        long ts = getLastWatched(url);
        if (ts <= 0) {
            // Brak historii – zwróć myślnik lub pusty tekst
            return "-";
        }
        java.text.DateFormat df = android.text.format.DateFormat.getDateFormat(context);
        java.text.DateFormat tf = android.text.format.DateFormat.getTimeFormat(context);
        java.util.Date date = new java.util.Date(ts);
        return "Oglądano: " + df.format(date) + ", " + tf.format(date);
    }

    public void upsertVideoIfMissing(String title, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cursor = db.query(TABLE_VIDEOS,
                new String[]{"id"},
                "url = ?",
                new String[]{url},
                null,
                null,
                null);
        boolean exists = false;
        if (cursor.moveToFirst()) {
            exists = true;
        }
        cursor.close();

        if (!exists) {
            ContentValues values = new ContentValues();
            values.put("title", title);
            values.put("url", url);
            values.put("is_user", 0); // wideo z JSON-a, nie userowe
            db.insert(TABLE_VIDEOS, null, values);
        }
        db.close();
    }
}
