package com.PopCorp.Purchases.Controllers;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.internal.widget.AdapterViewCompat;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Adapters.CategoriesAdapter;
import com.PopCorp.Purchases.Adapters.ListAdapter;
import com.PopCorp.Purchases.Data.Category;
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

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class ListController implements CallbackForLoadingSMS, DialogController.CallbackForEditingList {

    private final MainActivity context;
    private final DB db;
    private final ListFragment fragment;
    private final SharedPreferences sPref;
    private final SharedPreferences.Editor editor;

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
    private DialogForNewItem dialogForNewItem;


    public ListController(ListFragment fragment, String datelist, ViewGroup layoutForSnackBar, ShowHideOnScroll touchListener) {
        this.fragment = fragment;
        this.layoutForSnackBar = layoutForSnackBar;
        context = (MainActivity) fragment.getActivity();
        sPref = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sPref.edit();
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
        dialogForNewItem = new DialogForNewItem(editedItem);
        dialogForNewItem.show();
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
        }
    };

    public void hideActionButton() {
        fragment.hideActionButtonToRight();
    }

    public void showActionButton() {
        fragment.showActionButtonFromRight();
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

    public void showDailogForNewItem() {
        dialogForNewItem = new DialogForNewItem();
        dialogForNewItem.show();
    }

    public DialogForNewItem getDialogForNewItem() {
        return dialogForNewItem;
    }

    public class DialogForNewItem {

        private ListItem item;
        private ArrayList<String> edizmsForSpinner;
        private TextInputLayout inputLayout;
        private AutoCompleteTextView editName;
        private EditText editCount;
        private EditText editCoast;
        private EditText editComment;
        private ArrayAdapter<String> adapterForSpinnerEdizm;
        private Spinner spinnerForEdizm;
        private Spinner spinnerForCategory;
        private ArrayList<String> shopesForSpinner;
        private ArrayAdapter<String> adapterForSpinnerShop;
        private Spinner spinnerForShop;
        private CheckBox checkBoxForImportant;
        private ArrayList<String> categories;
        private CategoriesAdapter adapterForSpinnerCategory;

        public DialogForNewItem(){}

        public DialogForNewItem(ListItem item){
            this.item = item;
        }

        public void show() {
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View customView = inflater.inflate(R.layout.content_list_fields, null);

            inputLayout = (TextInputLayout) customView.findViewById(R.id.content_list_fields_input_layout);

            editName = (AutoCompleteTextView) customView.findViewById(R.id.content_list_fields_edittext_name);
            editName.setAdapter(getAdapterWithProducts());
            editName.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    ListItem item = getSelectedItem(position);
                    if (item != null) {
                        putItemInFields(item);
                    }
                }
            });

            ImageView buttonForVoice = (ImageView) customView.findViewById(R.id.content_list_fields_button_voice);

            buttonForVoice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.displaySpeechRecognizer();
                }
            });

            editCount = (EditText) customView.findViewById(R.id.content_list_fields_edittext_count);
            editCount.setText("1");

            editCoast = (EditText) customView.findViewById(R.id.content_list_fields_edittext_coast);

            editComment = (EditText) customView.findViewById(R.id.content_list_fields_edittext_comment);

            ImageView buttonCountPlus = (ImageView) customView.findViewById(R.id.content_list_fields_count_plus);
            ImageView buttonCountMinus = (ImageView) customView.findViewById(R.id.content_list_fields_count_minus);
            buttonCountPlus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        BigDecimal count = new BigDecimal(editCount.getText().toString());
                        count = count.add(new BigDecimal("1"));
                        editCount.setText(count.toString());
                    } catch (Exception ignored) {}
                }
            });

            buttonCountMinus.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        BigDecimal count = new BigDecimal(editCount.getText().toString());
                        if (count.doubleValue() >= 1) {
                            count = count.subtract(new BigDecimal("1"));
                            editCount.setText(count.toString());
                        }
                    } catch (Exception ignored) {
                    }
                }
            });


            edizmsForSpinner = new ArrayList<>(sPref.getStringSet(SD.PREFS_EDIZMS, new LinkedHashSet<String>()));

            adapterForSpinnerEdizm = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, edizmsForSpinner);
            adapterForSpinnerEdizm.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerForEdizm = (Spinner) customView.findViewById(R.id.content_list_fields_spinner_edizm);
            spinnerForEdizm.setAdapter(adapterForSpinnerEdizm);
            spinnerForEdizm.setSelection(getPositionForEdizm(sPref.getString(SD.PREFS_DEF_EDIZM, context.getResources().getString(R.string.default_unit_one))));

            spinnerForEdizm.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    editCoast.setHint(context.getResources().getString(R.string.string_coast_za_ed) + " " + spinnerForEdizm.getItemAtPosition(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });

            categories = getCategories();
            ArrayList<Integer> colors = getColors();

            adapterForSpinnerCategory = new CategoriesAdapter(context, categories, colors);
            adapterForSpinnerCategory.setDropDownViewResource(R.layout.item_list_category);
            spinnerForCategory = (Spinner) customView.findViewById(R.id.content_list_fields_spinner_category);
            spinnerForCategory.setAdapter(adapterForSpinnerCategory);
            spinnerForCategory.setSelection(adapterForSpinnerCategory.getCount() - 1);

            shopesForSpinner = new ArrayList<>(sPref.getStringSet(SD.PREFS_SHOPES, new LinkedHashSet<String>()));
            shopesForSpinner.add(context.getResources().getString(R.string.string_no_shop));//add item for no shop

            adapterForSpinnerShop = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, shopesForSpinner);
            adapterForSpinnerShop.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerForShop = (Spinner) customView.findViewById(R.id.content_list_fields_spinner_shop);
            spinnerForShop.setAdapter(adapterForSpinnerShop);
            spinnerForShop.setSelection(shopesForSpinner.size()-1);

            checkBoxForImportant = (CheckBox) customView.findViewById(R.id.content_list_fields_checkbox_important);

            MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
            builder.customView(customView, true);
            if (item!=null) {
                builder.positiveText(R.string.dialog_edit);
            } else{
                builder.positiveText(R.string.dialog_add);
            }
            builder.negativeText(R.string.dialog_cancel);
            builder.autoDismiss(false);
            builder.backgroundColorRes(android.R.color.white);
            builder.callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    if (!editName.getText().toString().isEmpty()) {
                        String shop = (String) spinnerForShop.getSelectedItem();
                        if (spinnerForShop.getSelectedItemPosition() == adapterForSpinnerShop.getCount() - 1) {
                            shop = "";
                        }
                        boolean result = addNewListItem(
                                editName.getText().toString(),
                                editCount.getText().toString(),
                                (String) spinnerForEdizm.getSelectedItem(),
                                editCoast.getText().toString(),
                                (String) spinnerForCategory.getSelectedItem(),
                                shop,
                                editComment.getText().toString(),
                                String.valueOf(checkBoxForImportant.isChecked()));
                        if (result) {
                            dialogForNewItem = null;
                            editedItem = null;
                            dialog.dismiss();
                        } else{
                            inputLayout.setError(context.getString(R.string.notify_listitem_already_exists));
                        }
                    } else {
                        inputLayout.setError(context.getString(R.string.notify_enter_name_of_listitem));
                    }
                }

                @Override
                public void onNegative(MaterialDialog dialog) {
                    super.onNegative(dialog);
                    dialogForNewItem = null;
                    editedItem = null;
                    dialog.dismiss();
                }
            });
            if (item!=null){
                putItemInFields(item);
            }
            Dialog dialog = builder.build();

            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }

        private int getPositionForEdizm(String edizm) {
            if (edizm == null) {
                return adapterForSpinnerEdizm.getCount() - 1;
            }
            if (!edizmsForSpinner.contains(edizm)) {
                if (edizm.equals("")) {
                    return adapterForSpinnerEdizm.getCount() - 1;
                }
                edizmsForSpinner.add(0, edizm);
                addNewEdizmToPrefs(edizm);
            }
            return adapterForSpinnerEdizm.getPosition(edizm);
        }

        private void addNewEdizmToPrefs(final String newEdizm) {
            Set<String> edizmsFromPrefs = sPref.getStringSet(SD.PREFS_EDIZMS, new LinkedHashSet<String>());
            if (edizmsFromPrefs != null) {
                edizmsFromPrefs.add(newEdizm);
            }
            editor.putStringSet(SD.PREFS_EDIZMS, edizmsFromPrefs);
            editor.commit();
        }

        private int getPositionForShop(String shop) {
            if (shop == null) {
                return adapterForSpinnerShop.getCount() - 1;
            }
            if (!shopesForSpinner.contains(shop)) {
                if (shop.equals("")) {
                    return adapterForSpinnerShop.getCount() - 1;
                }
                shopesForSpinner.add(0, shop);
                addNewShopToPrefs(shop);
            }
            return adapterForSpinnerShop.getPosition(shop);
        }

        private void addNewShopToPrefs(final String newShop) {
            Set<String> shopesFromPrefs = sPref.getStringSet(SD.PREFS_SHOPES, new LinkedHashSet<String>());
            if (shopesFromPrefs != null) {
                shopesFromPrefs.add(newShop);
            }
            editor.putStringSet(SD.PREFS_SHOPES, shopesFromPrefs);
            editor.commit();
        }

        public void showError(int errorId) {
            inputLayout.setError(context.getString(errorId));
        }

        public void setNameItem(String name){
            editName.setText(name);
        }

        private void putItemInFields(ListItem item) {
            editName.setText(item.getName());
            inputLayout.setError(null);
            editName.requestFocus();
            editName.dismissDropDown();
            editCount.setText(item.getCountInString());
            spinnerForEdizm.setSelection(getPositionForEdizm(item.getEdizm()));
            editCoast.setText(item.getCoastInString());
            putCategoryInSpinner(item.getCategory());
            spinnerForShop.setSelection(getPositionForShop(item.getShop()));
            editComment.setText(item.getComment());
            checkBoxForImportant.setChecked(item.isImportant());
        }

        private void putCategoryInSpinner(String category) {
            if (categories.contains(category)) {
                spinnerForCategory.setSelection(adapterForSpinnerCategory.getItemPosition(category));
            } else {
                spinnerForCategory.setSelection(adapterForSpinnerCategory.getCount() - 1);
            }
        }
    }
}
