package com.PopCorp.Purchases.Utilites;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.Set;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.Data.List;
import com.PopCorp.Purchases.Data.ListItem;
import com.PopCorp.Purchases.DataBase.DB;

public class ListWriter
{
	private SharedPreferences sPref;
	private Context context;

	public ListWriter(Context context){
		this.context = context;
		sPref = PreferenceManager.getDefaultSharedPreferences(context);
	}

	private String putCategory(String str, String category){
		if (!str.contains(category)){
			str += "\n" + category + ":\n";
		}
		return str;
	}

	public String write(String name, String currency, ArrayList<ListItem> array)
	{
		Collections.sort(array, new SortOnlyCategories());
		String resultString = name.replace("_", "") + "_" + currency + ":\n";

		String currentCategory = null;
		for (ListItem item : array){
			NumberFormat nf = NumberFormat.getInstance();
			nf.setGroupingUsed(false);

			if (!item.getCategory().equals(currentCategory)){
				if (item.getCategory().equals("")){
					putCategory(resultString, context.getString(R.string.string_no_category));
				}else{
					resultString = putCategory(resultString, item.getCategory());
				}
				currentCategory = item.getCategory();
			}

			resultString += item.getName().replace("_", " ") + "_";

			nf.setMaximumFractionDigits(3);
			resultString += nf.format(item.getCount()) + "_";

			resultString += item.getEdizm().replace("_", " ") + "_" + context.getString(R.string.string_po) + "_";

			nf.setMaximumFractionDigits(2);
			resultString += nf.format(item.getCoast());

			resultString += "_" + currency.replace("_", " ");

			if (!item.getComment().equals("")){
				resultString += "_" + item.getComment().replace("_", " ");
			}

			if (item.isBuyed()){
				resultString += "_" + context.getString(R.string.string_buyed);
			}

			if (!item.getShop().equals("")){
				resultString += "_" + context.getString(R.string.string_in) + "_" + item.getShop().replace("_", " ");
			}

			if (item.isImportant()){
				resultString += "_" + context.getString(R.string.string_important);
			}
			resultString+="\n";
		}
		return resultString;
	}

	public List read(DB db, Context context, String dataFromClip, boolean changeList){
		List list = new List(db, 0, null, String.valueOf(Calendar.getInstance().getTimeInMillis()), "", null);
		String[] lines = dataFromClip.split("\n");
		String currentCategory = "";
		for (String line : lines) {
			if ((line.endsWith(":")) && (line.contains("_"))) {
				if (changeList) {
					String[] strings = line.split("_");
					list.setName(strings[0]);
					list.setCurrency(strings[1].substring(0, strings[1].length() - 1));
				}
				continue;
			}
			if (line.equals("")) {
				continue;
			}
			if (line.endsWith(":") && !line.contains("_")) {
				currentCategory = line.substring(0, line.length() - 1);
				continue;
			}

			int position = 0;
			String[] paths = line.split("_");
			if (paths.length < 5) {
				continue;
			}
			String name = paths[position++];
			String count = paths[position++].replace(",", ".");
			String edizm = paths[position++];
			position++;
			String coast = paths[position++].replace(",", ".");

			position++;

			String comment = "";
			String buyed = "false";
			String shop = "";
			String important = "false";
			try {
				if (!paths[position].equals(context.getString(R.string.string_in))) {
					if (!paths[position].equals(context.getString(R.string.string_buyed))) {
						comment = paths[position++];
					}
				}

				if (paths[position].equals(context.getString(R.string.string_buyed))) {
					buyed = "true";
					position++;
				}

				if (paths[position].equals(context.getString(R.string.string_in))) {
					position++;
				}

				if (!paths[position].equals(context.getString(R.string.string_important))) {
					shop = paths[position++];
				}

				if (paths[position].equals(context.getString(R.string.string_important))) {
					important = "true";
				}
			} catch (Exception ignored){

			} finally {
				list.addNewItem(db, name, count, edizm, coast, currentCategory, shop, comment, buyed, important);
			}
		}
		return list;
	}

	private class SortOnlyCategories implements Comparator<ListItem>
	{
		private ArrayList<String> categories;

		public SortOnlyCategories(){
			categories = new ArrayList<>();
			Set<String> s = sPref.getStringSet("categories", new LinkedHashSet<String>());
			for (String i:new ArrayList<>(s)){
				for (int y=0;y<i.length();y++){
					if (i.charAt(y)=='!'){
						categories.add(i.substring(y+1,i.length()));
					}
				}
			}
		}

		public int compare(ListItem p1, ListItem p2)
		{
			if ((p1.getCategory().equals(p2.getCategory()))){
				return p1.getName().compareToIgnoreCase(p2.getName());
			}else{
				if (categories.contains(p1.getCategory()) && categories.contains(p2.getCategory())){
					if (categories.indexOf(p1.getCategory()) > categories.indexOf(p2.getCategory())){
						return 1;
					}else{
						return -1;
					}
				}else if (categories.contains(p1.getCategory())){
					return 1;
				}else if (categories.contains(p2.getCategory())){
					return -1;
				}else{
					return p1.getCategory().compareToIgnoreCase(p2.getCategory());
				}
			}
		}
	}
}
