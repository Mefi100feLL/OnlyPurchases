package com.PopCorp.Purchases.Data;

import android.database.Cursor;

import com.PopCorp.Purchases.DataBase.DB;

public class Category {

    private long id;
    private String name;
    private int color;

    public Category(long id, String name, int color){
        setId(id);
        setName(name);
        setColor(color);
    }

    public Category(Cursor cursor){
        this(cursor.getLong(cursor.getColumnIndex(DB.KEY_ID)), cursor.getString(cursor.getColumnIndex(DB.KEY_CATEGS_NAME)), cursor.getInt(cursor.getColumnIndex(DB.KEY_CATEGS_COLOR)));
    }

    public void putInDb(DB db){
        long id = db.addRec(DB.TABLE_CATEGORIES, new String[] {DB.KEY_CATEGS_NAME, DB.KEY_CATEGS_COLOR}, new String[] {name, String.valueOf(color)});
        if (id!=-1){
            this.id = id;
        }
    }

    public void updateInDb(DB db, String name, int color){
        setName(name);
        setColor(color);
        db.update(DB.TABLE_CATEGORIES, new String[] {DB.KEY_CATEGS_NAME, DB.KEY_CATEGS_COLOR}, DB.KEY_ID + "=" + id, new String[] {name, String.valueOf(color)});
    }

    public void removeFromDb(DB db){
        db.deleteRows(DB.TABLE_CATEGORIES, DB.KEY_ID + "=" + id);
    }

    @Override
    public String toString(){
        return name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getColor() {
        return color;
    }

    public void setColor(int color) {
        this.color = color;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }
}
