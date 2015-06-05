package com.PopCorp.Purchases.Loaders;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.content.CursorLoader;

import com.PopCorp.Purchases.DataBase.DB;

public class ProductsLoader extends CursorLoader{

	private DB db;

	public ProductsLoader(Context context, DB db) {
		super(context);
		this.db = db;
	}

	@Override
	public Cursor loadInBackground() {
		return db.getAllData(DB.TABLE_ALL_ITEMS);
	}
}
