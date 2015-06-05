package com.PopCorp.Purchases.Comparators;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import java.util.Comparator;

import com.PopCorp.Purchases.Data.List;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;

public class MenuComparator implements Comparator<List>{

	private Context context;
	private SharedPreferences sPref;

	public MenuComparator(Context context){
		this.context = context;
		sPref = PreferenceManager.getDefaultSharedPreferences(context);
	}

	@Override
	public int compare(List oneList, List twoList) {
		if (sPref.getString(SD.PREFS_SORT_LISTS, context.getString(R.string.prefs_default_sort_list_one)).equals(context.getString(R.string.prefs_default_sort_list_one))){
			return oneList.getName().compareToIgnoreCase(twoList.getName());
		} else{
			return twoList.getDatelist().compareToIgnoreCase(oneList.getDatelist());
		}
	}
}