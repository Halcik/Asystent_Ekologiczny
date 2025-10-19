package com.example.asystent_ekologiczny;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

public class ProductDbHelper extends SQLiteOpenHelper {

    private static final String DB_NAME = "products.db";
    private static final int DB_VERSION = 1;

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
            COL_PURCHASE_DATE + " TEXT" +
            ")";

    public ProductDbHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PRODUCTS);
        onCreate(db);
    }

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
        return db.insert(TABLE_PRODUCTS, null, cv);
    }

    public List<Product> getAllProducts() {
        List<Product> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.query(TABLE_PRODUCTS, null, null, null, null, null, COL_ID + " DESC");
        try {
            int idxId = c.getColumnIndexOrThrow(COL_ID);
            int idxName = c.getColumnIndexOrThrow(COL_NAME);
            int idxPrice = c.getColumnIndexOrThrow(COL_PRICE);
            int idxExp = c.getColumnIndexOrThrow(COL_EXPIRATION);
            int idxCat = c.getColumnIndexOrThrow(COL_CATEGORY);
            int idxDesc = c.getColumnIndexOrThrow(COL_DESCRIPTION);
            int idxStore = c.getColumnIndexOrThrow(COL_STORE);
            int idxPurchase = c.getColumnIndexOrThrow(COL_PURCHASE_DATE);
            while (c.moveToNext()) {
                list.add(new Product(
                        c.getLong(idxId),
                        c.getString(idxName),
                        c.getDouble(idxPrice),
                        c.getString(idxExp),
                        c.getString(idxCat),
                        c.getString(idxDesc),
                        c.getString(idxStore),
                        c.getString(idxPurchase)
                ));
            }
        } finally {
            c.close();
        }
        return list;
    }
}

