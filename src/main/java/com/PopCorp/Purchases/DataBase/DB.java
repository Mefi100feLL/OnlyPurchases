package com.PopCorp.Purchases.DataBase;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

public class DB {
	public static final String KEY_ID = "_id";

	public static final String TABLE_LISTS = "Lists";
	public static final String TABLE_ITEMS = "Items";
	public static final String TABLE_ALL_ITEMS = "AllItems";
	public static final String TABLE_FAVORITE_ITEMS = "Favorite";
	public static final String TABLE_SALES = "Sales";
	public static final String TABLE_CITIES = "Cities";
	public static final String TABLE_SHOPES = "Shopes";
	public static final String TABLE_CATEGORIES = "Categories";


	//////////////////////////////////////////////////////// CATEGORIES ///////////////////////////////////////////////////////
	public static final String KEY_CATEGS_NAME = "name";
	public static final String KEY_CATEGS_COLOR = "color";

	public static final String[] COLUMNS_CATEGS_WITH_ID = new String[] {KEY_ID, KEY_CATEGS_NAME, KEY_CATEGS_COLOR};

	public static final String[] COLUMNS_CATEGS = new String[] {KEY_CATEGS_NAME, KEY_CATEGS_COLOR};

	public static final String CREATE_TABLE_CATEGS = "CREATE TABLE IF NOT EXISTS " + TABLE_CATEGORIES + 
			"( " + KEY_ID + " integer primary key autoincrement, " + KEY_CATEGS_NAME + " text, " + KEY_CATEGS_COLOR + " integer);";


	//////////////////////////////////////////////////////// LISTS ///////////////////////////////////////////////////////
	public static final String KEY_LISTS_NAME = "name";
	public static final String KEY_LISTS_DATELIST = "date";
	public static final String KEY_LISTS_ALARM = "datealarm";
	public static final String KEY_LISTS_CURRENCY = "currency";

	public static final String[] COLUMNS_LISTS_WITH_ID = new String[] {KEY_ID, KEY_LISTS_NAME, KEY_LISTS_DATELIST, KEY_LISTS_ALARM, KEY_LISTS_CURRENCY};

	public static final String[] COLUMNS_LISTS = new String[] {KEY_LISTS_NAME, KEY_LISTS_DATELIST, KEY_LISTS_ALARM, KEY_LISTS_CURRENCY};

	public static final String CREATE_TABLE_LISTS = "CREATE TABLE IF NOT EXISTS " + TABLE_LISTS + 
			"( " + KEY_ID + " integer primary key autoincrement, " + KEY_LISTS_NAME + " text, " + KEY_LISTS_DATELIST + " text, " + 
			KEY_LISTS_ALARM + " text, " + KEY_LISTS_CURRENCY + " text);";


	///////////////////////////////////////////////////////////// ITEMS ////////////////////////////////////////////////
	public static final String KEY_ITEMS_DATELIST = "date";
	public static final String KEY_ITEMS_NAME = "name";
	public static final String KEY_ITEMS_COUNT = "count";
	public static final String KEY_ITEMS_EDIZM = "edizm";
	public static final String KEY_ITEMS_COAST = "coast";
	public static final String KEY_ITEMS_CATEGORY = "category";
	public static final String KEY_ITEMS_SHOP = "shop";
	public static final String KEY_ITEMS_COMMENT = "comment";
	public static final String KEY_ITEMS_BUYED = "buyed";
	public static final String KEY_ITEMS_IMPORTANT = "important";

	public static final String[] COLUMNS_ITEMS_WITHOUT_DATELIST = new String[] {KEY_ITEMS_NAME, KEY_ITEMS_COUNT, KEY_ITEMS_EDIZM,
		KEY_ITEMS_COAST, KEY_ITEMS_CATEGORY, KEY_ITEMS_SHOP, KEY_ITEMS_COMMENT, KEY_ITEMS_BUYED, KEY_ITEMS_IMPORTANT};

	public static final String[] COLUMNS_ITEMS = new String[] {KEY_ITEMS_DATELIST, KEY_ITEMS_NAME, KEY_ITEMS_COUNT, KEY_ITEMS_EDIZM,
		KEY_ITEMS_COAST, KEY_ITEMS_CATEGORY, KEY_ITEMS_SHOP, KEY_ITEMS_COMMENT, KEY_ITEMS_BUYED, KEY_ITEMS_IMPORTANT};

	public static final String[] COLUMNS_ITEMS_WITH_ID = new String[] {KEY_ID, KEY_ITEMS_DATELIST, KEY_ITEMS_NAME, KEY_ITEMS_COUNT, KEY_ITEMS_EDIZM,
		KEY_ITEMS_COAST, KEY_ITEMS_CATEGORY, KEY_ITEMS_SHOP, KEY_ITEMS_COMMENT, KEY_ITEMS_BUYED, KEY_ITEMS_IMPORTANT};

