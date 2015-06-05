package com.PopCorp.Purchases.Data;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.text.SpannableStringBuilder;
import android.text.Spanned;

import com.PopCorp.Purchases.Comparators.ListComparator;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.Receivers.AlarmReceiver;
import com.PopCorp.Purchases.Utilites.EllipsizeLineSpan;
import com.PopCorp.Purchases.Utilites.ListWriter;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.ListIterator;
import java.util.Locale;

public class List {

	public static final String FORMAT_FOR_DATE_ALARM = "HHmmssddMMyyyy";

	public static final int TYPE_OF_SENDING_LIST_TO_SMS = 1;
	public static final int TYPE_OF_SENDING_LIST_TO_EMAIL = 2;
	public static final int TYPE_OF_SENDING_LIST_AS_TEXT = 3;

	public static final int[] TYPES_OF_SENDING_LIST = {TYPE_OF_SENDING_LIST_TO_SMS, TYPE_OF_SENDING_LIST_TO_EMAIL, TYPE_OF_SENDING_LIST_AS_TEXT};

	private ArrayList<ListItem> items = new ArrayList<>();
	private long id;
	private String name;
	private String datelist;
	private String alarm = "";
	private String currency;

	private BigDecimal totalBuyed;
	private BigDecimal total;

	//////////////////////////////////////////// CONSTRUCTORS ///////////////////////////////////////////////
	public List(DB db, long id, String name, String datelist, String alarm, String currency){
		this.id = id;
		this.name = name;
		this.datelist = datelist;
		this.alarm = alarm;
		this.currency = currency;

		if (db!=null){
			loadItemsFromDB(db);
		}
	}

