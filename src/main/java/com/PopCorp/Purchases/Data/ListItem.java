package com.PopCorp.Purchases.Data;

import android.database.Cursor;

import java.math.BigDecimal;

import com.PopCorp.Purchases.DataBase.DB;

public class ListItem {

	private long id;
	private String datelist;
	private String name;
	private BigDecimal count;
	private String edizm;
	private BigDecimal coast;
	private String category;
	private String shop;
	private String comment;
	private boolean buyed;
	private boolean important;

	public ListItem(long id, String datelist, String name, String count, String edizm, String coast, String category, String shop, String comment, String buyed, String important){
		setId(id);
		setDatelist(datelist);
		setName(name);
		setCount(count);
		setEdizm(edizm);
		setCoast(coast);
		setCategory(category);
		setShop(shop);
		setComment(comment);
		setBuyed(buyed);
		setImportant(important);
	}
	
	public void update(DB db, String name, String count, String edizm, String coast, String category, String shop, String comment, String important){
		if (this.name.equals(name)){
			db.update(DB.TABLE_ALL_ITEMS, DB.COLUMNS_ALL_ITEMS, DB.KEY_ALL_ITEMS_NAME + "='" + name + "'", new String[] {name, count, edizm, coast, category, shop, comment, "true"});
		} else{
			Cursor cursor = db.getdata(DB.TABLE_ALL_ITEMS, DB.COLUMNS_ALL_ITEMS, DB.KEY_ALL_ITEMS_NAME + "='" + name + "'", null, null, null, null);
			if (cursor!=null){
				if (!cursor.moveToFirst()){
					db.addRec(DB.TABLE_ALL_ITEMS, DB.COLUMNS_ALL_ITEMS, new String[] {name, count, edizm, coast, category, shop, comment, "true"});
				}
				cursor.close();
			}
		}
		setName(name);
		setCount(count);
		setEdizm(edizm);
		setCoast(coast);
		setCategory(category);
		setShop(shop);
		setComment(comment);
		setImportant(important);
		db.update(DB.TABLE_ITEMS, DB.COLUMNS_ITEMS_WITHOUT_DATELIST, DB.KEY_ID + "=" + getId(), getFields());
	}

	public void changeBuyed(DB db){
		setBuyed(!isBuyed());
		db.update(DB.TABLE_ITEMS, DB.KEY_ID + "=" + getId(), DB.KEY_ITEMS_BUYED, isBuyedInString());
	}
	
	public void remove(DB db){
		db.deleteRows(DB.TABLE_ITEMS, DB.KEY_ID + "=" + getId());
	}
	
	public String[] getFields(){
		return new String[] {
				getName(),
				getCountInString(),
				getEdizm(),
				getCoastInString(),
				getCategory(),
				getShop(),
				getComment(),
				isBuyedInString(),
				isImportantInString()
		};
	}

	////////////////////////////////////////////////// getters and setters
	private long getId() {
		return id;
	}

	private void setId(long id) {
		this.id = id;
	}

	public String getDatelist() {
		return datelist;
	}

	private void setDatelist(String datelist) {
		if (datelist!=null){
			this.datelist = datelist;
		}else{
			this.datelist = "";
		}
	}

	public String getName() {
		return name;
	}

	private void setName(String name) {
		if (name!=null){
			this.name = name;
		}else{
			this.name = "";
		}
	}

	public BigDecimal getCount() {
		return count;
	}
	
	public String getCountInString() {
		return count.toString();
	}

	public void setCount(BigDecimal count) {
		if (count!=null){
			this.count = count;
		}else{
			this.count = new BigDecimal("0");
		}
	}

	private void setCount(String count) {
		try{
			this.count = new BigDecimal(count);
		} catch(Exception e){
			this.count = new BigDecimal("0");
		}
	}

	public String getEdizm() {
		return edizm;
	}

	private void setEdizm(String edizm) {
		if (edizm!=null){
			this.edizm = edizm;
		}else{
			this.edizm = "";
		}
	}

	public BigDecimal getCoast() {
		return coast;
	}
	
	public String getCoastInString() {
		return coast.toString();
	}

	private void setCoast(BigDecimal coast) {
		if (coast!=null){
			this.coast = coast;
		}else{
			this.coast = new BigDecimal("0");
		}
	}

	private void setCoast(String coast) {
		try{
			setCoast(new BigDecimal(coast));
		} catch(Exception e){
			setCoast(new BigDecimal("0"));
		}
	}

	public String getCategory() {
		return category;
	}

	private void setCategory(String category) {
		if (category!=null){
			this.category = category;
		}else{
			this.category = "";
		}
	}

	public String getShop() {
		return shop;
	}

	private void setShop(String shop) {
		if (shop!=null){
			this.shop = shop;
		}else{
			this.shop = "";
		}
	}

	public String getComment() {
		return comment;
	}

	private void setComment(String comment) {
		if (comment!=null){
			this.comment = comment;
		}else{
			this.comment = "";
		}
	}

	public boolean isBuyed() {
		return buyed;
	}
	
	public String isBuyedInString() {
		return String.valueOf(buyed);
	}

	private void setBuyed(boolean buyed) {
		this.buyed = buyed;
	}

	private void setBuyed(String buyed) {
		try{
			setBuyed(Boolean.valueOf(buyed));
		} catch(Exception e){
			setBuyed(false);
		}
	}

	public boolean isImportant() {
		return important;
	}
	
	public String isImportantInString() {
		return String.valueOf(important);
	}

	private void setImportant(boolean important) {
		this.important = important;
	}

	private void setImportant(String important) {
		try{
			setImportant(Boolean.valueOf(important));
		} catch(Exception e){
			setImportant(false);
		}
	}

	public Product getProduct() {
		return new Product(id, name, getCountInString(), edizm, getCoastInString(), category, shop, comment, "false");
	}
}
