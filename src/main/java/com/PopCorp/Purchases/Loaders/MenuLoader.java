package com.PopCorp.Purchases.Loaders;

import com.PopCorp.Purchases.DataBase.DB;

import android.content.Context;
import android.database.Cursor;
import android.os.Bundle;
import android.content.CursorLoader;

public class MenuLoader extends CursorLoader{

	private DB db;

	public MenuLoader(Context context, Bundle args, DB db) {
		super(context);
		this.db = db;
	}

	@Override
	public Cursor loadInBackground() {
		return db.getAllData(DB.TABLE_LISTS);
	}
}
