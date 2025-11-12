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
    private static final int DB_VERSION = 1;

    public static final String TABLE_DEPOSITS = "deposits";
    public static final String COL_ID = "_id";
    public static final String COL_TYPE = "type";
    public static final String COL_VALUE = "value"; // wartość kaucji w zł
    public static final String COL_BARCODE = "barcode"; // kod kreskowy (opcjonalny)

    private static final String SQL_CREATE = "CREATE TABLE " + TABLE_DEPOSITS + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_TYPE + " TEXT NOT NULL, " +
            COL_VALUE + " REAL NOT NULL, " +
            COL_BARCODE + " TEXT" +
            ")";

    public DepositDbHelper(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) { db.execSQL(SQL_CREATE); }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) { /* brak migracji */ }

    public long insertDeposit(DepositItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TYPE, item.getType());
        cv.put(COL_VALUE, item.getValue());
        cv.put(COL_BARCODE, item.getBarcode());
        return db.insert(TABLE_DEPOSITS, null, cv);
    }

    public boolean updateDeposit(DepositItem item) {
        if (item.getId() < 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_TYPE, item.getType());
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
            int idxType = c.getColumnIndexOrThrow(COL_TYPE);
            int idxValue = c.getColumnIndexOrThrow(COL_VALUE);
            int idxBarcode = c.getColumnIndexOrThrow(COL_BARCODE);
            while (c.moveToNext()) {
                list.add(new DepositItem(
                        c.getLong(idxId),
                        c.getString(idxType),
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
                int idxType = c.getColumnIndexOrThrow(COL_TYPE);
                int idxValue = c.getColumnIndexOrThrow(COL_VALUE);
                int idxBarcode = c.getColumnIndexOrThrow(COL_BARCODE);
                return new DepositItem(
                        c.getLong(idxId),
                        c.getString(idxType),
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

