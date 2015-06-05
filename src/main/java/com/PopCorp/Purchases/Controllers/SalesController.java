package com.PopCorp.Purchases.Controllers;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.app.Dialog;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.graphics.Palette;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Adapters.CategoriesAdapter;
import com.PopCorp.Purchases.Adapters.SalesAdapter;
import com.PopCorp.Purchases.Comparators.MenuComparator;
import com.PopCorp.Purchases.Data.List;
import com.PopCorp.Purchases.Data.ListItem;
import com.PopCorp.Purchases.Data.Sale;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Fragments.SalesFragment;
import com.PopCorp.Purchases.Loaders.SalesInternetLoader;
import com.PopCorp.Purchases.Loaders.SalesLoader;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.PopCorp.Purchases.Utilites.InternetConnection;
import com.afollestad.materialdialogs.AlertDialogWrapper;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.ImageLoadingListener;
import com.software.shell.fab.ActionButton;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class SalesController implements LoaderCallbacks<Cursor> {

    public static final int ID_FOR_CREATE_SALES_LOADER_FROM_DB = 1;
    public static final int ID_FOR_CREATE_SALES_LOADER_FROM_NET = 2;

    private final ImageLoader imageLoader;
    private final DisplayImageOptions options;

    private SalesFragment fragment;
    private Context context;
    private DB db;
    private SharedPreferences sPref;
    private SharedPreferences.Editor editor;

    private SalesAdapter adapter;
    private ArrayList<Sale> sales = new ArrayList<>();

    private String currentShopId;
    private String currentShopName;

    private ArrayList<List> lists = new ArrayList<>();
    private ArrayList<List> selectedLists = new ArrayList<>();
    private ArrayList<String> names = new ArrayList<>();
    private Sale selectedSale;
    private ArrayAdapter<String> adapterForSpinnerShop;
    private ArrayList<String> shopesForSpinner;
    private ArrayList<String> edizmsForSpinner;
    private ArrayAdapter<String> adapterForSpinnerEdizm;

    private View.OnClickListener zoomedImageListener;
    private boolean animating = false;
    private int mShortAnimationDuration;
    private Palette palette;
    private String idSaleAnimated = "";
    private boolean loadingFinished = false;

    public SalesController(SalesFragment fragment, Context context, String currentShopId, String currentShopName) {
        this.fragment = fragment;
        this.context = context;
        this.currentShopId = currentShopId;
        this.currentShopName = currentShopName;
        sPref = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sPref.edit();

        db = ((MainActivity) context).getDB();

        adapter = new SalesAdapter(this, sales);

        imageLoader = ImageLoader.getInstance();
        options = new DisplayImageOptions.Builder()
                .showImageOnLoading(R.drawable.back_right)
                .showImageForEmptyUri(R.drawable.ic_error_image)
                .showImageOnFail(R.drawable.ic_no_internet)
                .cacheInMemory(true)
                .cacheOnDisk(true)
                .considerExifParams(true)
                .bitmapConfig(Bitmap.Config.RGB_565)
                .build();
    }


    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Loader<Cursor> result = null;
        if (id == ID_FOR_CREATE_SALES_LOADER_FROM_DB) {
            result = new SalesLoader(context, db, currentShopId);
        }
        return result;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                addSaleFromCusror(cursor);
                while (cursor.moveToNext()) {
                    addSaleFromCusror(cursor);
                }
            }
            cursor.close();
        }
        if (sales.size() > 0) {
            fragment.showListView();
            adapter.update();
        }
        fragment.loadSalesFromNet();
    }

    private void addSaleFromCusror(Cursor cursor) {
        Sale newSale = new Sale(cursor);
        if (isSaleActual(newSale)) {
            if (!sales.contains(newSale)) {
                sales.add(newSale);
            }
        }
    }

    public void updateSales(ArrayList<Sale> data) {
        if (data==null){
            if (sales.size() > 0) {
                fragment.showListView();
                adapter.update();
            } else{
                fragment.showEmpty(R.string.string_no_internet_connection, R.drawable.ic_no_internet);
            }
            return;
        }
        for (Sale sale : data) {
            if (!sales.contains(sale)) {
                sale.putInDB(db);
                sales.add(sale);
            } else{
                Sale existsSale = sales.get(sales.indexOf(sale));
                if (!Arrays.equals(existsSale.getFields(), sale.getFields())){
                    existsSale.setFields(sale.getFields());
                    existsSale.updateInDB(db);
                }
            }
        }
        if (sales.size() > 0) {
            fragment.showListView();
            adapter.update();
        } else{
            fragment.showEmpty(R.string.string_no_sales, R.drawable.ic_no_sales);
        }
    }

    public void addListsAndShowDialogForSelect(Sale clickedSale) {
        lists.clear();
        names.clear();
        selectedLists.clear();
        selectedSale = clickedSale;
        Cursor cursor = db.getAllData(DB.TABLE_LISTS);
        if (cursor != null) {
            if (cursor.moveToFirst()) {
                addListFromCursor(cursor);
                while (cursor.moveToNext()) {
                    addListFromCursor(cursor);
                }
            }
            cursor.close();
        }
        Collections.sort(lists, new MenuComparator(context));
        if (lists.size() > 0) {
            showDialogWithLists();
        } else {
            showDialogQuestionForCreateList();
        }
    }

    private void showDialogQuestionForCreateList() {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
        builder.title(R.string.dialog_no_lists);
        builder.content(R.string.dialog_are_create_new_list);
        builder.positiveText(R.string.dialog_create);
        builder.negativeText(R.string.dialog_cancel);
        builder.callback(new MaterialDialog.ButtonCallback() {
            @Override
            public void onPositive(MaterialDialog dialog) {
                showDialogForNewList();
            }
        });
        Dialog dialog = builder.build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }


    private void showDialogWithLists() {
        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.dialog_select_list)
                .items(names.toArray(new String[names.size()]))
                .itemsCallbackMultiChoice(new Integer[]{}, new MaterialDialog.ListCallbackMultiChoice() {
                    @Override
                    public boolean onSelection(MaterialDialog dialog, Integer[] which, CharSequence[] text) {
                        selectedLists.clear();
                        for (int i : which) {
                            selectedLists.add(lists.get(i));
                        }
                        return true;
                    }
                })
                .autoDismiss(false)
                .alwaysCallMultiChoiceCallback()
                .positiveText(R.string.dialog_select)
                .negativeText(R.string.dialog_cancel)
                .callback(new MaterialDialog.ButtonCallback() {
                    @Override
                    public void onPositive(MaterialDialog dialog) {
                        if (selectedLists.size() == 0) {
                            Toast.makeText(context, R.string.notify_no_selected_lists, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        new DialogForSendingSale().showDialogForSending();
                        dialog.dismiss();
                    }

                    @Override
                    public void onNegative(MaterialDialog dialog) {
                        super.onNegative(dialog);
                        dialog.dismiss();
                    }
                })
                .build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();;
    }


    private void addListFromCursor(Cursor cursor) {
        List newList = new List(db, cursor);
        lists.add(newList);
        names.add(newList.getName());
    }

    private class DialogForSendingSale {
        private AutoCompleteTextView editName;
        private EditText editCount;
        private EditText editCoast;
        private EditText editComment;
        private Spinner spinnerForEdizm;
        private Spinner spinnerForCategory;
        private Spinner spinnerForShop;
        private CheckBox checkBoxForImportant;
        private TextInputLayout inputLayout;

        public void showDialogForSending() {
            MaterialDialog.Builder builder = new MaterialDialog.Builder(context);
            builder.customView(R.layout.content_sale_fields, true);
            builder.positiveText(R.string.dialog_send);
            builder.negativeText(R.string.dialog_cancel);
            builder.autoDismiss(false);
            builder.backgroundColorRes(android.R.color.white);
            builder.callback(new MaterialDialog.ButtonCallback() {
                @Override
                public void onPositive(MaterialDialog dialog) {
                    if (editName.getText().toString().isEmpty()){
                        inputLayout.setError(context.getString(R.string.notify_enter_name_of_listitem));
                        return;
                    }
                    String shop = (String) spinnerForShop.getSelectedItem();
                    if (spinnerForShop.getSelectedItemPosition() == adapterForSpinnerShop.getCount() - 1) {
                        shop = "";
                    }
                    String total = "";
                    for (List list : selectedLists) {
                            ListItem addedItem = list.addNewItem(db,
                                    editName.getText().toString(),
                                    editCount.getText().toString(),
                                    (String) spinnerForEdizm.getSelectedItem(),
                                    editCoast.getText().toString(),
                                    (String) spinnerForCategory.getSelectedItem(),
                                    shop,
                                    editComment.getText().toString(),
                                    "false", //buyed
                                    String.valueOf(checkBoxForImportant.isChecked()));
                            if (addedItem == null) {
                                total += context.getString(R.string.notify_already_exists_in_list) + " " + list.getName();
                            } else {
                                total += context.getString(R.string.notify_added_in_list) + " " + list.getName();
                            }
                            total += "\n";
                    }
                    Toast.makeText(context, total.substring(0, total.length()-1), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                }

                @Override
                public void onNegative(MaterialDialog dialog) {
                    super.onNegative(dialog);
                    dialog.dismiss();
                }
            });
            Dialog dialog = builder.build();
            inputLayout = (TextInputLayout) dialog.findViewById(R.id.content_sale_fields_input_layout);

            editName = (AutoCompleteTextView) dialog.findViewById(R.id.content_sale_fields_edittext_name);
            editName.setText(selectedSale.getTitle());

            editCount = (EditText) dialog.findViewById(R.id.content_sale_fields_edittext_count);
            editCount.setText("1");

            editCoast = (EditText) dialog.findViewById(R.id.content_sale_fields_edittext_coast);
            editCoast.setText(selectedSale.getCoast().split(" ")[0]);

            editComment = (EditText) dialog.findViewById(R.id.content_sale_fields_edittext_comment);
            editComment.setText(selectedSale.getSubTitle());

            ImageView buttonCountPlus = (ImageView) dialog.findViewById(R.id.content_sale_fields_count_plus);
            ImageView buttonCountMinus = (ImageView) dialog.findViewById(R.id.content_sale_fields_count_minus);
            buttonCountPlus.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        BigDecimal count = new BigDecimal(editCount.getText().toString());
                        count = count.add(new BigDecimal("1"));
                        editCount.setText(count.toString());
                    } catch (Exception ignored) {}
                }
            });

            buttonCountMinus.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(View v) {
                    try {
                        BigDecimal count = new BigDecimal(editCount.getText().toString());
                        if (count.doubleValue() >= 1) {
                            count = count.subtract(new BigDecimal("1"));
                            editCount.setText(count.toString());
                        }
                    } catch (Exception ignored) {}
                }
            });


            edizmsForSpinner = new ArrayList<>(sPref.getStringSet(SD.PREFS_EDIZMS, new LinkedHashSet<String>()));

            adapterForSpinnerEdizm = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, edizmsForSpinner);
            adapterForSpinnerEdizm.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerForEdizm = (Spinner) dialog.findViewById(R.id.content_sale_fields_spinner_edizm);
            spinnerForEdizm.setAdapter(adapterForSpinnerEdizm);
            spinnerForEdizm.setSelection(getEdizmForSale(selectedSale));

            spinnerForEdizm.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    editCoast.setHint(context.getResources().getString(R.string.string_coast_za_ed) + " " + spinnerForEdizm.getItemAtPosition(position));
                }

                @Override
                public void onNothingSelected(AdapterView<?> arg0) {
                }
            });

            ArrayList<String> categories = getCategories();
            ArrayList<Integer> colors = getColors();

            CategoriesAdapter adapterForSpinnerCategory = new CategoriesAdapter(context, categories, colors);
            adapterForSpinnerCategory.setDropDownViewResource(R.layout.item_list_category);
            spinnerForCategory = (Spinner) dialog.findViewById(R.id.content_sale_fields_spinner_category);
            spinnerForCategory.setAdapter(adapterForSpinnerCategory);
            spinnerForCategory.setSelection(adapterForSpinnerCategory.getCount() - 1);

            shopesForSpinner = new ArrayList<>(sPref.getStringSet(SD.PREFS_SHOPES, new LinkedHashSet<String>()));
            shopesForSpinner.add(context.getResources().getString(R.string.string_no_shop));//add item for no shop

            adapterForSpinnerShop = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, shopesForSpinner);
            adapterForSpinnerShop.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerForShop = (Spinner) dialog.findViewById(R.id.content_sale_fields_spinner_shop);
            spinnerForShop.setAdapter(adapterForSpinnerShop);
            spinnerForShop.setSelection(getPositionForShop(currentShopName));

            checkBoxForImportant = (CheckBox) dialog.findViewById(R.id.content_sale_fields_checkbox_important);
            dialog.setCanceledOnTouchOutside(false);
            dialog.show();
        }
    }

    private int getEdizmForSale(Sale sale) {
        String edizm;
        if (sale.getCount().isEmpty()) {
            edizm = sPref.getString(SD.PREFS_DEF_EDIZM, context.getResources().getString(R.string.default_unit_one));
        } else {
            edizm = sale.getCount().split(" ")[1];
            if (edizm.equals("л")){
                if (sale.getCount().split(" ")[0].equals("1")){
                    edizm = context.getResources().getString(R.string.default_unit_three);
                } else{
                    edizm = context.getResources().getString(R.string.default_unit_one);
                }
            }
            if (edizm.equals("мл")){
                edizm = context.getResources().getString(R.string.default_unit_one);
            }
            if (edizm.equals("г")){
                edizm = context.getResources().getString(R.string.default_unit_one);
            }
            if (edizm.equals("кг")){
                if (!sale.getCount().split(" ")[0].equals("1")){
                    edizm = context.getResources().getString(R.string.default_unit_one);
                }
            }
        }
        return getPositionForEdizm(edizm);
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
        editor.putStringSet(SD.PREFS_EDIZMS, edizmsFromPrefs).commit();
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
        editor.putStringSet(SD.PREFS_SHOPES, shopesFromPrefs).commit();
    }


    public void showDialogForNewList() {
        AlertDialogWrapper.Builder builder = new AlertDialogWrapper.Builder(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View layout = inflater.inflate(R.layout.dialog_new_list, null);
        final EditText edittextForName = (EditText) layout.findViewById(R.id.dialog_new_list_edittext_name);
        final TextInputLayout inputLayout = (TextInputLayout) layout.findViewById(R.id.dialog_new_list_input_layout);

        final Spinner spinnerForCurrency = (Spinner) layout.findViewById(R.id.dialog_new_list_spinner_currency);
        Set<String> currencys = sPref.getStringSet(SD.PREFS_CURRENCYS, new LinkedHashSet<String>());
        if (currencys == null) {
            currencys = new LinkedHashSet<>();
        }
        ArrayAdapter<String> adapterForSpinnerCurrency = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, currencys.toArray(new String[currencys.size()]));
        adapterForSpinnerCurrency.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerForCurrency.setAdapter(adapterForSpinnerCurrency);
        spinnerForCurrency.setSelection(adapterForSpinnerCurrency.getPosition(sPref.getString(SD.PREFS_DEF_CURRENCY, context.getString(R.string.prefs_default_currency))));

        builder.setTitle(R.string.dialog_title_new_list);
        builder.setView(layout);
        builder.autoDismiss(false);
        builder.setPositiveButton(context.getResources().getString(R.string.dialog_create), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (checkNameAndCreateNewList(edittextForName.getText().toString(), (String) spinnerForCurrency.getSelectedItem())) {
                    dialog.dismiss();
                } else{
                    inputLayout.setError(context.getString(R.string.notify_no_entered_name_of_list));
                }
            }
        });
        builder.setNegativeButton(context.getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });

        final Dialog dialog = builder.create();

        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

        edittextForName.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(android.widget.TextView v, int actionId, KeyEvent event) {
                if (checkNameAndCreateNewList(edittextForName.getText().toString(), (String) spinnerForCurrency.getSelectedItem())) {
                    dialog.dismiss();
                } else{
                    inputLayout.setError(context.getString(R.string.notify_no_entered_name_of_list));
                }
                return true;
            }
        });
    }

    public boolean checkNameAndCreateNewList(String name, String currency) {
        if (name.isEmpty()) {
            return false;
        }
        addNewList(name, currency);
        return true;
    }

    public void addNewList(String newName, String currency) {
        String datelist = String.valueOf(Calendar.getInstance().getTimeInMillis());
        long id = db.addRec(DB.TABLE_LISTS, DB.COLUMNS_LISTS, new String[]{newName, datelist, "", currency});
        List newList = new List(db, id, newName, datelist, "", currency);
        lists.add(newList);
        Collections.sort(lists, new MenuComparator(context));
        selectedLists.add(newList);
        new DialogForSendingSale().showDialogForSending();
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


    private boolean isSaleActual(Sale newSale) {
        Calendar dateOfSaleFinish = Calendar.getInstance();
        dateOfSaleFinish.setTime(newSale.getPeriodFinish());
        dateOfSaleFinish.add(Calendar.DAY_OF_MONTH, 1);
        if (dateOfSaleFinish.getTimeInMillis() < Calendar.getInstance().getTimeInMillis()) {
            newSale.remove(db);
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    /////////////////////////////////////////////// SETTERS AND GETTERS //////////////////////////////////////////////
    public String getCurrentShop() {
        return currentShopId;
    }

    public SalesAdapter getAdapter() {
        return adapter;
    }

    public void zoomSale(final View thumbView, final Sale sale) {
        final ImageView expandedImageView = (ImageView) fragment.getView().findViewById(R.id.fragment_sales_zoomed_image);

        if (animating) {
            if (!idSaleAnimated.equals(sale.getSaleId()) && !loadingFinished){
                idSaleAnimated = sale.getSaleId();
                imageLoader.cancelDisplayTask(expandedImageView);
            } else{
                return;
            }
        }
        animating = true;

        if (sale.getCoast().equals("")){
            LoaderSaleDescription loader = new LoaderSaleDescription();
            loader.execute(sale);
        }

        if (fragment.getView()==null){
            return;
        }

        final Rect startBounds = new Rect();
        final Rect finalBounds = new Rect();
        final Point globalOffset = new Point();

        thumbView.getGlobalVisibleRect(startBounds);
        fragment.getView().findViewById(R.id.fragment_sales_container).getGlobalVisibleRect(finalBounds, globalOffset);
        startBounds.offset(-globalOffset.x, -globalOffset.y);
        finalBounds.offset(-globalOffset.x, -globalOffset.y);

        float startScale;
        if ((float) finalBounds.width() / finalBounds.height() > (float) startBounds.width() / startBounds.height()) {
            startScale = (float) startBounds.height() / finalBounds.height();
            float startWidth = startScale * finalBounds.width();
            float deltaWidth = (startWidth - startBounds.width()) / 2;
            startBounds.left -= deltaWidth;
            startBounds.right += deltaWidth;
        } else {
            startScale = (float) startBounds.width() / finalBounds.width();
            float startHeight = startScale * finalBounds.height();
            float deltaHeight = (startHeight - startBounds.height()) / 2;
            startBounds.top -= deltaHeight;
            startBounds.bottom += deltaHeight;
        }



        final AnimatorSet set = new AnimatorSet();
        set.play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left, finalBounds.left))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top, finalBounds.top))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScale, 1f))
                .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScale, 1f));
        mShortAnimationDuration = context.getResources().getInteger(android.R.integer.config_shortAnimTime);
        set.setDuration(mShortAnimationDuration);
        set.setInterpolator(new DecelerateInterpolator());
        set.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                showFieldsAndFAB(sale);
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                animating = false;
            }
        });

        imageLoader.displayImage(sale.getImageUrl(), expandedImageView, options, new ImageLoadingListener() {
            @Override
            public void onLoadingStarted(String s, View view) {
                loadingFinished = false;
            }

            @Override
            public void onLoadingFailed(String s, View view, FailReason failReason) {
                loadingFinished = true;
                showExpandedImageView();
            }

            @Override
            public void onLoadingComplete(String s, View view, Bitmap bitmap) {
                loadingFinished = true;
                palette = Palette.from(bitmap).generate();
                showExpandedImageView();
            }

            @Override
            public void onLoadingCancelled(String s, View view) {
                loadingFinished = false;
            }

            private void showExpandedImageView() {
                thumbView.setAlpha(0f);
                expandedImageView.setVisibility(View.VISIBLE);
                expandedImageView.setPivotX(0f);
                expandedImageView.setPivotY(0f);
                set.start();
            }
        });

        final float startScaleFinal = startScale;
        zoomedImageListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (animating) {
                    return;
                }
                animating = true;
                ActionButton fab = (ActionButton) fragment.getView().findViewById(R.id.fragment_sales_fab);
                fab.setHideAnimation(ActionButton.Animations.SCALE_DOWN);
                fab.getHideAnimation().setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        final LinearLayout layoutFields = (LinearLayout) fragment.getView().findViewById(R.id.fragment_sales_layout_with_fields);
                        Animation animForFields = AnimationUtils.loadAnimation(context, R.anim.scale_sale_texts_to_small);
                        animForFields.setAnimationListener(new Animation.AnimationListener() {
                            @Override
                            public void onAnimationStart(Animation animation) {

                            }

                            @Override
                            public void onAnimationEnd(Animation animation) {
                                layoutFields.setVisibility(View.GONE);
                                AnimatorSet set = new AnimatorSet();
                                set.play(ObjectAnimator.ofFloat(expandedImageView, View.X, startBounds.left))
                                        .with(ObjectAnimator.ofFloat(expandedImageView, View.Y, startBounds.top))
                                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_X, startScaleFinal))
                                        .with(ObjectAnimator.ofFloat(expandedImageView, View.SCALE_Y, startScaleFinal));
                                set.setDuration(mShortAnimationDuration);
                                set.setInterpolator(new DecelerateInterpolator());
                                set.addListener(new AnimatorListenerAdapter() {
                                    @Override
                                    public void onAnimationEnd(Animator animation) {
                                        thumbView.setAlpha(1f);
                                        expandedImageView.setVisibility(View.GONE);
                                        animating = false;
                                    }

                                    @Override
                                    public void onAnimationCancel(Animator animation) {
                                        thumbView.setAlpha(1f);
                                        expandedImageView.setVisibility(View.GONE);
                                        animating = false;
                                    }
                                });
                                set.start();
                            }

                            @Override
                            public void onAnimationRepeat(Animation animation) {

                            }
                        });
                        layoutFields.setAnimation(animForFields);
                        layoutFields.startAnimation(animForFields);
                        int primaryColor = sPref.getInt(SD.PREFS_COLOR_PRIMARY, context.getResources().getColor(R.color.primary));
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            ((AppCompatActivity) context).getWindow().setNavigationBarColor(primaryColor);
                        }
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                fab.hide();
            }
        };
        expandedImageView.setOnClickListener(zoomedImageListener);
    }

    private void showFieldsAndFAB(final Sale sale) {
        if (fragment.getView()==null){
            return;
        }
        int color = context.getResources().getColor(R.color.md_blue_grey_600);
        final LinearLayout layoutFields = (LinearLayout) fragment.getView().findViewById(R.id.fragment_sales_layout_with_fields);
        if (palette!=null) {
            color = palette.getMutedColor(0x000000);
            if (color != 0) {
                layoutFields.setBackgroundColor(color);
            } else {
                color = palette.getVibrantColor(0x000000);
                if (color != 0) {
                    layoutFields.setBackgroundColor(color);
                }
            }
        }
        updateFields(sale);
        layoutFields.setVisibility(View.VISIBLE);
        Animation animForFields = AnimationUtils.loadAnimation(context, R.anim.scale_sale_texts_to_big);
        animForFields.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                ActionButton fab = (ActionButton) fragment.getView().findViewById(R.id.fragment_sales_fab);
                fab.setShowAnimation(ActionButton.Animations.SCALE_UP);
                fab.getShowAnimation().setAnimationListener(new Animation.AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {

                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        animating = false;

                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {

                    }
                });
                fab.show();
                fab.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        addListsAndShowDialogForSelect(sale);
                    }
                });
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
        layoutFields.setAnimation(animForFields);
        layoutFields.startAnimation(animForFields);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            ((AppCompatActivity) context).getWindow().setNavigationBarColor(color);
        }
    }

    private void updateFields(Sale sale) {
        if (fragment.getView()==null){
            return;
        }
        TextView name = (TextView) fragment.getView().findViewById(R.id.fragment_sales_name);
        TextView subTitle = (TextView) fragment.getView().findViewById(R.id.fragment_sales_subtitle);
        TextView coast = (TextView) fragment.getView().findViewById(R.id.fragment_sales_coast);
        TextView period = (TextView) fragment.getView().findViewById(R.id.fragment_sales_period);
        TextView shop = (TextView) fragment.getView().findViewById(R.id.fragment_sales_shop);
        TextView count = (TextView) fragment.getView().findViewById(R.id.fragment_sales_count);

        name.setText(sale.getTitle());

        if (sale.getCount().isEmpty()) {
            count.setText("");
        } else {
            count.setText(sale.getCount() + " ");
        }

        if (sale.getCoast().isEmpty()) {
            coast.setText("");
        } else{
            coast.setText(context.getString(R.string.string_coast_za) + " " + sale.getCoast());
        }

        shop.setText(" " + context.getString(R.string.string_in_shop) + " " + currentShopName);

        if (sale.getPeriodBeginInString().equals(sale.getPeriodFinishInString())) {
            period.setText(sale.getPeriodBeginInString());
        } else {
            period.setText(sale.getPeriodBeginInString() + " - " + sale.getPeriodFinishInString());
        }

        if (sale.getSubTitle().isEmpty()) {
            subTitle.setVisibility(View.GONE);
        } else {
            subTitle.setVisibility(View.VISIBLE);
            subTitle.setText(sale.getSubTitle());
        }
    }

    public void unzoomSale(View expandedImageView) {
        zoomedImageListener.onClick(expandedImageView);
    }

    private class LoaderSaleDescription extends AsyncTask<Sale, Void, Sale> {

        @Override
        protected Sale doInBackground(Sale... params) {
            Sale sale = params[0];
            InternetConnection connection = null;
            StringBuilder allpage = null;
            try {
                connection = new InternetConnection("http://mestoskidki.ru/view_sale.php?city=" + sPref.getString("city", "1") + "&id=" + sale.getSaleId());
                allpage = connection.getPageInStringBuilder();
            } catch(IOException e){
                return sale;
            } finally{
                if (connection!=null){
                    connection.disconnect();
                }
            }

            String title = SalesInternetLoader.getTitle(allpage.toString());
            if (title==null){
                return sale;
            }

            String subTitle = SalesInternetLoader.getSubTitle(allpage.toString());
            if (subTitle==null){
                return sale;
            }

            String coast = SalesInternetLoader.getCoast(allpage.toString());
            if (coast==null){
                return sale;
            }

            String count = SalesInternetLoader.getCount(allpage.toString());

            String coastFor = SalesInternetLoader.getCoastFor(allpage.toString());

            String imageUrl = SalesInternetLoader.getImageUrl(allpage.toString());
            if (imageUrl==null){
                return sale;
            }

            String imageId = SalesInternetLoader.getImageId(allpage.toString());
            if (imageId==null){
                return sale;
            }

            String[] period = SalesInternetLoader.getPeriod(allpage.toString());
            if (period==null){
                return sale;
            }

            sale.setFields(new String[]{sale.getSaleId(), title, subTitle, coast, count, coastFor, imageUrl, imageId, sale.getShop(), period[0], period[1]});
            sale.updateInDB(db);
            return sale;
        }

        @Override
        protected void onPostExecute(Sale sale) {
            super.onPostExecute(sale);
            updateFields(sale);
        }
    }

    public void showHelp() {
        MaterialDialog dialog = new MaterialDialog.Builder(context)
                .title(R.string.string_help)
                .content(R.string.string_help_sales)
                .positiveText(R.string.string_ok).build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
