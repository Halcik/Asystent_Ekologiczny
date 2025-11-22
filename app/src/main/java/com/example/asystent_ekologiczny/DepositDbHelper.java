package com.example.asystent_ekologiczny;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Pomocnik SQLite dla tabeli opakowań kaucyjnych.
 */
public class DepositDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "deposit.db";
    private static final int DB_VERSION = 2; // podniesiona wersja

    public static final String TABLE_DEPOSITS = "deposits";
    public static final String COL_ID = "_id";
    public static final String COL_CATEGORY = "type"; // poprzednia kolumna 'type' teraz kategoria
    public static final String COL_NAME = "name"; // nowa kolumna nazwy
    public static final String COL_VALUE = "value"; // wartość kaucji w zł
    public static final String COL_BARCODE = "barcode"; // kod kreskowy (opcjonalny)

    private static final String SQL_CREATE = "CREATE TABLE " + TABLE_DEPOSITS + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_CATEGORY + " TEXT NOT NULL, " +
            COL_NAME + " TEXT NOT NULL, " +
            COL_VALUE + " REAL NOT NULL, " +
            COL_BARCODE + " TEXT" +
            ")";

    public DepositDbHelper(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) { db.execSQL(SQL_CREATE); }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            // Dodaj kolumnę name (tymczasowo NULL), potem wypełnij istniejącymi wartościami typu jako nazwą.
            db.execSQL("ALTER TABLE " + TABLE_DEPOSITS + " ADD COLUMN " + COL_NAME + " TEXT");
            // Uzupełnij nazwę istniejącym typem.
            db.execSQL("UPDATE " + TABLE_DEPOSITS + " SET " + COL_NAME + " = " + COL_CATEGORY);
            // Zapewnienie NOT NULL - SQLite nie pozwala łatwo zmienić, więc pozostawiamy kontrakt aplikacyjny.
        }
    }

    public long insertDeposit(DepositItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CATEGORY, item.getCategory());
        cv.put(COL_NAME, item.getName());
        cv.put(COL_VALUE, item.getValue());
        cv.put(COL_BARCODE, item.getBarcode());
        return db.insert(TABLE_DEPOSITS, null, cv);
    }

    public boolean updateDeposit(DepositItem item) {
        if (item.getId() < 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CATEGORY, item.getCategory());
        cv.put(COL_NAME, item.getName());
        cv.put(COL_VALUE, item.getValue());
        cv.put(COL_BARCODE, item.getBarcode());
        int rows = db.update(TABLE_DEPOSITS, cv, COL_ID + "=?", new String[]{String.valueOf(item.getId())});
        return rows > 0;
    }

    public List<DepositItem> getAllDeposits() {
        List<DepositItem> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_DEPOSITS, null, null, null, null, null, COL_ID + " DESC")) {
            int idxId = c.getColumnIndexOrThrow(COL_ID);
            int idxCategory = c.getColumnIndexOrThrow(COL_CATEGORY);
            int idxName = c.getColumnIndexOrThrow(COL_NAME);
            int idxValue = c.getColumnIndexOrThrow(COL_VALUE);
            int idxBarcode = c.getColumnIndexOrThrow(COL_BARCODE);
            while (c.moveToNext()) {
                list.add(new DepositItem(
                        c.getLong(idxId),
                        c.getString(idxCategory),
                        c.getString(idxName),
                        c.getDouble(idxValue),
                        c.getString(idxBarcode)
                ));
            }
        }
        return list;
    }

    public DepositItem getDepositById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_DEPOSITS, null, COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToFirst()) {
                int idxId = c.getColumnIndexOrThrow(COL_ID);
                int idxCategory = c.getColumnIndexOrThrow(COL_CATEGORY);
                int idxName = c.getColumnIndexOrThrow(COL_NAME);
                int idxValue = c.getColumnIndexOrThrow(COL_VALUE);
                int idxBarcode = c.getColumnIndexOrThrow(COL_BARCODE);
                return new DepositItem(
                        c.getLong(idxId),
                        c.getString(idxCategory),
                        c.getString(idxName),
                        c.getDouble(idxValue),
                        c.getString(idxBarcode)
                );
            }
        }
        return null;
    }

    public boolean deleteDeposit(long id) {
        if (id < 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_DEPOSITS, COL_ID + "=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }
}
