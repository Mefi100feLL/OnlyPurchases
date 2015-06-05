package com.PopCorp.Purchases.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Intent;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.design.widget.TextInputLayout;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.PopCorp.Purchases.Adapters.CategoriesAdapter;
import com.PopCorp.Purchases.BuildConfig;
import com.PopCorp.Purchases.Controllers.AllProductsController;
import com.PopCorp.Purchases.Controllers.MenuController;
import com.PopCorp.Purchases.Data.Product;
import com.PopCorp.Purchases.PurchasesApplication;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.PopCorp.Purchases.Utilites.ShowHideOnScroll;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.software.shell.fab.ActionButton;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public class AllProductsFragment extends Fragment {

    public static final String TAG = AllProductsFragment.class.getSimpleName();

    private static final int REQUEST_CODE_FOR_INTENT_SPEECH = 2;

    private Activity context;
    private SharedPreferences sPref;
    private SharedPreferences.Editor editor;

    private RecyclerView listView;
    private LinearLayout fieldsLayout;
    private ActionButton floatingButton;
    private ShowHideOnScroll touchListener;
    private AllProductsController controller;
    private AutoCompleteTextView editTextForName;
    private EditText editTextForCount;
    private Spinner spinnerForEdizm;
    private EditText editTextForCoast;
    private Spinner spinnerForCategory;
    private Spinner spinnerForShop;
    private EditText editTextForComment;
    private CheckBox checkBoxForImportant;

    private ArrayList<String> edizmsForSpinner;
    private ArrayAdapter<String> adapterForSpinnerEdizm;

    private CategoriesAdapter adapterForSpinnerCategory;

    private ArrayList<String> shopesForSpinner;
    private ArrayAdapter<String> adapterForSpinnerShop;
    private ArrayList<String> categories;
    private ProgressBar progressbar;
    private TextView textViewEmpty;
    private TextInputLayout inputLayout;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_products, container, false);

        context = getActivity();
        sPref = PreferenceManager.getDefaultSharedPreferences(context);
        editor = sPref.edit();
        setHasOptionsMenu(true);

        listView = (RecyclerView) rootView.findViewById(R.id.fragment_products_listview);
        progressbar = (ProgressBar) rootView.findViewById(R.id.fragment_products_progressbar);
        textViewEmpty = (TextView) rootView.findViewById(R.id.fragment_products_textview_empty);
        floatingButton = (ActionButton) rootView.findViewById(R.id.fragment_products_fab);
        FrameLayout layoutForSnackBar = (FrameLayout) rootView.findViewById(R.id.fragment_products_layout_snackbar);

        controller = new AllProductsController(context, this, layoutForSnackBar);

        initializeFields(rootView);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        listView.setAdapter(controller.getAdapter());

        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        listView.setItemAnimator(itemAnimator);
        touchListener = new ShowHideOnScroll(floatingButton);
        listView.setOnTouchListener(touchListener);

        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (fieldsLayout.getVisibility() == View.GONE) {
                    clearFields();
                    fieldsLayout.setVisibility(View.VISIBLE);
                    animateFields(context, R.anim.list_fields_from_top, null);
                    touchListener.setIgnore(true);
                } else {
                    final boolean editing = controller.getEditedItem() != null;
                    if (!editTextForName.getText().toString().isEmpty()) {
                        String shop = (String) spinnerForShop.getSelectedItem();
                        if (spinnerForShop.getSelectedItemPosition() == adapterForSpinnerShop.getCount() - 1) {
                            shop = "";
                        }
                        boolean result = controller.addNewProduct(
                                editTextForName.getText().toString(),
                                editTextForCount.getText().toString(),
                                (String) spinnerForEdizm.getSelectedItem(),
                                editTextForCoast.getText().toString(),
                                (String) spinnerForCategory.getSelectedItem(),
                                shop,
                                editTextForComment.getText().toString(),
                                String.valueOf(checkBoxForImportant.isChecked()));
                        if (!result) {
                            controller.showToast(R.string.notify_listitem_already_exists);
                            return;
                        }
                    } else {
                        inputLayout.setError(getString(R.string.notify_enter_name_of_listitem));
                        return;
                    }
                    listView.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            animateFields(context, R.anim.list_fields_to_top, new Animation.AnimationListener() {
                                @Override
                                public void onAnimationStart(Animation animation) {

                                }

                                @Override
                                public void onAnimationEnd(Animation animation) {
                                    fieldsLayout.setVisibility(View.GONE);
                                    clearFields();
                                }

                                @Override
                                public void onAnimationRepeat(Animation animation) {

                                }
                            });
                            if (editing) {
                                animationChangeFabFromEditToAdd();
                            }
                        }
                    }, 200);
                    touchListener.setIgnore(false);
                }
            }
        });

        getLoaderManager().initLoader(MenuController.ID_FOR_CREATE_LOADER_FROM_DB, new Bundle(), controller);

        Loader<Cursor> loaderFromDB = getLoaderManager().getLoader(AllProductsController.ID_FOR_CREATE_LOADER_FROM_DB);
        loaderFromDB.forceLoad();

        if (!BuildConfig.DEBUG) {
            Tracker t = ((PurchasesApplication) getActivity().getApplication()).getTracker(PurchasesApplication.TrackerName.APP_TRACKER);
            t.setScreenName(this.getClass().getSimpleName());
            t.send(new HitBuilders.AppViewBuilder().build());
        }
        return rootView;
    }

    public void showListView(){
        progressbar.setVisibility(View.GONE);
        textViewEmpty.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    public void showEmpty(int text, int drawable){
        progressbar.setVisibility(View.GONE);
        textViewEmpty.setVisibility(View.VISIBLE);
        textViewEmpty.setText(text);
        textViewEmpty.setCompoundDrawablesWithIntrinsicBounds(0, drawable, 0, 0);
        listView.setVisibility(View.GONE);
    }

    private void initializeFields(ViewGroup rootView) {
        fieldsLayout = (LinearLayout) rootView.findViewById(R.id.fragment_products_fields_layout);
        editTextForName = (AutoCompleteTextView) rootView.findViewById(R.id.content_product_fields_edittext_name);
        inputLayout = (TextInputLayout) rootView.findViewById(R.id.content_product_fields_input_layout);
        editTextForCount = (EditText) rootView.findViewById(R.id.content_product_fields_edittext_count);
        spinnerForEdizm = (Spinner) rootView.findViewById(R.id.content_product_fields_spinner_edizm);
        editTextForCoast = (EditText) rootView.findViewById(R.id.content_product_fields_edittext_coast);
        spinnerForCategory = (Spinner) rootView.findViewById(R.id.content_product_fields_spinner_category);
        spinnerForShop = (Spinner) rootView.findViewById(R.id.content_product_fields_spinner_shop);
        editTextForComment = (EditText) rootView.findViewById(R.id.content_product_fields_edittext_comment);
        checkBoxForImportant = (CheckBox) rootView.findViewById(R.id.content_product_fields_checkbox_important);
        ImageView buttonCountPlus = (ImageView) rootView.findViewById(R.id.content_product_fields_count_plus);
        ImageView buttonCountMinus = (ImageView) rootView.findViewById(R.id.content_product_fields_count_minus);

        ImageView buttonForVoice = (ImageView) rootView.findViewById(R.id.content_product_fields_button_voice);
        buttonForVoice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySpeechRecognizer();
            }
        });

        buttonCountPlus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    BigDecimal count = new BigDecimal(editTextForCount.getText().toString());
                    count = count.add(new BigDecimal("1"));
                    editTextForCount.setText(count.toString());
                } catch (Exception ignored) {

                }
            }
        });

        buttonCountMinus.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    BigDecimal count = new BigDecimal(editTextForCount.getText().toString());
                    if (count.doubleValue() >= 1) {
                        count = count.subtract(new BigDecimal("1"));
                        editTextForCount.setText(count.toString());
                    }
                } catch (Exception ignored) {

                }
            }
        });

        initializeEdizms();
        initializeSpinnerForCategory();
        initializeSpinnerForShop();
    }

    private void displaySpeechRecognizer() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, REQUEST_CODE_FOR_INTENT_SPEECH);
        } catch (Exception e) {
            controller.showToast(R.string.notify_no_application_for_record_voice);
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_for_all_products, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_help){
            controller.showHelp();
        } else {
            item.setChecked(true);
            controller.sort(item.getItemId());
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onResume(){
        super.onResume();
        listView.setKeepScreenOn(sPref.getBoolean(SD.PREFS_DISPLAY_NO_OFF, true));
        listView.postDelayed(new Runnable() {
            @Override
            public void run() {
                showActionButton(null);
            }
        }, 300);
    }

    @Override
    public void onStop(){
        super.onStop();
        controller.removeProductFromTmpArray();
    }

    public boolean onBackPressed() {
        if (fieldsLayout.getVisibility() == View.VISIBLE) {
            touchListener.setIgnore(false);
            animateFields(context, R.anim.list_fields_to_top, new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fieldsLayout.setVisibility(View.GONE);
                    clearFields();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
            if (controller.getEditedItem()!=null){
                animationChangeFabFromEditToAdd();
                controller.setEditedItem(null);
            }
            return true;
        }
        return false;
    }

    private void clearFields() {
        editTextForName.setText("");
        inputLayout.setError(null);
        editTextForCount.setText("1.0");
        spinnerForEdizm.setSelection(getPositionForEdizm(sPref.getString(SD.PREFS_DEF_EDIZM, getResources().getString(R.string.default_unit_one))));
        editTextForCoast.setText("");
        spinnerForCategory.setSelection(adapterForSpinnerCategory.getCount() - 1);
        spinnerForShop.setSelection(getPositionForShop(shopesForSpinner.get(shopesForSpinner.size() - 1)));
        editTextForComment.setText("");
        checkBoxForImportant.setChecked(false);
    }

    public void putItemInFields(Product item) {
        editTextForName.setText(item.getName());
        inputLayout.setError(null);
        editTextForName.requestFocus();
        editTextForName.dismissDropDown();
        editTextForCount.setText(item.getCountInString());
        spinnerForEdizm.setSelection(getPositionForEdizm(item.getEdizm()));
        editTextForCoast.setText(item.getCoastInString());
        putCategoryInSpinner(item.getCategory());
        spinnerForShop.setSelection(getPositionForShop(item.getShop()));
        editTextForComment.setText(item.getComment());
        checkBoxForImportant.setChecked(item.isFavorite());

        if (fieldsLayout.getVisibility() == View.GONE) {
            animateFields(context, R.anim.list_fields_from_top, new Animation.AnimationListener() {

                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    fieldsLayout.setVisibility(View.VISIBLE);
                    touchListener.setIgnore(true);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            });
        }
    }

    private void initializeEdizms() {
        edizmsForSpinner = new ArrayList<>(sPref.getStringSet(SD.PREFS_EDIZMS, new LinkedHashSet<String>()));

        adapterForSpinnerEdizm = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, edizmsForSpinner);
        adapterForSpinnerEdizm.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerForEdizm.setAdapter(adapterForSpinnerEdizm);
        spinnerForEdizm.setSelection(getPositionForEdizm(sPref.getString(SD.PREFS_DEF_EDIZM, getResources().getString(R.string.default_unit_one))));

        spinnerForEdizm.setOnItemSelectedListener(new Spinner.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                editTextForCoast.setHint(getResources().getString(R.string.string_coast_za_ed) + " " + spinnerForEdizm.getItemAtPosition(position));
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
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


    private void initializeSpinnerForCategory() {
        categories = controller.getCategories();
        ArrayList<Integer> colors = controller.getColors();

        adapterForSpinnerCategory = new CategoriesAdapter(context, categories, colors);
        adapterForSpinnerCategory.setDropDownViewResource(R.layout.item_list_category);
        spinnerForCategory.setAdapter(adapterForSpinnerCategory);
        spinnerForCategory.setSelection(adapterForSpinnerCategory.getCount() - 1);//select no category default
    }

    private void putCategoryInSpinner(String category) {
        if (categories.contains(category)) {
            spinnerForCategory.setSelection(adapterForSpinnerCategory.getItemPosition(category));
        } else {
            spinnerForCategory.setSelection(adapterForSpinnerCategory.getCount() - 1);
        }
    }


    private void initializeSpinnerForShop() {
        shopesForSpinner = new ArrayList<>(sPref.getStringSet(SD.PREFS_SHOPES, new LinkedHashSet<String>()));
        shopesForSpinner.add(getResources().getString(R.string.string_no_shop));//add item for no shop

        adapterForSpinnerShop = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, shopesForSpinner);
        adapterForSpinnerShop.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerForShop.setAdapter(adapterForSpinnerShop);
        spinnerForShop.setSelection(getPositionForShop(shopesForSpinner.get(shopesForSpinner.size() - 1)));
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

    private void animateFields(Activity context, int animationResource, Animation.AnimationListener listener) {
        Animation animForFields = AnimationUtils.loadAnimation(context, animationResource);
        animForFields.setAnimationListener(listener);
        fieldsLayout.setAnimation(animForFields);
        fieldsLayout.startAnimation(animForFields);
    }

    private void showActionButton(Animation.AnimationListener listener) {
        if (floatingButton.getAnimation() != null) {
            if (!floatingButton.getAnimation().hasEnded()) {
                return;
            }
        }
        floatingButton.setShowAnimation(ActionButton.Animations.SCALE_UP);
        floatingButton.getShowAnimation().setAnimationListener(listener);
        floatingButton.show();
    }

    private void hideActionButton(Animation.AnimationListener listener) {
        if (floatingButton.isHidden()){
            listener.onAnimationEnd(floatingButton.getHideAnimation());
        } else{
            if (floatingButton.getAnimation() != null) {
                if (!floatingButton.getAnimation().hasEnded()) {
                    return;
                }
            }
            floatingButton.setHideAnimation(ActionButton.Animations.SCALE_DOWN);
            floatingButton.getHideAnimation().setAnimationListener(listener);
            floatingButton.hide();
        }
    }

    private void animationChangeFabFromEditToAdd() {
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                if (floatingButton.getAnimation() != null) {
                    if (!floatingButton.getAnimation().hasEnded()) {
                        return;
                    }
                }
                floatingButton.setImageResource(R.drawable.ic_add_white_24dp);
                floatingButton.setShowAnimation(ActionButton.Animations.SCALE_UP);
                floatingButton.show();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        hideActionButton(listener);
    }

    public void animationChangeFabFromAddToEdit() {
        if (floatingButton.isHidden()){
            if (floatingButton.getAnimation() != null) {
                if (!floatingButton.getAnimation().hasEnded()) {
                    return;
                }
            }
            floatingButton.setImageResource(R.drawable.ic_create_white_24dp);
            floatingButton.setShowAnimation(ActionButton.Animations.SCALE_UP);
            floatingButton.show();
        } else {
            Animation.AnimationListener listener = new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {

                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (floatingButton.getAnimation() != null) {
                        if (!floatingButton.getAnimation().hasEnded()) {
                            return;
                        }
                    }
                    floatingButton.setImageResource(R.drawable.ic_create_white_24dp);
                    floatingButton.setShowAnimation(ActionButton.Animations.SCALE_UP);
                    floatingButton.show();
                }

                @Override
                public void onAnimationRepeat(Animation animation) {

                }
            };
            hideActionButton(listener);
        }
    }
}