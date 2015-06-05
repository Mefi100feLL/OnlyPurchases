package com.PopCorp.Purchases.Controllers;

import java.util.ArrayList;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;

import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.Activities.ProductsActivity;
import com.PopCorp.Purchases.Adapters.ProductsAdapter;
import com.PopCorp.Purchases.Data.Product;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Loaders.ProductsLoader;

public class ProductsController implements LoaderCallbacks<Cursor>{
	
	public static final int ID_FOR_CREATE_LOADER_FROM_DB = 1;
	
	private ProductsActivity activity;
	private DB db;
	
	private ProductsAdapter adapter;
	private ArrayList<Product> items = new ArrayList<>();

	public ProductsController(ProductsActivity context, ArrayList<Product> itemsInList){
		this.activity = context;
		items.addAll(itemsInList);
		db = new DB(activity);
		openDB();

		adapter = new ProductsAdapter(activity, items, getCategories(), getColors());
	}	
	
	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		Loader<Cursor> result = null;
		if (id == ID_FOR_CREATE_LOADER_FROM_DB){
			result = new ProductsLoader(activity, db);
		}
		return result;
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
		if (cursor!=null){
			if (cursor.moveToFirst()){
				addListItemAllFromCursor(cursor);
				while (cursor.moveToNext()){
					addListItemAllFromCursor(cursor);
				}
			}
			cursor.close();
			if (items.size()==0){
				activity.showEmpty();
			} else{
				activity.showListView();
			}
			adapter.getFilter().filter(ProductsAdapter.FILTER_TYPE_NAMES);
		}
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		
	}
	
	public ArrayList<Product> apply(){
		ArrayList<Product> result = new ArrayList<>();
		for (Product product : items){
			if (product.isSelected()){
				result.add(product);
			}
		}
		return result;
	}
	
	private void addListItemAllFromCursor(Cursor cursor){
		Product newProduct = new Product(cursor);
		if (!items.contains(newProduct)) {
			items.add(newProduct);
		}
	}


	public void sort(int itemId) {
		switch (itemId){
		case R.id.action_sort_by_abc:{
			adapter.getFilter().filter(ProductsAdapter.FILTER_TYPE_NAMES);
			break;
		}
		case R.id.action_sort_by_category:{
			adapter.getFilter().filter(ProductsAdapter.FILTER_TYPE_CATEGORIES);
			break;
		}
		case R.id.action_sort_by_favorite:{
			adapter.getFilter().filter(ProductsAdapter.FILTER_TYPE_FAVORITE);
			break;
		}
		}
	}
	
	
	public ArrayList<String> getCategories() {
		ArrayList<String> result = new ArrayList<>();
		Cursor cursor = db.getAllData(DB.TABLE_CATEGORIES);
		if (cursor!=null){
			if (cursor.moveToFirst()){
				result.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CATEGS_NAME)));
				while (cursor.moveToNext()){
					result.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CATEGS_NAME)));
				}
			}
			cursor.close();
		}
		return result;
	}

	public ArrayList<Integer> getColors() {
		ArrayList<Integer> result = new ArrayList<>();
		Cursor cursor = db.getAllData(DB.TABLE_CATEGORIES);
		if (cursor!=null){
			if (cursor.moveToFirst()){
				result.add(cursor.getInt(cursor.getColumnIndex(DB.KEY_CATEGS_COLOR)));
				while (cursor.moveToNext()){
					result.add(cursor.getInt(cursor.getColumnIndex(DB.KEY_CATEGS_COLOR)));
				}
			}
			cursor.close();
		}
		return result;
	}
	
	
	public void closeDB(){
		if (db!=null){
			if (!db.isClosed()){
				db.close();
			}
		}
	}
	
	public void openDB(){
		if (db!=null){
			if (db.isClosed()){
				db.open();
			}
		}
	}
	/////////////////////////////////////////////////////////////
	public ArrayList<Product> getItems() {
		return items;
	}

	public ProductsAdapter getAdapter() {
		return adapter;
	}

	public void setAdapter(ProductsAdapter adapter) {
		this.adapter = adapter;
	}
}
