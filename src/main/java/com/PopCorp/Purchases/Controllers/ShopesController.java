package com.PopCorp.Purchases.Controllers;

import android.app.Dialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.view.View;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Adapters.ShopesAdapter;
import com.PopCorp.Purchases.Data.Shop;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Fragments.SalesFragment;
import com.PopCorp.Purchases.Fragments.ShopesFragment;
import com.PopCorp.Purchases.Loaders.ShopesLoader;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;

public class ShopesController implements LoaderManager.LoaderCallbacks<Cursor> {

	public static final int ID_FOR_CREATE_SHOPES_LOADER_FROM_DB = 3;
	public static final int ID_FOR_CREATE_SHOPES_LOADER_FROM_NET = 4;

	private ShopesFragment fragment;
	private AppCompatActivity context;
	private DB db;
	private SharedPreferences sPref;
	private SharedPreferences.Editor editor;

	private ShopesAdapter adapter;
	private ArrayList<Shop> shopes = new ArrayList<>();
	private String currentFilter = ShopesAdapter.FILTER_TYPE_ALL;

	public ShopesController(ShopesFragment fragment, AppCompatActivity context){
		this.fragment = fragment;
		this.context = context;
		sPref = PreferenceManager.getDefaultSharedPreferences(context);
		editor = sPref.edit();

		db = ((MainActivity) context).getDB();

		adapter = new ShopesAdapter(context, shopes, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Loader<Cursor> result = null;
		if (id == ID_FOR_CREATE_SHOPES_LOADER_FROM_DB){
			result = new ShopesLoader(context, db, sPref.getString(SD.PREFS_CITY, "1"));
		}
		return result;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor!=null){
			if (cursor.moveToFirst()){
				addShopFromCursor(cursor);
				while (cursor.moveToNext()){
					addShopFromCursor(cursor);
				}
			}
			cursor.close();
		}
		int checkedId = R.id.action_filter_all;
		if (shopes.size()>0){
			for (Shop shop : shopes){
				if (shop.isFavorite()){
					checkedId = R.id.action_filter_favorite;
				}
			}
			fragment.showMenu();
			filter(checkedId);
			fragment.checkMenuItem(checkedId);
		} else{
			fragment.hideMenu();
		}
		fragment.startLoaderFromNet();
	}
	
	private void addShopFromCursor(Cursor cursor) {
		Shop newShop = new Shop(cursor);
		if (!shopes.contains(newShop)){
			shopes.add(newShop);
		}
	}

	public void updateShopes(ArrayList<Shop> arrayWithNewShopesFromNet) {
		if (arrayWithNewShopesFromNet==null){
			if (shopes.size()==0){
				fragment.showEmpty(R.string.string_no_internet_connection, R.drawable.ic_no_internet);
			}
			return;
		}
		for (int i=0; i<arrayWithNewShopesFromNet.size(); i++){
			if (!shopes.contains(arrayWithNewShopesFromNet.get(i))){
				arrayWithNewShopesFromNet.get(i).putInDB(db);
				shopes.add(arrayWithNewShopesFromNet.get(i));
			} else{
                Shop shop = shopes.get(shopes.indexOf(arrayWithNewShopesFromNet.get(i)));
                if (!Arrays.equals(shop.getFields(), arrayWithNewShopesFromNet.get(i).getFields())){
                    shop.setFields(arrayWithNewShopesFromNet.get(i).getFields());
                    shop.updateInDB(db);
                }
            }
		}
		if (shopes.size()>0){
			fragment.showMenu();
			int checkedId = R.id.action_filter_all;
			if (currentFilter.equals(ShopesAdapter.FILTER_TYPE_FAVORITE)){
				checkedId = R.id.action_filter_favorite;
			}
			filter(checkedId);
			fragment.checkMenuItem(checkedId);
		}
	}
	
	public void openShop(int position){
		Fragment fragment = new SalesFragment();
		Bundle args = new Bundle();
		args.putString(SalesFragment.CURRENT_SHOP_ID_TO_SALES_FRAGMENT, adapter.getPublishItems().get(position).getId());
		args.putString(SalesFragment.CURRENT_SHOP_NAME_TO_SALES_FRAGMENT, adapter.getPublishItems().get(position).getName());
		fragment.setArguments(args);
		FragmentManager fragmentManager = context.getFragmentManager();
		FragmentTransaction transaction = fragmentManager.beginTransaction();
		transaction.replace(R.id.activity_main_content_frame, fragment, SalesFragment.TAG).commit();
	}

	public void filter(int itemId) {
		switch (itemId) {
			case R.id.action_filter_favorite: {
				adapter.getFilter().filter(ShopesAdapter.FILTER_TYPE_FAVORITE);
				currentFilter = ShopesAdapter.FILTER_TYPE_FAVORITE;
				break;
			}
			case R.id.action_filter_all: {
				adapter.getFilter().filter(ShopesAdapter.FILTER_TYPE_ALL);
				currentFilter = ShopesAdapter.FILTER_TYPE_ALL;
				break;
			}
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {

	}

	public ShopesAdapter getAdapter() {
		return adapter;
	}

	public void shopToFavorites(Shop shop){
		shop.setFavorite(true);
		shop.updateInDB(db);
	}

	public void shopFromFavorites(Shop shop){
		shop.setFavorite(false);
		shop.updateInDB(db);
		if (currentFilter.equals(ShopesAdapter.FILTER_TYPE_FAVORITE)) {
			filter(R.id.action_filter_favorite);
		}
	}

	public void showEmpty(int textEmpty, int drawableEmptyRes) {
		fragment.showEmpty(textEmpty, drawableEmptyRes);
	}

	public void showListView() {
		fragment.showListView();
	}

	public void showDialogWithCities() {
		ArrayList<String> cities = new ArrayList<>();
		final ArrayList<String> citiesIds = new ArrayList<>();
		Cursor cursor = db.getAllData(DB.TABLE_CITIES);
		if (cursor!=null){
			if (cursor.moveToFirst()){
				cities.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CITY_NAME)));
				citiesIds.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CITY_ID)));
				while (cursor.moveToNext()){
					cities.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CITY_NAME)));
					citiesIds.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CITY_ID)));
				}
			}
			cursor.close();
		}
		Dialog dialog = new MaterialDialog.Builder(context)
				.title(R.string.prefs_region)
				.items(cities.toArray(new String[cities.size()]))
				.itemsCallbackSingleChoice(citiesIds.indexOf(sPref.getString(SD.PREFS_CITY, "1")), new MaterialDialog.ListCallbackSingleChoice() {
					@Override
					public boolean onSelection(MaterialDialog dialog, View view, int which, CharSequence text) {
						editor.putString(SD.PREFS_CITY, citiesIds.get(which)).commit();
						editor.putBoolean(SD.PREFS_FIRST_SELECT_CITY, true).commit();
						fragment.startLoaderFromDb();
						return true;
					}
				})
				.positiveText(R.string.dialog_select)
				.negativeText(R.string.dialog_cancel)
				.build();
		dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}

	public void showHelp() {
		MaterialDialog dialog = new MaterialDialog.Builder(context)
				.title(R.string.string_help)
				.content(R.string.string_help_shopes)
				.positiveText(R.string.string_ok).build();
				dialog.setCanceledOnTouchOutside(false);
		dialog.show();
	}
}
