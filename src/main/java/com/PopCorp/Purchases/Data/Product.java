package com.PopCorp.Purchases.Data;

import java.math.BigDecimal;

import com.PopCorp.Purchases.DataBase.DB;

import android.database.Cursor;
import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable {

    private long id;
    private String name;
    private BigDecimal count;
    private String edizm;
    private BigDecimal coast;
    private String category;
    private String shop;
    private String comment;
    private boolean favorite = false;
    private boolean selected = false;

    public Product(long id, String name, String count, String edizm, String coast, String category, String shop, String comment, String favorite) {
        this.id = id;
        setName(name);
        setCount(count);
        setEdizm(edizm);
        setCoast(coast);
        setCategory(category);
        setShop(shop);
        setComment(comment);
        setFavorite(favorite);
    }

    public Product(Cursor cursor) {
        this(cursor.getLong(cursor.getColumnIndex(DB.KEY_ID)),
                cursor.getString(cursor.getColumnIndex(DB.KEY_ALL_ITEMS_NAME)),
                cursor.getString(cursor.getColumnIndex(DB.KEY_ALL_ITEMS_COUNT)),
                cursor.getString(cursor.getColumnIndex(DB.KEY_ALL_ITEMS_EDIZM)),
                cursor.getString(cursor.getColumnIndex(DB.KEY_ALL_ITEMS_COAST)),
                cursor.getString(cursor.getColumnIndex(DB.KEY_ALL_ITEMS_CATEGORY)),
                cursor.getString(cursor.getColumnIndex(DB.KEY_ALL_ITEMS_SHOP)),
                cursor.getString(cursor.getColumnIndex(DB.KEY_ALL_ITEMS_COMMENT)),
                cursor.getString(cursor.getColumnIndex(DB.KEY_ALL_ITEMS_FAVORITE)));
    }

    public void remove(DB db) {
        db.deleteRows(DB.TABLE_ALL_ITEMS, DB.KEY_ID + "=" + getId());
    }

    public void update(DB db, String name, String count, String edizm, String coast, String category, String shop, String comment) {
        this.name = name;
        setCount(count);
        this.edizm = edizm;
        setCoast(coast);
        this.category = category;
        this.shop = shop;
        this.comment = comment;
        db.update(DB.TABLE_ALL_ITEMS, DB.COLUMNS_ALL_ITEMS, DB.KEY_ID + "=" + getId(), getFields());
    }

    public void updateInDb(DB db){
        db.update(DB.TABLE_ALL_ITEMS, DB.COLUMNS_ALL_ITEMS, DB.KEY_ID + "=" + getId(), getFields());
    }

    public String[] getFields() {
        return new String[]{
                getName(),
                getCount().toString(),
                getEdizm(),
                getCoast().toString(),
                getCategory(),
                getShop(),
                getComment(),
                isFavoriteInString()
        };
    }

    @Override
    public boolean equals(Object object){
        Product product = (Product) object;
        return getName().equals(product.getName());
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(count.toString());
        dest.writeString(edizm);
        dest.writeString(coast.toString());
        dest.writeString(category);
        dest.writeString(shop);
        dest.writeString(comment);
    }

    public static final Parcelable.Creator<Product> CREATOR = new Parcelable.Creator<Product>() {
        public Product createFromParcel(Parcel in) {
            return new Product(in);
        }

        public Product[] newArray(int size) {
            return new Product[size];
        }
    };

    private Product(Parcel parcel) {
        id = parcel.readLong();
        name = parcel.readString();
        count = new BigDecimal(parcel.readString());
        edizm = parcel.readString();
        coast = new BigDecimal(parcel.readString());
        category = parcel.readString();
        shop = parcel.readString();
        comment = parcel.readString();
        favorite = true;
        selected = true;
    }

    @Override
    public String toString() {
        return name;
    }


    ////////////////////////////////////// SETTERS AND GETTERS /////////////////////////////////////////
    public String getName() {
        return name;
    }

    public void setName(String name) {
        if (name == null) {
            name = "";
        }
        this.name = name;
    }

    public BigDecimal getCount() {
        return count;
    }

    public String getCountInString() {
        return count.toString();
    }

    public void setCount(String count) {
        try {
            this.count = new BigDecimal(count);
        } catch (Exception e) {
            this.count = new BigDecimal("0");
        }
    }

    public String getEdizm() {
        return edizm;
    }

    public void setEdizm(String edizm) {
        if (edizm == null) {
            edizm = "";
        }
        this.edizm = edizm;
    }

    public BigDecimal getCoast() {
        return coast;
    }

    public String getCoastInString() {
        return coast.toString();
    }

    public void setCoast(String coast) {
        try {
            this.coast = new BigDecimal(coast);
        } catch (Exception e) {
            this.coast = new BigDecimal("0");
        }
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        if (category == null) {
            category = "";
        }
        this.category = category;
    }

    public String getShop() {
        return shop;
    }

    public void setShop(String shop) {
        if (shop == null) {
            shop = "";
        }
        this.shop = shop;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        if (comment == null) {
            comment = "";
        }
        this.comment = comment;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public boolean isFavorite() {
        return favorite;
    }

    public void setFavorite(boolean favorite) {
        this.favorite = favorite;
    }

    public void setFavorite(String favorite) {
        this.favorite = Boolean.valueOf(favorite);
    }

    public String isFavoriteInString() {
        return String.valueOf(favorite);
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}
