package com.PopCorp.Purchases.Controllers;

import android.app.Activity;
import android.app.Dialog;
import android.app.LoaderManager;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.PopupMenu;
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

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Adapters.AllProductsAdapter;
import com.PopCorp.Purchases.Adapters.CategoriesAdapter;
import com.PopCorp.Purchases.Adapters.ProductsAdapter;
import com.PopCorp.Purchases.Data.ListItem;
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
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class AllProductsController implements LoaderManager.LoaderCallbacks<Cursor> {

    public static final int ID_FOR_CREATE_LOADER_FROM_DB = 1;

    private final Activity context;
    private final SharedPreferences sPref;
    private final AllProductsFragment fragment;
    private final ViewGroup layoutForSnackBar;
    private final DB db;
    private final AllProductsAdapter adapter;
    private final ArrayList<Product> items = new ArrayList<>();
    private final SharedPreferences.Editor editor;
    private String currentFilter = ProductsAdapter.FILTER_TYPE_NAMES;
    private Product editedItem;
    private Product removedItem;
    private DialogForNewItem dialogForNewItem;


    public AllProductsController(Activity context, AllProductsFragment fragment, ViewGroup layoutForSnackBar) {
        this.context = context;
        this.layoutForSnackBar = layoutForSnackBar;
        this.fragment = fragment;
        sPref = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sPref.edit();
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
        dialogForNewItem = new DialogForNewItem(editedItem);
        dialogForNewItem.show();
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

    public void showDailogForNewItem() {
        dialogForNewItem = new DialogForNewItem();
        dialogForNewItem.show();
    }

    public DialogForNewItem getDialogForNewItem() {
        return dialogForNewItem;
    }

    public class DialogForNewItem {

        private Product item;
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

        public DialogForNewItem(Product item){
            this.item = item;
        }

        public void show() {
            final LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            View customView = inflater.inflate(R.layout.content_product_fields, null);

            inputLayout = (TextInputLayout) customView.findViewById(R.id.content_product_fields_input_layout);

            editName = (AutoCompleteTextView) customView.findViewById(R.id.content_product_fields_edittext_name);

            ImageView buttonForVoice = (ImageView) customView.findViewById(R.id.content_product_fields_button_voice);

            buttonForVoice.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    fragment.displaySpeechRecognizer();
                }
            });

            editCount = (EditText) customView.findViewById(R.id.content_product_fields_edittext_count);
            editCount.setText("1");

            editCoast = (EditText) customView.findViewById(R.id.content_product_fields_edittext_coast);

            editComment = (EditText) customView.findViewById(R.id.content_product_fields_edittext_comment);

            ImageView buttonCountPlus = (ImageView) customView.findViewById(R.id.content_product_fields_count_plus);
            ImageView buttonCountMinus = (ImageView) customView.findViewById(R.id.content_product_fields_count_minus);
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
            spinnerForEdizm = (Spinner) customView.findViewById(R.id.content_product_fields_spinner_edizm);
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
            spinnerForCategory = (Spinner) customView.findViewById(R.id.content_product_fields_spinner_category);
            spinnerForCategory.setAdapter(adapterForSpinnerCategory);
            spinnerForCategory.setSelection(adapterForSpinnerCategory.getCount() - 1);

            shopesForSpinner = new ArrayList<>(sPref.getStringSet(SD.PREFS_SHOPES, new LinkedHashSet<String>()));
            shopesForSpinner.add(context.getResources().getString(R.string.string_no_shop));//add item for no shop

            adapterForSpinnerShop = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, shopesForSpinner);
            adapterForSpinnerShop.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerForShop = (Spinner) customView.findViewById(R.id.content_product_fields_spinner_shop);
            spinnerForShop.setAdapter(adapterForSpinnerShop);
            spinnerForShop.setSelection(shopesForSpinner.size()-1);

            checkBoxForImportant = (CheckBox) customView.findViewById(R.id.content_product_fields_checkbox_important);

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
                        boolean result = addNewProduct(
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

        private void putItemInFields(Product item) {
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
            checkBoxForImportant.setChecked(item.isFavorite());
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
