package com.PopCorp.Purchases.Controllers;

import android.app.Dialog;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.view.MenuItem;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Adapters.ListAdapter;
import com.PopCorp.Purchases.Data.List;
import com.PopCorp.Purchases.Data.ListItem;
import com.PopCorp.Purchases.Data.Product;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Fragments.ListFragment;
import com.PopCorp.Purchases.Loaders.LoaderItemsFromSMS;
import com.PopCorp.Purchases.Loaders.LoaderItemsFromSMS.CallbackForLoadingSMS;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.PopCorp.Purchases.Utilites.ListWriter;
import com.PopCorp.Purchases.Utilites.ShowHideOnScroll;
import com.afollestad.materialdialogs.MaterialDialog;
import com.afollestad.materialdialogs.MaterialDialog.ButtonCallback;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class ListController implements CallbackForLoadingSMS, DialogController.CallbackForEditingList {

    private final MainActivity context;
    private final DB db;
    private final ListFragment fragment;
    private final SharedPreferences sPref;

    private LoaderItemsFromSMS loadingSms;

    private List currentList;
    private ListAdapter adapter;

    private final ViewGroup layoutForSnackBar;

    private final ArrayList<String> allProducts = new ArrayList<>();
    private ArrayAdapter<String> adapterWithProducts;
    private final Handler handler = new Handler();

    private final ArrayList<String> shopsForFilter = new ArrayList<>();
    private String filterShop = "";

    private final ArrayList<ListItem> itemsForRemove = new ArrayList<>();
    private ListItem editedItem;


    public ListController(ListFragment fragment, String datelist, ViewGroup layoutForSnackBar, ShowHideOnScroll touchListener) {
        this.fragment = fragment;
        this.layoutForSnackBar = layoutForSnackBar;
        context = (MainActivity) fragment.getActivity();
        sPref = PreferenceManager.getDefaultSharedPreferences(context);
        db = context.getDB();

        loadAllProducts();
        openList(datelist, touchListener);
    }

    private void openList(String datelist, ShowHideOnScroll touchListener) {
        Cursor cursor = db.getdata(DB.TABLE_LISTS, DB.COLUMNS_LISTS_WITH_ID, DB.KEY_LISTS_DATELIST + "='" + datelist + "'", null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                currentList = new List(db, cursor);
            }
            cursor.close();
        }
        if (currentList == null) {
            fragment.backToLists();
            return;
        }
        adapter = new ListAdapter(context, currentList.getItems(), this, currentList.getCurrency(), touchListener);
        fragment.setTitle(currentList.getName());
        if (currentList.getItems().size() == 0) {
            fragment.showEmpty();
        } else {
            fragment.showListView();
        }
    }


    ////////////////////////////////////////////////////////////// OPERATIONS WITH LIST ///////////////////////////////////////////////////
    private void renameCurrentList(String newName) {
        currentList.rename(db, newName);
        fragment.setTitle(newName);
    }

    public void removeCurrentList() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.dialog_title_remove_list);
        builder.content(R.string.dialog_are_you_sure_to_remove_list);
        builder.positiveText(R.string.dialog_remove);
        builder.negativeText(R.string.dialog_cancel);
        builder.callback(new ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                currentList.remove(db);
                fragment.backToLists();
            }
        });
        Dialog dialog = builder.build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
    ////////////////////////////////////////////////////////////// OPERATIONS WITH LIST ///////////////////////////////////////////////////


    /////////////////////////////////////////////////////////////// OPERATIONS WITH ITEMS //////////////////////////////////////////////////
    public void changeItemBuyed(int positionInPublish, ListItem item) {
        item.changeBuyed(db);
        adapter.getPublishItems().updateItemAt(positionInPublish, item);
        recoastTotals();
    }

    public void startEditingItem(ListItem listItem) {
        editedItem = listItem;
        fragment.startEditingItem(editedItem);
    }

    public void removeItems(ArrayList<ListItem> selectedItems) {
        removeItemsFromTmpArray();
        itemsForRemove.addAll(selectedItems);
        currentList.getItems().removeAll(itemsForRemove);
        refreshAll();

        String text;
        if (itemsForRemove.size() == 1) {
            text = context.getString(R.string.string_removed) + " " + itemsForRemove.get(0).getName();
        } else {
            text = context.getString(R.string.string_removed_elements) + " " + itemsForRemove.size();
        }
        SnackbarManager.show(Snackbar.with(context.getApplicationContext())
                .text(text)
                .actionLabel(R.string.string_undo)
                .actionColor(sPref.getInt(SD.PREFS_COLOR_ACCENT, context.getResources().getColor(R.color.accent)))
                .actionListener(new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        currentList.getItems().addAll(itemsForRemove);
                        refreshAll();
                        itemsForRemove.clear();
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
                        removeItemsFromTmpArray();
                    }

                    @Override
                    public void onDismissByReplace(Snackbar snackbar) {

                    }

                    @Override
                    public void onDismissed(Snackbar snackbar) {

                    }
                }), layoutForSnackBar);
    }

    public void removeItemsFromTmpArray() {
        if (itemsForRemove != null) {
            currentList.removeItems(db, itemsForRemove);
            itemsForRemove.clear();
        }
    }


    public boolean addNewListItem(String name, String count, String edizm, String coast, String category, String shop, String comment, String important) {
        if (editedItem != null) {
            return editItem(name, count, edizm, coast, category, shop, comment, important);
        }
        ListItem newItem = currentList.addNewItem(db, name, count, edizm, coast, category, shop, comment, "false", important);
        if (newItem == null) {
            return false;
        }
        refreshAll();
        return true;
    }

    private boolean editItem(String name, String count, String edizm, String coast, String category, String shop, String comment, String important) {
        for (ListItem item : currentList.getItems()) {
            if (item.getName().equals(name)) {
                if (!item.equals(editedItem)) {
                    return false;
                }
            }
        }
        if (!currentList.getItems().contains(editedItem)) {
            showToast(R.string.notify_listitem_are_deleted);
            return true;
        }
        int oldPosition = adapter.getPublishItems().indexOf(editedItem);
        editedItem.update(db, name, count, edizm, coast, category, shop, comment, important);
        if (oldPosition!=-1) {
            adapter.getPublishItems().updateItemAt(oldPosition, editedItem);
        }
        refreshAll();
        return true;
    }

    public void sendItems(int typeOfSending, ArrayList<ListItem> selectedItems) {
        currentList.sendItems(context, typeOfSending, selectedItems);
    }
    /////////////////////////////////////////////////////////////// OPERATIONS WITH ITEMS //////////////////////////////////////////////////

    public void refreshAll() {
        if (currentList.getItems().size() == 0) {
            fragment.showEmpty();
        } else {
            fragment.showListView();
        }
        recoastTotals();
        refreshFilterShops();
    }

    private void recoastTotals() {
        currentList.recoastTotals();
        fragment.showTotals(currentList.getTotalBuyed(), currentList.getTotal(), String.valueOf(currentList.getItems().size()));
    }

    private void refreshFilterShops() {
        shopsForFilter.clear();
        shopsForFilter.addAll(currentList.refreshFilterShops());
        if (shopsForFilter.size() == 0) {
            filterShop = context.getString(R.string.string_all_shops);
            fragment.hideFilterMenuItem();
        } else {
            shopsForFilter.add(0, context.getString(R.string.string_all_shops));
            if (!shopsForFilter.contains(filterShop)) {
                filterShop = context.getString(R.string.string_all_shops);
            }
            fragment.showFilterMenuItem(shopsForFilter, filterShop);
        }
        selectFilter(filterShop);
    }

    private void selectFilter(String filter) {
        adapter.getFilter().filter(filter);
    }


    public void updateListFromProducts(ArrayList<Product> newArray) {
        ArrayList<ListItem> updatedItems = currentList.updateItems(db, newArray);
        for (ListItem item : updatedItems) {
            int position = adapter.getPublishItems().indexOf(item);
            adapter.getPublishItems().updateItemAt(position, item);
        }
        refreshAll();
    }

    @Override
    public void showToast(int text) {
        SnackbarManager.show(Snackbar.with(context.getApplicationContext()).actionColor(sPref.getInt(SD.PREFS_COLOR_ACCENT, context.getResources().getColor(R.color.accent))).text(text), layoutForSnackBar);
    }

    public void showDialogForAlarm() {
        DialogController.showDialogForAlarm(db, context, currentList);
    }


    public boolean closeActionMode() {
        if (adapter.getActionMode() != null) {
            adapter.getActionMode().finish();
            return true;
        }
        return false;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();
        for (String shop : shopsForFilter) {
            if (itemId == shop.hashCode()) {
                if (!shop.equals(filterShop)) {
                    filterShop = shop;
                    selectFilter(filterShop);
                    return true;
                }
            }
        }
        return false;
    }

    //////////////////////////////////////////////////////////////// LOADING FROM SMS /////////////////////////////////////////////////////////////
    public void loadFromSMS() {
        if (loadingSms != null) {
            if (loadingSms.getStatus().equals(AsyncTask.Status.RUNNING)) {
                if (!loadingSms.isCancelled()) {
                    return;
                }
            }
        }
        loadingSms = new LoaderItemsFromSMS(context, this);
        loadingSms.execute();
    }

    @Override
    public void onSMSLoaded(ArrayList<HashMap<String, String>> loadedSms) {
        DialogController.showDialogWithSMS(context, loadedSms, this);
    }


    @Override
    public void loadFromSelectedSMS(ArrayList<HashMap<String, String>> mapsSMS) {
        if (mapsSMS.size()==0){
            showToast(R.string.notify_no_selected_sms);
            return;
        }
        String sms = "";
        for (int i = 0; i < mapsSMS.size(); i++) {
            sms += mapsSMS.get(i).get(SD.SMS_KEY_BODY) + "\n";
        }
        List newList = new ListWriter(context).read(db, context, sms, true);
        if (newList.getName() != null && newList.getCurrency() != null) {
            for (ListItem item : newList.getItems()) {
                currentList.addNewItem(db, item);
            }
            refreshAll();
        } else {
            showToast(R.string.notify_bad_sms);
        }
    }


    public ListAdapter getAdapter() {
        return adapter;
    }

    public List getCurrentList() {
        return currentList;
    }

    public ListItem getEditedItem() {
        return editedItem;
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

    public ArrayAdapter<String> getAdapterWithProducts() {
        return adapterWithProducts;
    }

    private void setAdapterWithProducts(ArrayAdapter<String> adapter) {
        adapterWithProducts = adapter;
    }

    public ListItem getSelectedItem(int position) {
        ListItem result = null;
        Cursor cursor = db.getdata(DB.TABLE_ALL_ITEMS, null, DB.KEY_ALL_ITEMS_NAME + "='" + adapterWithProducts.getItem(position) + "'", null, null, null, null);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                Product tmpProduct = new Product(cursor);
                result = new ListItem(0, "", tmpProduct.getName(), tmpProduct.getCountInString(), tmpProduct.getEdizm(), tmpProduct.getCoastInString(), tmpProduct.getCategory(), tmpProduct.getShop(), tmpProduct.getComment(), "false", "false");
            }
            cursor.close();
        }
        return result;
    }

    private void loadAllProducts() {
        Thread th = new Thread(null, loadAll, "loadAll");
        th.setPriority(10);
        th.setDaemon(true);
        th.start();
    }

    private final Runnable loadAll = new Runnable() {
        @Override
        public void run() {
            Cursor cursorWithAllProducts = db.getdata(DB.TABLE_ALL_ITEMS, new String[]{DB.KEY_ALL_ITEMS_NAME}, null, null, null, null, null);
            if (cursorWithAllProducts != null) {
                if (cursorWithAllProducts.moveToFirst()) {
                    allProducts.add(cursorWithAllProducts.getString(cursorWithAllProducts.getColumnIndex(DB.KEY_ALL_ITEMS_NAME)));
                    while (cursorWithAllProducts.moveToNext()) {
                        allProducts.add(cursorWithAllProducts.getString(cursorWithAllProducts.getColumnIndex(DB.KEY_ALL_ITEMS_NAME)));
                    }
                }
                cursorWithAllProducts.close();
            }

            setAdapterWithProducts(new ArrayAdapter<>(context, android.R.layout.simple_dropdown_item_1line, allProducts.toArray(new String[allProducts.size()])));
            handler.post(new Runnable() {
                public void run() {
                    fragment.setAutoCompleteAdapter();
                }
            });
        }
    };

    public void hideActionButton() {
        fragment.hideActionButtonToRight();
    }

    public void showActionButton() {
        fragment.showActionButtonFromRight();
    }

    public void setEditedItem(ListItem editedItem) {
        this.editedItem = editedItem;
    }

    public void showDialogForSendingList() {
        DialogController.showDialogForSendingList(context, currentList, this);
    }

    @Override
    public void onListEdited(List list, String name, String currency) {
        if (!list.getName().equals(name)) {
            renameCurrentList(name);
        }
        if (!currentList.getCurrency().equals(currency)) {
            currentList.changeCurrency(db, currency);
            adapter.setCurrency(currency);
            adapter.notifyDataSetChanged();
            refreshAll();
        }
    }

    public void showHelp() {
        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.string_help)
                .content(R.string.string_help_list)
                .positiveText(R.string.string_ok).build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }

    @Override
    public void addNewList(String name, String currency) {}

    public void showDialogForEditingList() {
        DialogController.showDialogForEditingList(context, currentList, sPref, this);
    }

    public void hideActionMode() {
        fragment.hideActionMode();
    }

    public void showActionMode() {
        fragment.showActionMode();
    }
}