	public static final String CREATE_TABLE_ITEMS = "CREATE TABLE IF NOT EXISTS " + TABLE_ITEMS + 
			"( " + KEY_ID + " integer primary key autoincrement, " + KEY_ITEMS_DATELIST + " text, " + KEY_ITEMS_NAME + " text, " + 
			KEY_ITEMS_COUNT + " integer, " + KEY_ITEMS_EDIZM + " text, " + KEY_ITEMS_COAST + " integer, " + 
			KEY_ITEMS_CATEGORY + " text, " + KEY_ITEMS_SHOP + " text, " + KEY_ITEMS_COMMENT + " text, " + 
			KEY_ITEMS_BUYED + " boolean, " + KEY_ITEMS_IMPORTANT + " boolean);";

	////////////////////////////////////////////////////////////// ALL ITEMS ///////////////////////////////////////////////////
	public static final String KEY_ALL_ITEMS_NAME = "name";
	public static final String KEY_ALL_ITEMS_COUNT = "count";
	public static final String KEY_ALL_ITEMS_EDIZM = "edizm";
	public static final String KEY_ALL_ITEMS_COAST = "coast";
	public static final String KEY_ALL_ITEMS_CATEGORY = "category";
	public static final String KEY_ALL_ITEMS_SHOP = "shop";
	public static final String KEY_ALL_ITEMS_COMMENT = "comment";
	public static final String KEY_ALL_ITEMS_FAVORITE = "favorite";

	public static final String[] COLUMNS_ALL_ITEMS = new String[] {KEY_ALL_ITEMS_NAME, KEY_ALL_ITEMS_COUNT, KEY_ALL_ITEMS_EDIZM,
		KEY_ALL_ITEMS_COAST, KEY_ALL_ITEMS_CATEGORY, KEY_ALL_ITEMS_SHOP, KEY_ALL_ITEMS_COMMENT, KEY_ALL_ITEMS_FAVORITE};

	public static final String CREATE_TABLE_ALL_ITEMS = "CREATE TABLE IF NOT EXISTS " + TABLE_ALL_ITEMS + 
			"( " + KEY_ID + " integer primary key autoincrement, " + KEY_ALL_ITEMS_NAME + " text, " + 
			KEY_ALL_ITEMS_COUNT + " integer, " + KEY_ALL_ITEMS_EDIZM + " text, " + KEY_ALL_ITEMS_COAST + " integer, " + 
			KEY_ALL_ITEMS_CATEGORY + " text, " + KEY_ALL_ITEMS_SHOP + " text, " + KEY_ALL_ITEMS_COMMENT + " text, " + KEY_ALL_ITEMS_FAVORITE + " boolean);";

	//////////////////////////////////////////////////////////// FAVORITE ITEMS ////////////////////////////////////////////////////////////
	public static final String KEY_FAVORITE_ITEMS_NAME = "name";
	public static final String KEY_FAVORITE_ITEMS_COUNT = "count";
	public static final String KEY_FAVORITE_ITEMS_EDIZM = "edizm";
	public static final String KEY_FAVORITE_ITEMS_COAST = "coast";
	public static final String KEY_FAVORITE_ITEMS_CATEGORY = "category";
	public static final String KEY_FAVORITE_ITEMS_SHOP = "shop";
	public static final String KEY_FAVORITE_ITEMS_COMMENT = "comment";

	public static final String[] COLUMNS_FAVORITE_ITEMS = new String[] {KEY_FAVORITE_ITEMS_NAME, KEY_FAVORITE_ITEMS_COUNT, KEY_FAVORITE_ITEMS_EDIZM,
		KEY_FAVORITE_ITEMS_COAST, KEY_FAVORITE_ITEMS_CATEGORY, KEY_FAVORITE_ITEMS_SHOP, KEY_FAVORITE_ITEMS_COMMENT};

	public static final String CREATE_TABLE_FAVORITE_ITEMS = "CREATE TABLE IF NOT EXISTS " + TABLE_FAVORITE_ITEMS + 
			"( " + KEY_ID + " integer primary key autoincrement, " + KEY_FAVORITE_ITEMS_NAME + " text, " + 
			KEY_FAVORITE_ITEMS_COUNT + " integer, " + KEY_FAVORITE_ITEMS_EDIZM + " text, " + KEY_FAVORITE_ITEMS_COAST + " integer, " + 
			KEY_FAVORITE_ITEMS_CATEGORY + " text, " + KEY_FAVORITE_ITEMS_SHOP + " text, " + KEY_FAVORITE_ITEMS_COMMENT + " text);";


