package com.PopCorp.Purchases.Loaders;

import android.content.Context;
import android.database.Cursor;
import android.content.CursorLoader;

import com.PopCorp.Purchases.DataBase.DB;

public class SalesLoader extends CursorLoader{

	private DB db;
	private String shop;

	public SalesLoader(Context context, DB db, String shop) {
		super(context);
		this.db = db;
		this.shop = shop;
	}

	@Override
	public Cursor loadInBackground() {
		if (shop!=null){
			return db.getdata(DB.TABLE_SALES, DB.COLUMNS_SALES_WITH_ID, DB.KEY_SALES_SHOP + "='" + shop + "'", null, null, null, null);
		}
		return null;
	}
}
