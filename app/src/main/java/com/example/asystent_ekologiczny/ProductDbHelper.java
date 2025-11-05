package com.example.asystent_ekologiczny;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/**
 * Pomocnik SQLite dla tabeli produktów.
 * Przechowuje podstawowe informacje wpisywane w formularzu.
 */
public class ProductDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "products.db";
    private static final int DB_VERSION = 2; // podniesiona wersja aby dodać kolumnę used

    public static final String TABLE_PRODUCTS = "products";
    public static final String COL_ID = "_id";
    public static final String COL_NAME = "name";
    public static final String COL_PRICE = "price";
    public static final String COL_EXPIRATION = "expiration_date";
    public static final String COL_CATEGORY = "category";
    public static final String COL_DESCRIPTION = "description";
    public static final String COL_STORE = "store";
    public static final String COL_PURCHASE_DATE = "purchase_date";

    private static final String SQL_CREATE = "CREATE TABLE " + TABLE_PRODUCTS + " (" +
            COL_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COL_NAME + " TEXT NOT NULL, " +
            COL_PRICE + " REAL NOT NULL, " +
            COL_EXPIRATION + " TEXT, " +
            COL_CATEGORY + " TEXT, " +
            COL_DESCRIPTION + " TEXT, " +
            COL_STORE + " TEXT, " +
            COL_PURCHASE_DATE + " TEXT, " +
            "used INTEGER DEFAULT 0" +
            ")";

    public ProductDbHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL("ALTER TABLE " + TABLE_PRODUCTS + " ADD COLUMN used INTEGER DEFAULT 0");
        }
    }

    /** Wstawia nowy produkt. Zwraca ID rekordu lub -1 przy błędzie. */
    public long insertProduct(Product p) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME, p.getName());
        cv.put(COL_PRICE, p.getPrice());
        cv.put(COL_EXPIRATION, p.getExpirationDate());
        cv.put(COL_CATEGORY, p.getCategory());
        cv.put(COL_DESCRIPTION, p.getDescription());
        cv.put(COL_STORE, p.getStore());
        cv.put(COL_PURCHASE_DATE, p.getPurchaseDate());
        cv.put("used", p.isUsed() ? 1 : 0);
        return db.insert(TABLE_PRODUCTS, null, cv);
    }

    /** Aktualizuje istniejący produkt po jego ID. Zwraca true jeśli co najmniej 1 rekord został zmieniony. */
    public boolean updateProduct(Product p) {
        if (p.getId() < 0) return false; // brak poprawnego klucza
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put(COL_NAME, p.getName());
        cv.put(COL_PRICE, p.getPrice());
        cv.put(COL_EXPIRATION, p.getExpirationDate());
        cv.put(COL_CATEGORY, p.getCategory());
        cv.put(COL_DESCRIPTION, p.getDescription());
        cv.put(COL_STORE, p.getStore());
        cv.put(COL_PURCHASE_DATE, p.getPurchaseDate());
        cv.put("used", p.isUsed() ? 1 : 0);
        int rows = db.update(TABLE_PRODUCTS, cv, COL_ID + "=?", new String[]{String.valueOf(p.getId())});
        return rows > 0;
    }

    /** Zwraca listę wszystkich produktów (ostatnio dodane pierwsze). */
    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_PRODUCTS, null, null, null, null, null, COL_ID + " DESC")) {
            int idxId = c.getColumnIndexOrThrow(COL_ID);
            int idxName = c.getColumnIndexOrThrow(COL_NAME);
            int idxPrice = c.getColumnIndexOrThrow(COL_PRICE);
            int idxExp = c.getColumnIndexOrThrow(COL_EXPIRATION);
            int idxCat = c.getColumnIndexOrThrow(COL_CATEGORY);
            int idxDesc = c.getColumnIndexOrThrow(COL_DESCRIPTION);
            int idxStore = c.getColumnIndexOrThrow(COL_STORE);
            int idxPurchase = c.getColumnIndexOrThrow(COL_PURCHASE_DATE);
            int idxUsed = c.getColumnIndex("used");
            while (c.moveToNext()) {
                boolean used = idxUsed >=0 && c.getInt(idxUsed) == 1;
                list.add(new Product(
                        c.getLong(idxId),
                        c.getString(idxName),
                        c.getDouble(idxPrice),
                        c.getString(idxExp),
                        c.getString(idxCat),
                        c.getString(idxDesc),
                        c.getString(idxStore),
                        c.getString(idxPurchase),
                        used
                ));
            }
        }
        return list;
    }

    /** Unikalne kategorie (do autocomplete). */
    public List<String> getDistinctCategories() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(true, TABLE_PRODUCTS, new String[]{COL_CATEGORY}, COL_CATEGORY + " IS NOT NULL AND " + COL_CATEGORY + " != ''", null, null, null, COL_CATEGORY + " COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                String val = c.getString(0);
                if (val != null) list.add(val);
            }
        }
        return list;
    }

    /** Unikalne sklepy (do autocomplete). */
    public List<String> getDistinctStores() {
        List<String> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(true, TABLE_PRODUCTS, new String[]{COL_STORE}, COL_STORE + " IS NOT NULL AND " + COL_STORE + " != ''", null, null, null, COL_STORE + " COLLATE NOCASE", null)) {
            while (c.moveToNext()) {
                String val = c.getString(0);
                if (val != null) list.add(val);
            }
        }
        return list;
    }

    /** Pobiera produkt po ID lub null jeśli brak. */
    public Product getProductById(long id) {
        SQLiteDatabase db = getReadableDatabase();
        try (Cursor c = db.query(TABLE_PRODUCTS, null, COL_ID + "=?", new String[]{String.valueOf(id)}, null, null, null)) {
            if (c.moveToFirst()) {
                int idxId = c.getColumnIndexOrThrow(COL_ID);
                int idxName = c.getColumnIndexOrThrow(COL_NAME);
                int idxPrice = c.getColumnIndexOrThrow(COL_PRICE);
                int idxExp = c.getColumnIndexOrThrow(COL_EXPIRATION);
                int idxCat = c.getColumnIndexOrThrow(COL_CATEGORY);
                int idxDesc = c.getColumnIndexOrThrow(COL_DESCRIPTION);
                int idxStore = c.getColumnIndexOrThrow(COL_STORE);
                int idxPurchase = c.getColumnIndexOrThrow(COL_PURCHASE_DATE);
                int idxUsed = c.getColumnIndex("used");
                boolean used = idxUsed >=0 && c.getInt(idxUsed) == 1;
                return new Product(
                        c.getLong(idxId),
                        c.getString(idxName),
                        c.getDouble(idxPrice),
                        c.getString(idxExp),
                        c.getString(idxCat),
                        c.getString(idxDesc),
                        c.getString(idxStore),
                        c.getString(idxPurchase),
                        used
                );
            }
        }
        return null;
    }

    public boolean deleteProduct(long id) {
        if (id < 0) return false;
        SQLiteDatabase db = getWritableDatabase();
        int rows = db.delete(TABLE_PRODUCTS, COL_ID + "=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }

    public long duplicateProduct(long id) {
        Product original = getProductById(id);
        if (original == null) return -1;
        Product copy = new Product(
                original.getName(),
                original.getPrice(),
                original.getExpirationDate(),
                original.getCategory(),
                original.getDescription(),
                original.getStore(),
                original.getPurchaseDate()
        ); // used domyślnie false
        return insertProduct(copy);
    }

    public boolean setUsed(long id, boolean used) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues cv = new ContentValues();
        cv.put("used", used ? 1 : 0);
        int rows = db.update(TABLE_PRODUCTS, cv, COL_ID + "=?", new String[]{String.valueOf(id)});
        return rows > 0;
    }
}
