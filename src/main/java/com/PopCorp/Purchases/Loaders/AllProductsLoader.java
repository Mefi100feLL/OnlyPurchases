package com.PopCorp.Purchases.Loaders;

import android.content.Context;
import android.content.CursorLoader;
import android.database.Cursor;

import com.PopCorp.Purchases.DataBase.DB;

public class AllProductsLoader extends CursorLoader {

    private DB db;

    public AllProductsLoader(Context context, DB db) {
        super(context);
        this.db = db;
    }

    @Override
    public Cursor loadInBackground() {
        return db.getAllData(DB.TABLE_ALL_ITEMS);
    }
}
