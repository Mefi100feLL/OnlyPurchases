package com.PopCorp.Purchases.Loaders;

import java.util.ArrayList;

import android.content.Context;
import android.os.Bundle;
import android.content.AsyncTaskLoader;

import com.PopCorp.Purchases.Data.Shop;

public class ShopesInternetLoader extends AsyncTaskLoader<ArrayList<Shop>> {

	private String cityId;

	public ShopesInternetLoader(Context ctx, Bundle args, String cityId) {
		super(ctx);
		this.cityId = cityId;
	}

	@Override
	public ArrayList<Shop> loadInBackground() {
		String page = LoaderShopesFromInternet.getFirstPage(cityId);
		if (page==null){
			return null;
		}
		return LoaderShopesFromInternet.getLinksForShops(page, cityId);
	}
}
