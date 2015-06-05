package com.PopCorp.Purchases.Controllers;

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Adapters.AllProductsAdapter;
import com.PopCorp.Purchases.Adapters.ProductsAdapter;
import com.PopCorp.Purchases.Data.Product;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Fragments.AllProductsFragment;
import com.PopCorp.Purchases.Loaders.AllProductsLoader;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class AllProductsController implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int ID_FOR_CREATE_LOADER_FROM_DB = 1;

    private final Activity context;
    private final SharedPreferences sPref;
    private final AllProductsFragment fragment;
    private final ViewGroup layoutForSnackBar;
    private final DB db;
    private final AllProductsAdapter adapter;
    private final ArrayList<Product> items = new ArrayList<>();
    private String currentFilter = ProductsAdapter.FILTER_TYPE_NAMES;
    private Product editedItem;
    private Product removedItem;


    public AllProductsController(Activity context, AllProductsFragment fragment, ViewGroup layoutForSnackBar) {
        this.context = context;
        this.layoutForSnackBar = layoutForSnackBar;
        this.fragment = fragment;
        sPref = PreferenceManager.getDefaultSharedPreferences(context);
        db = ((MainActivity) context).getDB();

        ArrayList<String> categories = getCategories();
        ArrayList<Integer> colors = getColors();
        adapter = new AllProductsAdapter(fragment, this, items, categories, colors);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Loader<Cursor> result = null;
        if (id == ID_FOR_CREATE_LOADER_FROM_DB) {
            result = new AllProductsLoader(context, db);
        }
        return result;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                addListItemAllFromCursor(cursor);
                while (cursor.moveToNext()) {
                    addListItemAllFromCursor(cursor);
                }
            }
            cursor.close();
            adapter.getFilter().filter(ProductsAdapter.FILTER_TYPE_NAMES);
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void addListItemAllFromCursor(Cursor cursor) {
        Product newProduct = new Product(cursor);
        if (!items.contains(newProduct)) {
            items.add(newProduct);
        }
    }

    public void sort(int itemId) {
        switch (itemId) {
            case R.id.action_sort_by_abc: {
                adapter.getFilter().filter(ProductsAdapter.FILTER_TYPE_NAMES);
                currentFilter = ProductsAdapter.FILTER_TYPE_NAMES;
                break;
            }
            case R.id.action_sort_by_category: {
                adapter.getFilter().filter(ProductsAdapter.FILTER_TYPE_CATEGORIES);
                currentFilter = ProductsAdapter.FILTER_TYPE_CATEGORIES;
                break;
            }
            case R.id.action_sort_by_favorite: {
                adapter.getFilter().filter(ProductsAdapter.FILTER_TYPE_FAVORITE);
                currentFilter = ProductsAdapter.FILTER_TYPE_FAVORITE;
                break;
            }
        }
    }

    public void showPopupMenu(View view, final Product product) {
        PopupMenu popupMenu = new PopupMenu(context, view);
        popupMenu.inflate(R.menu.popup_menu_for_product);
        if (product.isFavorite()) {
            popupMenu.getMenu().findItem(R.id.action_to_favorite).setVisible(false);
        } else {
            popupMenu.getMenu().findItem(R.id.action_remove_from_favorite).setVisible(false);
        }
        Object menuHelper;
        Class[] argTypes;
        try {
            Field fMenuHelper = PopupMenu.class.getDeclaredField("mPopup");
            fMenuHelper.setAccessible(true);
            menuHelper = fMenuHelper.get(popupMenu);
            argTypes = new Class[]{boolean.class};
            menuHelper.getClass().getDeclaredMethod("setForceShowIcon", argTypes).invoke(menuHelper, true);
        } catch (Exception e) {
            popupMenu.show();
            return;
        }
        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_edit_product: {
                        startEditingItem(product);
                        return true;
                    }
                    case R.id.action_remove_product: {
                        removeProduct(product);
                        return true;
                    }
                    case R.id.action_to_favorite: {
                        product.setFavorite(true);
                        product.updateInDb(db);
                        return true;
                    }
                    case R.id.action_remove_from_favorite: {
                        product.setFavorite(false);
                        product.updateInDb(db);
                        if (currentFilter.equals(ProductsAdapter.FILTER_TYPE_FAVORITE)) {
                            int position = adapter.getPublishItems().indexOf(product);
                            adapter.notifyItemRemoved(position);
                            adapter.getPublishItems().remove(product);
                            adapter.sort();
                        }
                        return true;
                    }
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }

    private void removeProduct(Product product) {
        removeProductFromTmpArray();
        removedItem = product;
        items.remove(product);
        int position = adapter.getPublishItems().indexOf(product);
        adapter.notifyItemRemoved(position);
        adapter.getPublishItems().remove(product);
        adapter.sort();

        SnackbarManager.show(Snackbar.with(context.getApplicationContext())
                .text(R.string.notify_listitem_are_deleted)
                .actionLabel(R.string.string_undo)
                .actionColor(sPref.getInt(SD.PREFS_COLOR_ACCENT, context.getResources().getColor(R.color.accent)))
                .actionListener(new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        if (removedItem != null) {
                            items.add(removedItem);
                            if (currentFilter.equals(ProductsAdapter.FILTER_TYPE_FAVORITE)) {
                                if (!removedItem.isFavorite()) {
                                    removedItem = null;
                                    return;
                                }
                            }
                            adapter.getPublishItems().add(removedItem);
                            adapter.sort();
                            int position = adapter.getPublishItems().indexOf(removedItem);
                            if (position != -1) {
                                adapter.notifyItemInserted(position);
                            }
                            removedItem = null;
                        }
                    }
                })
                .eventListener(new EventListener() {
                    @Override
                    public void onShow(Snackbar snackbar) {

                    }

                    @Override
                    public void onShowByReplace(Snackbar snackbar) {

                    }

                    @Override
                    public void onShown(Snackbar snackbar) {

                    }

                    @Override
                    public void onDismiss(Snackbar snackbar) {
                        removeProductFromTmpArray();
                    }

                    @Override
                    public void onDismissByReplace(Snackbar snackbar) {

                    }

                    @Override
                    public void onDismissed(Snackbar snackbar) {

                    }
                }), layoutForSnackBar);
    }

    public void removeProductFromTmpArray() {
        if (removedItem != null) {
            removedItem.remove(db);
            removedItem = null;
        }
    }

    private void startEditingItem(Product product) {
        editedItem = product;
        fragment.putItemInFields(editedItem);
        fragment.animationChangeFabFromAddToEdit();
    }

    public void showToast(int text) {
        SnackbarManager.show(Snackbar.with(context.getApplicationContext()).actionColor(sPref.getInt(SD.PREFS_COLOR_ACCENT, context.getResources().getColor(R.color.accent))).text(text), layoutForSnackBar);
    }

    public ArrayList<String> getCategories() {
        ArrayList<String> result = new ArrayList<>();
        Cursor cursor = db.getAllData(DB.TABLE_CATEGORIES);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CATEGS_NAME)));
                while (cursor.moveToNext()) {
                    result.add(cursor.getString(cursor.getColumnIndex(DB.KEY_CATEGS_NAME)));
                }
            }
            cursor.close();
        }
        result.add(context.getString(R.string.string_no_category));
        return result;
    }

    public ArrayList<Integer> getColors() {
        ArrayList<Integer> result = new ArrayList<>();
        Cursor cursor = db.getAllData(DB.TABLE_CATEGORIES);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                result.add(cursor.getInt(cursor.getColumnIndex(DB.KEY_CATEGS_COLOR)));
                while (cursor.moveToNext()) {
                    result.add(cursor.getInt(cursor.getColumnIndex(DB.KEY_CATEGS_COLOR)));
                }
            }
            cursor.close();
        }
        result.add(context.getResources().getColor(android.R.color.transparent));
        return result;
    }

    public AllProductsAdapter getAdapter() {
        return adapter;
    }

    public Product getEditedItem() {
        return editedItem;
    }

    public boolean addNewProduct(String name, String count, String edizm, String coast, String category, String shop, String comment, String favorite) {
        if (editedItem != null) {
            return editItem(name, count, edizm, coast, category, shop, comment, favorite);
        }
        for (Product item : items) {
            if (item.getName().equals(name)) {
                return false;
            }
        }
        Product newProduct = new Product(0, name, count, edizm, coast, category, shop, comment, favorite);
        long id = db.addRec(DB.TABLE_ALL_ITEMS, DB.COLUMNS_ALL_ITEMS, newProduct.getFields());
        newProduct.setId(id);
        items.add(newProduct);
        if (currentFilter.equals(ProductsAdapter.FILTER_TYPE_FAVORITE)) {
            if (!Boolean.valueOf(favorite)) {
                return true;
            }
        }
        adapter.getPublishItems().add(newProduct);
        adapter.sort();
        int position = adapter.getPublishItems().indexOf(newProduct);
        if (position != -1) {
            adapter.notifyItemInserted(position);
        }
        return true;
    }

    private boolean editItem(String name, String count, String edizm, String coast, String category, String shop, String comment, String important) {
        for (Product item : items) {
            if (item.getName().equals(name)) {
                if (!item.equals(editedItem)) {
                    return false;
                }
            }
        }
        if (!items.contains(editedItem)) {
            showToast(R.string.notify_listitem_are_deleted);
            return true;
        }
        int oldPosition = adapter.getPublishItems().indexOf(editedItem);
        editedItem.setFavorite(important);
        editedItem.update(db, name, count, edizm, coast, category, shop, comment);
        if (currentFilter.equals(ProductsAdapter.FILTER_TYPE_FAVORITE)) {
            if (!Boolean.valueOf(important)) {
                adapter.getPublishItems().remove(editedItem);
                adapter.notifyItemRemoved(oldPosition);
                adapter.sort();
                editedItem = null;
                return true;
            }
        }

        adapter.sort();
        int newPosition = adapter.getPublishItems().indexOf(editedItem);
        if (newPosition != -1) {
            adapter.notifyItemChanged(oldPosition);
        }
        if (oldPosition != newPosition) {
            if (oldPosition == -1) {
                adapter.notifyItemInserted(newPosition);
            } else if (newPosition == -1) {
                adapter.notifyItemRemoved(oldPosition);
            } else {
                adapter.notifyItemMoved(oldPosition, newPosition);
            }
        }

        editedItem = null;
        return true;
    }

    public void showHelp() {
        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.string_help)
                .content(R.string.string_help_products)
                .positiveText(R.string.string_ok).build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    public void setEditedItem(Product editedItem) {
        this.editedItem = editedItem;
    }
}