	public List(DB db, Cursor cursor){
		this(db,
				cursor.getLong(cursor.getColumnIndex(DB.KEY_ID)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_LISTS_NAME)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_LISTS_DATELIST)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_LISTS_ALARM)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_LISTS_CURRENCY)));
	}

	public List(DB db, String json){
		setDatelist(String.valueOf(Calendar.getInstance().getTimeInMillis()));
		items = new ArrayList<>();
		addItemsFromJSON(db, json);
		long id = db.addRec(DB.TABLE_LISTS, DB.COLUMNS_LISTS, new String[]{name, datelist, "", currency});
		setId(id);
	}

	public static List getListFromSms(DB db, Context context, String dataFromClip){
		List result = new ListWriter(context).read(db, context, dataFromClip, true);
		if (result.getName() != null && result.getCurrency() != null) {
			long id = db.addRec(DB.TABLE_LISTS, DB.COLUMNS_LISTS, new String[]{result.getName(), result.getDatelist(), "", result.getCurrency()});
			result.setId(id);
		} else{
			return null;
		}
		return result;
	}
	////////////////////////////////////////////CONSTRUCTORS ///////////////////////////////////////////////

	@Override
	public boolean equals(Object object){
		List list = (List) object;
		return getDatelist().equals(list.getDatelist());
	}

	public SpannableStringBuilder getSpannableStringBuilder(){
		SpannableStringBuilder spannableString = new SpannableStringBuilder("");
		for (ListItem item : items){
			spannableString.append(item.getName());
			if (item.isBuyed()){
				spannableString.setSpan(new EllipsizeLineSpan(true), spannableString.length()-item.getName().length() , spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}else{
				spannableString.setSpan(new EllipsizeLineSpan(false), spannableString.length()-item.getName().length() , spannableString.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
			}
			if (items.indexOf(item)!=items.size()-1){
				spannableString.append("\n");
			}
		}
		return spannableString;
	}

	private void loadItemsFromDB(DB db) {
		Cursor cursor = db.getdata(DB.TABLE_ITEMS, DB.COLUMNS_ITEMS_WITH_ID, DB.KEY_ITEMS_DATELIST + "='" + datelist + "'", null, null, null, null);
		if (cursor!=null){
			if (cursor.moveToFirst()){
				addListItem(cursor);
				while (cursor.moveToNext()){
					addListItem(cursor);
				}
			}
			cursor.close();
		}
	}

	private void addListItem(Cursor cursor){
		ListItem item = new ListItem( 
				cursor.getLong(cursor.getColumnIndex(DB.KEY_ID)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_DATELIST)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_NAME)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_COUNT)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_EDIZM)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_COAST)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_CATEGORY)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_SHOP)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_COMMENT)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_BUYED)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_ITEMS_IMPORTANT)));
		items.add(item);
	}

	public void remove(DB db){
		db.deleteRows(DB.TABLE_LISTS, DB.KEY_LISTS_DATELIST + "='" + datelist + "'");
		db.deleteRows(DB.TABLE_ITEMS, DB.KEY_ITEMS_DATELIST + "='" + datelist + "'");
	}

	public void rename(DB db, String newName){
		db.update(DB.TABLE_LISTS, DB.KEY_LISTS_DATELIST + "='" + datelist + "'", DB.KEY_LISTS_NAME, newName);
		setName(newName);
	}

	public void changeCurrency(DB db, String newCurrency){
		db.update(DB.TABLE_LISTS, DB.KEY_LISTS_DATELIST + "='" + datelist + "'", DB.KEY_LISTS_CURRENCY, newCurrency);
		setCurrency(newCurrency);
	}

	public void setAlarm(DB db, Context context, Date dateAlarm){
		SimpleDateFormat formatter = new SimpleDateFormat(FORMAT_FOR_DATE_ALARM, new Locale("ru"));
		cancelAlarm(db, context);
		db.update(DB.TABLE_LISTS, DB.KEY_LISTS_DATELIST + "='" + datelist + "'", DB.KEY_LISTS_ALARM, formatter.format(dateAlarm));
		AlarmReceiver alarmReceiver = new AlarmReceiver();
		alarmReceiver.setAlarm(context, dateAlarm.getTime(), name, datelist);
		setAlarm(formatter.format(dateAlarm));
	}

	public void cancelAlarm(DB db, Context context){
		db.update(DB.TABLE_LISTS, DB.KEY_LISTS_DATELIST + "='" + datelist + "'", DB.KEY_LISTS_ALARM, "");
		AlarmReceiver alarmReceiver = new AlarmReceiver();
		alarmReceiver.cancelAlarm(context, name, datelist);
		setAlarm("");
	}

	public void recoastTotals(){
		totalBuyed = new BigDecimal("0");
		total = new BigDecimal("0");
		for (ListItem item : items){
			if (item.isBuyed()){
				totalBuyed = totalBuyed.add(item.getCoast().multiply(item.getCount()));
			}
			total = total.add(item.getCoast().multiply(item.getCount()));
		}
	}

	public void send(Context context, int typeOfSending){
		sendItems(context, typeOfSending, items);
	}

	public void removeItems(DB db, ArrayList<ListItem> itemsForRemoving){
		for (ListItem item : itemsForRemoving){
			removeItem(db, item);
		}
	}

	private void removeItem(DB db, ListItem itemForRemove){
		itemForRemove.remove(db);
		items.remove(itemForRemove);
	}

	public ListItem addNewItem(DB db, ListItem item){
		return addNewItem(db, item.getName(), item.getCountInString(), item.getEdizm(), item.getCoastInString(), item.getCategory(), item.getShop(), item.getComment(), item.isBuyedInString(), item.isImportantInString());
	}

	public ListItem addNewItem(DB db, String name, String count, String edizm, String coast, String category, String shop, String comment, String buyed, String important){
		for (ListItem item : items){
			if (item.getName().equals(name)){
				return null;
			}
		}
		long id = db.addRec(DB.TABLE_ITEMS, DB.COLUMNS_ITEMS, new String[] {getDatelist(), name, count, edizm, coast, category, shop, comment, buyed, important});
		ListItem newItem = new ListItem(id, getDatelist(), name, count, edizm, coast, category, shop, comment, buyed, important);
		items.add(newItem);
		
		Cursor cursor = db.getdata(DB.TABLE_ALL_ITEMS, DB.COLUMNS_ALL_ITEMS, DB.KEY_ALL_ITEMS_NAME + "='" + name + "'", null, null, null, null);
		if (cursor!=null){
			if (cursor.moveToFirst()){
				db.update(DB.TABLE_ALL_ITEMS, DB.COLUMNS_ALL_ITEMS, DB.KEY_ALL_ITEMS_NAME + "='" + name + "'", new String[] {name, count, edizm, coast, category, shop, comment, "true"});
				cursor.close();
				return newItem;
			}
			cursor.close();
		}
		db.addRec(DB.TABLE_ALL_ITEMS, DB.COLUMNS_ALL_ITEMS, new String[] {name, count, edizm, coast, category, shop, comment, "true"});
		return newItem;
	}

	public void sendItems(Context context, int typeOfSending, ArrayList<ListItem> itemsForSending){
		String listInText = new ListWriter(context).write(name, currency, itemsForSending);
		Intent intent = null;

		switch (typeOfSending){
		case TYPE_OF_SENDING_LIST_TO_SMS : {
			intent = getIntentForSendListAsSMS(listInText);
			break;
		}
		case TYPE_OF_SENDING_LIST_TO_EMAIL : {
			intent = getIntentForSendListAsMail(listInText, itemsForSending);
			break;
		}
		case TYPE_OF_SENDING_LIST_AS_TEXT : {
			intent = getIntentForSendListAsText(listInText);
			break;
		}
		}
		if (intent != null){
			try{
				context.startActivity(Intent.createChooser(intent, context.getResources().getString(R.string.content_select_app_for_sending)));
			} catch(Exception ignored){}
		}
	}

	public ArrayList<ListItem> updateItems(DB db, ArrayList<Product> newArray){
		ArrayList<ListItem> result = new ArrayList<>();
		ListIterator<ListItem> iterator = items.listIterator();
		while (iterator.hasNext()){
			ListItem item = iterator.next();
			boolean finded = false;
			for (Product product : newArray){
				if (item.getName().equals(product.getName())){
					finded = true;
				}
			}
			if (!finded){
				item.remove(db);
				iterator.remove();
			}
		}
		for (Product product : newArray){
			boolean finded = false;
			for (ListItem item : items){
				if (item.getName().equals(product.getName())){
					finded = true;
					if (!item.getCountInString().equals(product.getCountInString())){
						item.update(db, item.getName(), product.getCountInString(), item.getEdizm(), item.getCoastInString(), item.getCategory(), item.getShop(), item.getComment(), item.isImportantInString());
						result.add(item);
					}
				}
			}
			if (!finded){
				addNewItem(db, product.getName(), product.getCountInString(), product.getEdizm(), product.getCoastInString(), product.getCategory(), product.getShop(), product.getComment(), "false", "false");
			}
		}
		return result;
	}

	public ArrayList<String> refreshFilterShops(){
		ArrayList<String> filterShops = new ArrayList<>();
		for (ListItem item : items){
			if (!item.getShop().equals("")){
				if (!filterShops.contains(item.getShop())){
					filterShops.add(item.getShop());
				}
			}
		}
		return filterShops;
	}

	private Intent getIntentForSendListAsText(String list){
		Intent intent = new Intent(Intent.ACTION_SEND);
		intent.setType("text/plain");
		intent.putExtra(Intent.EXTRA_SUBJECT, getName());
		intent.putExtra(Intent.EXTRA_TEXT, list);
		return intent;
	}

	private Intent getIntentForSendListAsSMS(String list){
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		String uriText = "sms:" + "?subject=" + Uri.encode(getName()) + "&body=" + Uri.encode(list);
		Uri uri = Uri.parse(uriText);
		intent.setData(uri);
		return intent;
	}

	private Intent getIntentForSendListAsMail(String list, ArrayList<ListItem> itemsForSending){
		Intent intent = new Intent(Intent.ACTION_SENDTO);
		String uriText = "mailto:" + "?subject=" + Uri.encode(getName()) + "&body=" + Uri.encode(list);
		Uri uri = Uri.parse(uriText);
		intent.setData(uri);
		try {
			File file = createFile(itemsForSending);
			if (file!=null){
				intent.putExtra(Intent.EXTRA_STREAM, Uri.parse("file://"+ file.getAbsolutePath()));
			}
		} catch (IOException ignored) {}
		return intent;
	}

	private File createFile(ArrayList<ListItem> itemsForSending) throws IOException{
		File directory = Environment.getExternalStorageDirectory();
		if (!isExternalStorageWritable()){
			return null;
		}
		directory = new File(directory.getAbsolutePath() + "/Purchases");
		if (!directory.exists()){
			if (!directory.mkdirs()){
				return null;
			}
		}
		File listFile = new File(directory.getAbsolutePath() + "/" + name + ".purchaseslist");
		FileWriter writer = new FileWriter(listFile);
		writer.append(itemsToJSON(itemsForSending));
		writer.flush();
		writer.close();
		return listFile;
	}

	static public boolean isExternalStorageWritable() {
		String state = Environment.getExternalStorageState();
		return Environment.MEDIA_MOUNTED.equals(state);
	}

	private static final String JSON_LIST_NAME = "JSON_LIST_NAME";
	private static final String JSON_LIST_CURRENCY = "JSON_LIST_CURRENCY";

	private static final String JSON_ITEM_NAME = "JSON_ITEM_NAME";
	private static final String JSON_ITEM_COUNT = "JSON_ITEM_COUNT";
	private static final String JSON_ITEM_EDIZM = "JSON_ITEM_EDIZM";
	private static final String JSON_ITEM_COAST = "JSON_ITEM_COAST";
	private static final String JSON_ITEM_CATEGORY = "JSON_ITEM_CATEGORY";
	private static final String JSON_ITEM_SHOP = "JSON_ITEM_SHOP";
	private static final String JSON_ITEM_COMMENT = "JSON_ITEM_COMMENT";
	private static final String JSON_ITEM_BUYED = "JSON_ITEM_BUYED";
	private static final String JSON_ITEM_IMPORTANT = "JSON_ITEM_IMPORTANT";

	private String itemsToJSON(ArrayList<ListItem> itemsForSending){
		JSONArray array = new JSONArray();
		JSONObject list = new JSONObject();
		try {
			list.put(JSON_LIST_NAME, name);
			list.put(JSON_LIST_CURRENCY, currency);
		} catch (JSONException ignored) {}
		array.put(list);
		for (ListItem item : itemsForSending){
			JSONObject object = new JSONObject();
			try {
				object.put(JSON_ITEM_NAME, item.getName());
				object.put(JSON_ITEM_COUNT, item.getCountInString());
				object.put(JSON_ITEM_EDIZM, item.getEdizm());
				object.put(JSON_ITEM_COAST, item.getCoastInString());
				object.put(JSON_ITEM_CATEGORY, item.getCategory());
				object.put(JSON_ITEM_SHOP, item.getShop());
				object.put(JSON_ITEM_COMMENT, item.getComment());
				object.put(JSON_ITEM_BUYED, item.isBuyedInString());
				object.put(JSON_ITEM_IMPORTANT, item.isImportantInString());
			} catch (JSONException e) {
				continue;
			}
			array.put(object);
		}
		return array.toString();
	}

	private void addItemsFromJSON(DB db, String json){
		try {
			JSONArray array = new JSONArray(json);
			JSONObject list = array.getJSONObject(0);
			setName(list.getString(JSON_LIST_NAME));
			setCurrency(list.getString(JSON_LIST_CURRENCY));

			for (int i=1; i<array.length(); i++){
				JSONObject object = array.getJSONObject(i);
				addNewItem(db,
						object.getString(JSON_ITEM_NAME),
						object.getString(JSON_ITEM_COUNT),
						object.getString(JSON_ITEM_EDIZM),
						object.getString(JSON_ITEM_COAST),
						object.getString(JSON_ITEM_CATEGORY),
						object.getString(JSON_ITEM_SHOP),
						object.getString(JSON_ITEM_COMMENT),
						object.getString(JSON_ITEM_BUYED),
						object.getString(JSON_ITEM_IMPORTANT));
			}
		} catch (JSONException ignored) {}
	}


	//////////////////////// setters and getters
	public String getDatelist() {
		return datelist;
	}

	private void setDatelist(String datelist) {
		this.datelist = datelist;
	}

	public String getAlarm() {
		return alarm;
	}

	private void setAlarm(String alarm) {
		this.alarm = alarm;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getId() {
		return id;
	}

	private void setId(long id) {
		this.id = id;
	}

	public ArrayList<ListItem> getItems() {
		return items;
	}

	public void setItems(ArrayList<ListItem> items) {
		this.items = items;
	}

	public String getCurrency() {
		return currency;
	}

	public void setCurrency(String currency) {
		this.currency = currency;
	}

	public String getTotalBuyed() {
		return totalBuyed.toString();
	}

	public String getTotal() {
		return total.toString();
	}

	public ArrayList<Product> getSelectedItems() {
		ArrayList<Product> result = new ArrayList<>();
		for (ListItem item : items){
			Product newProduct = item.getProduct();
			result.add(newProduct);
		}
		return result;
	}

	public void sort(Context context) {
		Collections.sort(items, new ListComparator(context));
	}
}