	///////////////////////////////////////////////////////////// SALES /////////////////////////////////////////////////////////////////
	public static final String KEY_SALES_ID_SALE = "id_sale";
	public static final String KEY_SALES_TITLE = "title";
	public static final String KEY_SALES_SUBTITLE = "subtitle";
	public static final String KEY_SALES_COAST = "coast";
	public static final String KEY_SALES_COUNT = "count";
	public static final String KEY_SALES_COAST_FOR = "coast_for";
	public static final String KEY_SALES_IMAGE_URL = "image_url";
	public static final String KEY_SALES_ID_IMAGE = "id_image";
	public static final String KEY_SALES_SHOP = "shop";
	public static final String KEY_SALES_PERIOD_BEGIN = "period_begin";
	public static final String KEY_SALES_PERIOD_FINISH = "period_finish";

	public static final String[] COLUMNS_SALES = new String[] {KEY_SALES_ID_SALE, KEY_SALES_TITLE, KEY_SALES_SUBTITLE, KEY_SALES_COAST,
		KEY_SALES_COUNT, KEY_SALES_COAST_FOR, KEY_SALES_IMAGE_URL, KEY_SALES_ID_IMAGE, KEY_SALES_SHOP, KEY_SALES_PERIOD_BEGIN, KEY_SALES_PERIOD_FINISH};

	public static final String[] COLUMNS_SALES_WITH_ID = new String[] {KEY_ID, KEY_SALES_ID_SALE, KEY_SALES_TITLE, KEY_SALES_SUBTITLE, KEY_SALES_COAST,
		KEY_SALES_COUNT, KEY_SALES_COAST_FOR, KEY_SALES_IMAGE_URL, KEY_SALES_ID_IMAGE, KEY_SALES_SHOP, KEY_SALES_PERIOD_BEGIN, KEY_SALES_PERIOD_FINISH};

	public static final String CREATE_TABLE_SALES = "CREATE TABLE IF NOT EXISTS " + TABLE_SALES + 
			"( " + KEY_ID + " integer primary key autoincrement, " + KEY_SALES_ID_SALE + " text, " + KEY_SALES_TITLE + " text, " + 
			KEY_SALES_SUBTITLE + " text, " + KEY_SALES_COAST + " text, " + KEY_SALES_COUNT + " text, " + 
			KEY_SALES_COAST_FOR + " text, " + KEY_SALES_IMAGE_URL + " text, " + KEY_SALES_ID_IMAGE + " text, " + 
			KEY_SALES_SHOP + " text, " + KEY_SALES_PERIOD_BEGIN + " text, " + KEY_SALES_PERIOD_FINISH + " text);";

	////////////////////////////////////////////////////////////// CITIES //////////////////////////////////////////////////////////////
	public static final String KEY_CITY_NAME = "name";
	public static final String KEY_CITY_ID = "id";

	public static final String CREATE_TABLE_CITIES = "CREATE TABLE IF NOT EXISTS " + TABLE_CITIES + 
			"( " + KEY_ID + " integer primary key autoincrement, " + KEY_CITY_NAME + " text, " + KEY_CITY_ID + " integer);";

	///////////////////////////////////////////////////////////// SHOPES //////////////////////////////////////////////////////
	public static final String KEY_SHOP_CITY_ID = "id_city";
	public static final String KEY_SHOP_ID = "id";
	public static final String KEY_SHOP_NAME = "name";
	public static final String KEY_SHOP_IMAGE_URL = "image_url";
	public static final String KEY_SHOP_COUNT_SALES = "count_sales";
	public static final String KEY_SHOP_FAVORITE = "favorite";

	public static final String[] COLUMNS_SHOPES = new String[] {KEY_SHOP_ID, KEY_SHOP_NAME, KEY_SHOP_IMAGE_URL, KEY_SHOP_COUNT_SALES, KEY_SHOP_FAVORITE};

	public static final String[] COLUMNS_SHOPES_WITH_CITY_ID = new String[] {KEY_SHOP_CITY_ID , KEY_SHOP_ID, KEY_SHOP_NAME, KEY_SHOP_IMAGE_URL, KEY_SHOP_COUNT_SALES, KEY_SHOP_FAVORITE};

	public static final String CREATE_TABLE_SHOPES = "CREATE TABLE IF NOT EXISTS " + TABLE_SHOPES + 
			"( " + KEY_ID + " integer primary key autoincrement, " + KEY_SHOP_CITY_ID + " integer, " + KEY_SHOP_ID + " integer, " + KEY_SHOP_NAME + " text, "
			+ KEY_SHOP_IMAGE_URL + " text, " + KEY_SHOP_COUNT_SALES + " text, " + KEY_SHOP_FAVORITE + " text);";

	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	private static final String DB_NAME = "PopCorp.Purchases.DB";
	private static final int DB_VERSION = 4;
	private Context context;

