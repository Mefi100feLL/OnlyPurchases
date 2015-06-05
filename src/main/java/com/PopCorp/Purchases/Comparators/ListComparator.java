package com.PopCorp.Purchases.Comparators;

import java.util.ArrayList;
import java.util.Comparator;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.preference.PreferenceManager;

import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.PopCorp.Purchases.Data.ListItem;
import com.PopCorp.Purchases.DataBase.DB;

public class ListComparator implements Comparator<ListItem>{
	
	private Context context;
	private SharedPreferences sPref;
	
	public ListComparator(Context context){
		this.context = context;
		sPref = PreferenceManager.getDefaultSharedPreferences(context);
	}
	
	@Override
	public int compare(ListItem oneItem, ListItem twoItem) {
		if (sPref.getBoolean(SD.PREFS_REPLACE_BUYED, true)){
			if (oneItem.isBuyed() && !twoItem.isBuyed()){
				return 1;
			}
			if (!oneItem.isBuyed() && twoItem.isBuyed()){
				return -1;
			}
		}
		if (sPref.getBoolean(SD.PREFS_SHOW_CATEGORIES, true)){
			ArrayList<String> categories = getCategories();
			int indexOne = 100;
			int indexTwo = 100;
			if (categories.contains(oneItem.getCategory())){
				indexOne = categories.indexOf(oneItem.getCategory());
			}
			if (categories.contains(twoItem.getCategory())){
				indexTwo = categories.indexOf(twoItem.getCategory());
			}
			if (indexOne < indexTwo){
				return -1;
			}
			if (indexOne > indexTwo){
				return 1;
			}
		}
		if (oneItem.isImportant() && !twoItem.isImportant()){
			return -1;
		}
		if (!oneItem.isImportant() && twoItem.isImportant()){
			return 1;
		}
		String sort = sPref.getString(SD.PREFS_SORT_LIST_ITEM, context.getString(R.string.prefs_default_sort_listitem_one));
		if (sort.equals(context.getString(R.string.prefs_default_sort_listitem_one))){
			return oneItem.getName().compareToIgnoreCase(twoItem.getName());
		} else if (sort.equals(context.getString(R.string.prefs_default_sort_listitem_two))){
			return twoItem.getName().compareToIgnoreCase(oneItem.getName());
		} else if (sort.equals(context.getString(R.string.prefs_default_sort_listitem_three))){
			return 0;
		}
		return 0;
	}
	
	public ArrayList<String> getCategories() {
		ArrayList<String> result = new ArrayList<String>();
		DB db = new DB(context);
		db.open();
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
}