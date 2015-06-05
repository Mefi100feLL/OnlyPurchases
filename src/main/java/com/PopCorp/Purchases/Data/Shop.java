package com.PopCorp.Purchases.Data;

import com.PopCorp.Purchases.DataBase.DB;

import android.database.Cursor;

public class Shop {

	private String cityId;
	private String name;
	private String id;
	private String imageUrl;
	private String countSales;
	private boolean favorite;
	
	
	public Shop(String cityId, String id, String name, String imageUrl, String countSales, String favorite){
		setCityId(cityId);
		setId(id);
		setName(name);
		setImageUrl(imageUrl);
		setCountSales(countSales);
		setFavorite(Boolean.valueOf(favorite));
	}

	public Shop(Cursor cursor) {
		this(cursor.getString(cursor.getColumnIndex(DB.KEY_SHOP_CITY_ID)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SHOP_ID)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SHOP_NAME)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SHOP_IMAGE_URL)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SHOP_COUNT_SALES)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SHOP_FAVORITE)));
	}
	
	
	public void putInDB(DB db){
		db.addRec(DB.TABLE_SHOPES, DB.COLUMNS_SHOPES_WITH_CITY_ID, new String[]{cityId, id, name, imageUrl, countSales, String.valueOf(favorite)});
	}

	public void updateInDB(DB db){
		db.update(DB.TABLE_SHOPES, DB.COLUMNS_SHOPES_WITH_CITY_ID, DB.KEY_SHOP_CITY_ID + "='" + cityId + "' AND " + DB.KEY_SHOP_ID + "='" + id + "'", getFields());
	}
	
	@Override
	public boolean equals(Object object){
		try{
			Shop shop = (Shop) object;
			if (shop.getId().equals(getId())){
				if (shop.getCityId().equals(getCityId())){
					return true;
				}
			}
			return false;
		} catch(Exception e){
			return false;
		}
	}
	
	public String[] getFields(){
		return new String[]{cityId, id, name, imageUrl, countSales, String.valueOf(favorite)};
	}

	public void setFields(String[] fields) {
		setName(fields[2]);
		setImageUrl(fields[3]);
		setCountSales(fields[4]);
	}
	
	
	

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}

	public String getCountSales() {
		return countSales;
	}

	public void setCountSales(String countSales) {
		this.countSales = countSales;
	}


	public String getCityId() {
		return cityId;
	}

	public void setCityId(String cityId) {
		this.cityId = cityId;
	}

	public boolean isFavorite() {
		return favorite;
	}

	public void setFavorite(boolean favorite) {
		this.favorite = favorite;
	}


}
