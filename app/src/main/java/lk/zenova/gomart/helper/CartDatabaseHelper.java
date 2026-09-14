package lk.zenova.gomart.helper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import lk.zenova.gomart.model.CartItem;

public class CartDatabaseHelper extends SQLiteOpenHelper {

    private static final String DB_NAME    = "gomart_local.db";
    private static final int    DB_VERSION = 1;

    private static final String TABLE_CART        = "cart";
    private static final String COL_ID            = "id";
    private static final String COL_PRODUCT_ID    = "product_id";
    private static final String COL_QUANTITY      = "quantity";
    private static final String COL_SYNCED        = "synced";

    private static final String TABLE_SEARCHES    = "recent_searches";
    private static final String COL_SEARCH_QUERY  = "search_query";
    private static final String COL_SEARCH_TIME   = "timestamp";

    public CartDatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_CART + " (" +
                COL_ID         + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_PRODUCT_ID + " TEXT NOT NULL, " +
                COL_QUANTITY   + " INTEGER NOT NULL, " +
                COL_SYNCED     + " INTEGER DEFAULT 0" +
                ")");

        db.execSQL("CREATE TABLE " + TABLE_SEARCHES + " (" +
                COL_ID           + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                COL_SEARCH_QUERY + " TEXT NOT NULL, " +
                COL_SEARCH_TIME  + " INTEGER NOT NULL" +
                ")");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CART);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SEARCHES);
        onCreate(db);
    }


    public void addCartItem(String productId, int quantity) {
        SQLiteDatabase db = this.getWritableDatabase();

        Cursor cursor = db.query(TABLE_CART,
                new String[]{COL_ID, COL_QUANTITY},
                COL_PRODUCT_ID + "=?",
                new String[]{productId},
                null, null, null);

        if (cursor.moveToFirst()) {
            int existingQty = cursor.getInt(
                    cursor.getColumnIndexOrThrow(COL_QUANTITY));
            ContentValues values = new ContentValues();
            values.put(COL_QUANTITY, existingQty + quantity);
            values.put(COL_SYNCED, 0);
            db.update(TABLE_CART, values,
                    COL_PRODUCT_ID + "=?", new String[]{productId});
        } else {
            ContentValues values = new ContentValues();
            values.put(COL_PRODUCT_ID, productId);
            values.put(COL_QUANTITY, quantity);
            values.put(COL_SYNCED, 0);
            db.insert(TABLE_CART, null, values);
        }

        cursor.close();
        db.close();
    }

    public List<CartItem> getUnsyncedCartItems() {
        List<CartItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CART,
                null,
                COL_SYNCED + "=0",
                null, null, null, null);

        while (cursor.moveToNext()) {
            CartItem item = new CartItem();
            item.setProductId(cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_PRODUCT_ID)));
            item.setQuantity(cursor.getInt(
                    cursor.getColumnIndexOrThrow(COL_QUANTITY)));
            items.add(item);
        }

        cursor.close();
        db.close();
        return items;
    }

    public List<CartItem> getAllCartItems() {
        List<CartItem> items = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_CART,
                null, null, null, null, null, null);

        while (cursor.moveToNext()) {
            CartItem item = new CartItem();
            item.setProductId(cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_PRODUCT_ID)));
            item.setQuantity(cursor.getInt(
                    cursor.getColumnIndexOrThrow(COL_QUANTITY)));
            items.add(item);
        }

        cursor.close();
        db.close();
        return items;
    }

    public void markAllSynced() {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(COL_SYNCED, 1);
        db.update(TABLE_CART, values, null, null);
        db.close();
    }

    public void clearCart() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_CART, null, null);
        db.close();
    }

    public void saveSearch(String query) {
        if (query == null || query.trim().isEmpty()) return;
        SQLiteDatabase db = this.getWritableDatabase();

        db.delete(TABLE_SEARCHES, COL_SEARCH_QUERY + "=?",
                new String[]{query.trim()});

        ContentValues values = new ContentValues();
        values.put(COL_SEARCH_QUERY, query.trim());
        values.put(COL_SEARCH_TIME, System.currentTimeMillis());
        db.insert(TABLE_SEARCHES, null, values);
        db.close();

        trimSearchHistory();
    }

    public List<String> getRecentSearches() {
        List<String> searches = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();

        Cursor cursor = db.query(TABLE_SEARCHES,
                new String[]{COL_SEARCH_QUERY},
                null, null, null, null,
                COL_SEARCH_TIME + " DESC",
                "10");

        while (cursor.moveToNext()) {
            searches.add(cursor.getString(
                    cursor.getColumnIndexOrThrow(COL_SEARCH_QUERY)));
        }

        cursor.close();
        db.close();
        return searches;
    }

    private void trimSearchHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLE_SEARCHES +
                " WHERE " + COL_ID + " NOT IN " +
                "(SELECT " + COL_ID + " FROM " + TABLE_SEARCHES +
                " ORDER BY " + COL_SEARCH_TIME + " DESC LIMIT 10)");
        db.close();
    }

    public void clearSearchHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_SEARCHES, null, null);
        db.close();
    }
}