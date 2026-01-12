package com.example.asystent_ekologiczny;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class VideoDatabaseHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "VideoDb";
    private static final int DB_VERSION = 1;
    private static final String TABLE_VIDEOS = "videos";

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
                "last_watched LONG DEFAULT 0)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_VIDEOS);
        onCreate(db);
    }

    // Dodanie własnego linku (Dodatkowe I)
    public void addVideo(String title, String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("title", title);
        values.put("url", url);
        db.insert(TABLE_VIDEOS, null, values);
        db.close();
    }

    // Aktualizacja historii (Dodatkowe I)
    public void updateHistory(String url) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("last_watched", System.currentTimeMillis());
        // Aktualizujemy rekord o danym URL (zakładając unikalność URL dla uproszczenia)
        db.update(TABLE_VIDEOS, values, "url = ?", new String[]{url});
        db.close();
    }
}
