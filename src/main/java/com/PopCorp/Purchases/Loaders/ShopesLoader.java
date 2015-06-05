package com.PopCorp.Purchases.Loaders;

import android.content.Context;
import android.database.Cursor;
import android.content.CursorLoader;

import com.PopCorp.Purchases.DataBase.DB;

public class ShopesLoader extends CursorLoader {

	private DB db;
	private String cityId;

	public ShopesLoader(Context context, DB db, String cityId) {
		super(context);
		this.db = db;
		this.cityId = cityId;
	}

	@Override
	public Cursor loadInBackground() {
		return db.getdata(DB.TABLE_SHOPES, DB.COLUMNS_SHOPES_WITH_CITY_ID, DB.KEY_SHOP_CITY_ID + "=" + cityId, null, null, null, null);
	}
}