	private DBHelper DBHelper;
	private SQLiteDatabase db;
	private boolean closed;

	public DB(Context ctx) {
		context = ctx;
		closed=true;
	}

	public boolean isClosed(){
		return closed;
	}

	public void open() {//��������� ��
		DBHelper = new DBHelper(context, DB_NAME, null, DB_VERSION);
		try{
			db = DBHelper.getWritableDatabase();
		}catch(SQLiteException e){
			db = DBHelper.getReadableDatabase();
		}
		closed=false;
	}

	public void close() {
		if (DBHelper!=null){
			DBHelper.close();
		}
		closed=true;
	}

	public Cursor getAllData(String table) {
		try{
			return db.query(table, null, null, null, null, null, null);
		} catch(SQLiteException e){
			return null;
		}
	}

	public void changeRec(String table, String column,int id, String txt) {//�������� ������
		ContentValues cv = new ContentValues();
		cv.put(column, txt);
		try{
			db.update(table, cv, "_id = " + id, null);
		}catch(SQLiteException e){}
	}

	public long addRec(String table, String[] columns, String[] values) {//�������� ������
		ContentValues cv = new ContentValues();
		for (int i=0;i<columns.length;i++){
			cv.put(columns[i].toString(), values[i].toString());
		}
		try{
			return db.insert(table, null, cv);
		}catch(SQLiteException e){
			return -1;
		}
	}

	public void createtable(String table, String[] name, String[] type){//������� �������, ���� � ���
		String create="CREATE TABLE IF NOT EXISTS " + table + "( _id integer primary key autoincrement";
		for (int i=0;i<name.length;i++){
			create=create + ", " + name[i].toString() + " " + type[i].toString();
		}
		create=create+");";

		try{
			db.execSQL(create);
		}catch(SQLiteException e){}
	}
	//��������� ������ �� id
	public void updatedata(String table,String column, int id, String text){
		try{
			db.execSQL("UPDATE " + table + " SET " + column + "='" + text + "' WHERE _id=" + String.valueOf(id) + ";");
		}catch(SQLiteException e){}
	}
	//�������� ������ �� ������� �� �������
	public Cursor getdata(String table, String[] columns, String selection, String[] selectionArgs, String groupBy, String having, String orderBy){
		try{
			return db.query(table,columns,selection,selectionArgs,groupBy,having,orderBy);
		}catch(SQLiteException e){
			return null;
		}
	}

	public Cursor getdata(String table, String[] columns, String selection){
		try{
			return db.query(table,columns,selection,null,null,null,null);
		}catch(SQLiteException e){
			return null;
		}
	}
	//������� ������ �� �������
	public void deleteRows(String table, String uslovie){
		try{
			if (uslovie==null){
				db.execSQL("DELETE FROM " + table);
			}else{
				db.execSQL("DELETE FROM " + table + " WHERE " + uslovie);
			}
		}catch(SQLiteException e){}
	}
	//��������� ������ �� �������
	public void update(String table, String uslovie, String column, String value) throws SQLException{
		db.execSQL("UPDATE " + table + " SET " + column + "='" + value + "' WHERE " + uslovie + ";");
	}

	public int update(String table, ContentValues values, String whereClause, String[] whereArgs){
		return db.update(table, values, whereClause, whereArgs);
	}

	public int update(String table, String[] columns, String uslovie, String[] values){
		ContentValues cv = new ContentValues();
		for (int i=0;i<columns.length;i++){
			cv.put(columns[i].toString(), values[i].toString());
		}
		return db.update(table, cv, uslovie, null);
	}

	//��������������� �������
	public void renametable(String lastname, String nextname){
		try{
			db.execSQL("ALTER TABLE " + lastname + " RENAME TO "+ nextname+ ";");
		}catch(SQLiteException e){}
	}
	//������� �������
	public void deleteall(String table){
		try{
			db.execSQL("DELETE FROM " + table +";");
		}catch(SQLiteException e){}
	}
	//������� �������
	public void removetable(String table){
		try{
			db.execSQL("DROP TABLE "+ table+";");
		}catch(SQLiteException e){}
	}

	public void insert(String table, String nullColumnHack, ContentValues values){
		db.insert(table, nullColumnHack, values);
	}

	static public String trans(String s){
		String res="a";
		for (byte i=0;i<s.length();i++){
			if (s.charAt(i)==' '){
				res+="_";
			}else{
				res+=s.charAt(i);
			}
		}
		return res;
	}

	static public String untrans(String s){
		String res="";
		for (byte i=0;i<s.length();i++){
			if (s.charAt(i)=='_'){
				res+=" ";
			}else{
				res+=s.charAt(i);
			}
		}
		return res.substring(1);
	}

	public Context getContext() {
		return context;
	}

}
