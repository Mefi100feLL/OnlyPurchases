package com.PopCorp.Purchases.Data;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

import android.database.Cursor;

import com.PopCorp.Purchases.DataBase.DB;
import com.nostra13.universalimageloader.core.ImageLoader;

public class Sale {
	
	private long id;
	private String saleId;
	private String title;
	private String subTitle;
	private String coast;
	private String count;
	private String coastFor;
	private String imageUrl;
	private String imageId;
	private String shop;
	private Date periodBegin;
	private Date periodFinish;
	private SimpleDateFormat formatter;
	
	public Sale(long id, String saleId, String title, String subtitle, String coast, String count, String coastFor, String imageUrl, String imageId, String shop, String periodBegin, String periodFinish){
		formatter = new SimpleDateFormat("dd.MM.yyyy", new Locale("ru"));
		setId(id);
		setSaleId(saleId);
		setTitle(title);
		setSubTitle(subtitle);
		setCoast(coast);
		setCount(count);
		setCoastFor(coastFor);
		setImageUrl(imageUrl);
		setImageId(imageId);
		setShop(shop);
		setPeriodBegin(periodBegin);
		setPeriodFinish(periodFinish);
	}

	public Sale(Cursor cursor){
		this(cursor.getLong(cursor.getColumnIndex(DB.KEY_ID)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_ID_SALE)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_TITLE)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_SUBTITLE)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_COAST)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_COUNT)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_COAST_FOR)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_IMAGE_URL)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_ID_IMAGE)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_SHOP)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_PERIOD_BEGIN)),
				cursor.getString(cursor.getColumnIndex(DB.KEY_SALES_PERIOD_FINISH))
		);
	}
	
	
	public void remove(DB db){
		db.deleteRows(DB.TABLE_SALES, DB.KEY_ID + "=" + getId());
		ImageLoader.getInstance().getDiskCache().remove(imageUrl);
	}
	
	@Override
	public boolean equals(Object object){
		try{
			Sale sale = (Sale) object;
			return sale.getSaleId().equals(getSaleId());
		} catch(Exception e){
			return false;
		}
	}
	
	public void putInDB(DB db){
		long id = db.addRec(DB.TABLE_SALES, DB.COLUMNS_SALES, getFields());
		setId(id);
	}

	public void updateInDB(DB db){
		if (id!=-1){
			db.update(DB.TABLE_SALES, DB.COLUMNS_SALES, DB.KEY_ID + "=" + getId(), getFields());
		} else{
			putInDB(db);
		}
	}

	public void setFields(String[] fields){
		setTitle(fields[1]);
		setSubTitle(fields[2]);
		setCoast(fields[3]);
		setCount(fields[4]);
		setCoastFor(fields[5]);
		setImageUrl(fields[6]);
		setImageId(fields[7]);
		setShop(fields[8]);
		setPeriodBegin(fields[9]);
		setPeriodFinish(fields[10]);
	}
	
	public String[] getFields(){
		return new String[] {
				getSaleId(),
				getTitle(),
				getSubTitle(),
				getCoast(),
				getCount(),
				getCoastFor(),
				getImageUrl(),
				getImageId(),
				getShop(),
				getPeriodBeginInString(),
				getPeriodFinishInString()
		};
	}
	
	////////////////////////////////////////// SETTERS AND GETTERS ///////////////////////////////////////////////
	public long getId() {
		return id;
	}


	public void setId(long id) {
		this.id = id;
	}


	public String getSaleId() {
		return saleId;
	}


	public void setSaleId(String saleId) {
		this.saleId = saleId;
	}


	public String getTitle() {
		return title;
	}


	public void setTitle(String title) {
		if (!title.equals("") || this.title==null) {
			title = title.replaceAll("<br />", " ");
			title = title.replaceAll("[\\r|\\n]", " ");
			title = title.replaceAll("&quot;", "\"");
			this.title = title;
		}
	}


	public String getSubTitle() {
		return subTitle;
	}


	public void setSubTitle(String subTitle) {
		if (!subTitle.equals("") || this.subTitle==null) {
			subTitle = subTitle.replaceAll("<br />", " ");
			this.subTitle = subTitle;
		}
	}


	public String getCoast() {
		return coast;
	}

	public void setCoast(String coast) {
		if (!coast.equals("") || this.coast==null) {
			this.coast = coast;
		}
	}


	public String getCount() {
		return count;
	}

	public void setCount(String count) {
		if (!count.equals("") || this.count==null) {
			this.count = count;
		}
	}


	public String getCoastFor() {
		return coastFor;
	}


	public void setCoastFor(String coastFor) {
		if (!coastFor.equals("") || this.coastFor==null) {
			this.coastFor = coastFor;
		}
	}
	

	public String getShop() {
		return shop;
	}


	public void setShop(String shop) {
		if (!shop.equals("") || this.shop==null) {
			this.shop = shop;
		}
	}
	

	public String getImageId() {
		return imageId;
	}
	

	public void setImageId(String imageId) {
		if (!imageId.equals("") || this.imageId==null) {
			this.imageId = imageId;
		}
	}

	public String getPeriodBeginInString() {
		return formatter.format(periodBegin);
	}

	public void setPeriodBegin(Date periodBegin) {
		this.periodBegin = periodBegin;
	}
	
	public void setPeriodBegin(String periodBegin) {
		if (!periodBegin.equals("") || this.periodBegin==null) {
			Calendar periodB = Calendar.getInstance();
			try {
				periodB.setTime(formatter.parse(periodBegin));
			} catch (ParseException ignored) {}
			setPeriodBegin(periodB.getTime());
		}
	}

	public Date getPeriodFinish() {
		return periodFinish;
	}
	
	public String getPeriodFinishInString() {
		return formatter.format(periodFinish);
	}

	public void setPeriodFinish(Date periodFinish) {
		this.periodFinish = periodFinish;
	}
	
	public void setPeriodFinish(String periodFinish) {
		if (!periodFinish.equals("") || this.periodFinish==null) {
			Calendar periodF = Calendar.getInstance();
			try {
				periodF.setTime(formatter.parse(periodFinish));
			} catch (ParseException ignored) {}
			setPeriodFinish(periodF.getTime());
		}
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		if (!imageUrl.equals("") || this.imageUrl==null) {
			this.imageUrl = imageUrl;
		}
	}

}
