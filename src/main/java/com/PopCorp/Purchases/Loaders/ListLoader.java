package com.PopCorp.Purchases.Loaders;

import android.content.Context;
import android.database.Cursor;
import android.content.CursorLoader;

import com.PopCorp.Purchases.DataBase.DB;

public class ListLoader extends CursorLoader{

	private DB db;
	private String datelist;

	public ListLoader(Context context, DB db, String datelist) {
		super(context);
		this.db = db;
		this.datelist = datelist;
	}

	@Override
	public Cursor loadInBackground() {
		return db.getdata(DB.TABLE_ITEMS, DB.COLUMNS_ITEMS_WITH_ID, DB.KEY_ITEMS_DATELIST + "='" + datelist + "'", null, null, null, null);
	}
}
