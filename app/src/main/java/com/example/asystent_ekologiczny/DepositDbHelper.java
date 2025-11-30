package com.example.asystent_ekologiczny;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * Pomocnik SQLite dla tabeli opakowań kaucyjnych.
 */
public class DepositDbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "deposit.db";
    private static final int DB_VERSION = 4; // dodano returned_at

    public static final String TABLE_DEPOSITS = "deposits";
    public static final String COL_ID = "_id";
    public static final String COL_CATEGORY = "type";
    public static final String COL_NAME = "name";
    public static final String COL_VALUE = "value";
    public static final String COL_BARCODE = "barcode";
    public static final String COL_RETURNED = "returned"; // 0/1
    public static final String COL_RETURNED_AT = "returned_at"; // TEXT ISO8601 YYYY-MM-DD

    private static final String SQL_CREATE = "CREATE TABLE " + TABLE_DEPOSITS + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_CATEGORY + " TEXT NOT NULL, " +
            COL_NAME + " TEXT NOT NULL, " +
            COL_VALUE + " REAL NOT NULL, " +
            COL_BARCODE + " TEXT, " +
            COL_RETURNED + " INTEGER NOT NULL DEFAULT 0, " +
            COL_RETURNED_AT + " TEXT" +
            ")";

    public DepositDbHelper(Context ctx) { super(ctx, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) { db.execSQL(SQL_CREATE); }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_DEPOSITS + " ADD COLUMN " + COL_NAME + " TEXT");
            db.execSQL("UPDATE " + TABLE_DEPOSITS + " SET " + COL_NAME + " = " + COL_CATEGORY);
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE " + TABLE_DEPOSITS + " ADD COLUMN " + COL_RETURNED + " INTEGER NOT NULL DEFAULT 0");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE " + TABLE_DEPOSITS + " ADD COLUMN " + COL_RETURNED_AT + " TEXT");
        }
    }

    public long insertDeposit(DepositItem item) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_CATEGORY, item.getCategory());
        cv.put(COL_NAME, item.getName());
        cv.put(COL_VALUE, item.getValue());
        cv.put(COL_BARCODE, item.getBarcode());
        cv.put(COL_RETURNED, item.isReturned() ? 1 : 0);
        cv.put(COL_RETURNED_AT, (String) null);
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
        cv.put(COL_RETURNED, item.isReturned() ? 1 : 0);
        // jeśli ustawiamy jako zwrócone, a nie ma daty – ustaw bieżącą
        cv.put(COL_RETURNED_AT, item.isReturned() ? todayIso() : null);
        int rows = db.update(TABLE_DEPOSITS, cv, COL_ID + "=?", new String[]{String.valueOf(item.getId())});
        return rows > 0;
    }

    /** Szybka aktualizacja pola returned + data zwrotu. */
    public boolean setReturned(long id, boolean returned) {
        if (id < 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_RETURNED, returned ? 1 : 0);
        cv.put(COL_RETURNED_AT, returned ? todayIso() : null);
        int rows = db.update(TABLE_DEPOSITS, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public boolean deleteDeposit(long id) {
        if (id < 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_DEPOSITS, COL_ID + "=?", new String[]{String.valueOf(id)});
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
            int idxReturned = c.getColumnIndexOrThrow(COL_RETURNED);
            // return_at nie jest używany w modelu – tylko do zapytań statystycznych
            while (c.moveToNext()) {
                list.add(new DepositItem(
                        c.getLong(idxId),
                        c.getString(idxCategory),
                        c.getString(idxName),
                        c.getDouble(idxValue),
                        c.getString(idxBarcode),
                        c.getInt(idxReturned) == 1
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
                int idxReturned = c.getColumnIndexOrThrow(COL_RETURNED);
                return new DepositItem(
                        c.getLong(idxId),
                        c.getString(idxCategory),
                        c.getString(idxName),
                        c.getDouble(idxValue),
                        c.getString(idxBarcode),
                        c.getInt(idxReturned) == 1
                );
            }
        }
        return null;
    }

    /** Zwraca liczbę opakowań oznaczonych jako zwrócone w danym miesiącu (YYYY-MM). */
    public int countReturnedInMonth(int year, int month01) {
        // month01: 1-12
        String ym = String.format(Locale.getDefault(), "%04d-%02d", year, month01);
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_DEPOSITS, new String[]{"COUNT(*) AS cnt"},
                COL_RETURNED + "=1 AND " + COL_RETURNED_AT + " LIKE ?",
                new String[]{ym + "%"}, null, null, null)) {
            if (c.moveToFirst()) {
                return c.getInt(c.getColumnIndexOrThrow("cnt"));
            }
        }
        return 0;
    }

    /** Zwraca sumę wartości (zł) opakowań oznaczonych jako zwrócone w danym miesiącu. */
    public double sumReturnedValueInMonth(int year, int month01) {
        String ym = String.format(Locale.ROOT, "%04d-%02d", year, month01);
        SQLiteDatabase db = getReadableDatabase();
        double sum = 0.0;
        String selection = COL_RETURNED + "=1 AND " + COL_RETURNED_AT + " LIKE ?";
        String[] args = new String[]{ym + "%"};
        try (Cursor c = db.query(TABLE_DEPOSITS, new String[]{"SUM(" + COL_VALUE + ") AS total"}, selection, args, null, null, null)) {
            if (c.moveToFirst()) {
                int idx = c.getColumnIndexOrThrow("total");
                if (!c.isNull(idx)) {
                    sum = c.getDouble(idx);
                }
            }
        }
        return sum;
    }

    private String todayIso() {
        return new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Calendar.getInstance().getTime());
    }
}
