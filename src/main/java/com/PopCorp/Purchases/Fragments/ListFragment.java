package com.PopCorp.Purchases.Fragments;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
import android.support.design.widget.TextInputLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
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

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.BuildConfig;
import com.PopCorp.Purchases.PurchasesApplication;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.PopCorp.Purchases.Activities.ProductsActivity;
import com.PopCorp.Purchases.Adapters.CategoriesAdapter;
import com.PopCorp.Purchases.Controllers.ListController;
import com.PopCorp.Purchases.Data.ListItem;
import com.PopCorp.Purchases.Data.Product;
import com.PopCorp.Purchases.Utilites.ShowHideOnScroll;
import com.PopCorp.Purchases.Views.ItemShadowDecorator;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.software.shell.fab.ActionButton;

public class ListFragment extends Fragment {

    public static final String TAG = ListFragment.class.getSimpleName();

    public static final String INTENT_TO_LIST_TITLE = "title";
    public static final String INTENT_TO_LIST_DATELIST = "datelist";

    public static final String INTENT_TO_LIST_RETURNED_LISTITEMS = "array";

    private static final int REQUEST_CODE_FOR_INTENT_TO_PRODUCTS = 1;
    private static final int REQUEST_CODE_FOR_INTENT_SPEECH = 2;

    private SharedPreferences sPref;
    private SharedPreferences.Editor editor;
    private AppCompatActivity activity;

    private Toolbar toolBar;

    private ShowHideOnScroll touchListener;

    private RecyclerView listView;
    private ActionButton floatingButton;
    private ProgressBar progressbar;
    private TextView textViewEmpty;
    private FrameLayout layoutForSnackBar;
    private LinearLayout layoutWithFields;
    private AutoCompleteTextView editTextForName;
    private EditText editTextForCount;
    private Spinner spinnerForEdizm;
    private EditText editTextForCoast;
    private Spinner spinnerForCategory;
    private Spinner spinnerForShop;
    private EditText editTextForComment;
    private CheckBox checkBoxForImportant;
    private TextView textviewForTotal;
    private TextView textviewForTotalBuyedCount;
    private TextView textviewForTotalCount;
    private TextView textviewCurrency;

    private ArrayList<String> edizmsForSpinner;
    private ArrayAdapter<String> adapterForSpinnerEdizm;

    private ArrayList<String> categories;
    private CategoriesAdapter adapterForSpinnerCategory;

    private ArrayList<String> shopesForSpinner;
    private ArrayAdapter<String> adapterForSpinnerShop;

    private ListController controller;
    private Menu menu;
    private TextInputLayout inputLayout;
    private LinearLayout backgroundTotals;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_list, container, false);
        activity = (AppCompatActivity) getActivity();
        sPref = PreferenceManager.getDefaultSharedPreferences(activity);
        editor = sPref.edit();

        setHasOptionsMenu(true);
        findViewsById(rootView);

        controller = new ListController(this, getArguments().getString(INTENT_TO_LIST_DATELIST), layoutForSnackBar, touchListener);
        listView.setAdapter(controller.getAdapter());

        initializeEdizms();
        initializeSpinnerForCategory();
        initializeSpinnerForShop();
        if (!BuildConfig.DEBUG) {
            Tracker t = ((PurchasesApplication) getActivity().getApplication()).getTracker(PurchasesApplication.TrackerName.APP_TRACKER);
            t.setScreenName(this.getClass().getSimpleName());
            t.send(new HitBuilders.AppViewBuilder().build());
        }
        return rootView;
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        menu.clear();
        inflater.inflate(R.menu.menu_for_list, menu);
        this.menu = menu;
        controller.refreshAll();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_all_items) {
            goToProducts();
            return true;
        }
        if (id == R.id.action_change_list) {
            controller.showDialogForEditingList();
            return true;
        }
        if (id == R.id.action_remove_list) {
            controller.removeCurrentList();
            return true;
        }
        if (id == R.id.action_send_list) {
            controller.showDialogForSendingList();
            return true;
        }
        if (id == R.id.action_load_list) {
            controller.loadFromSMS();
            return true;
        }
        if (id == R.id.action_put_alarm) {
            controller.showDialogForAlarm();
            return true;
        }
        if (id == R.id.action_help) {
            controller.showHelp();
            return true;
        }
        if (controller.onOptionsItemSelected(item)) {
            item.setChecked(true);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == REQUEST_CODE_FOR_INTENT_SPEECH) {
                ArrayList<String> results = data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null) {
                    String spokenText = results.get(0);
                    editTextForName.setText(spokenText);
                }
            }
            if (requestCode == REQUEST_CODE_FOR_INTENT_TO_PRODUCTS) {
                ArrayList<Product> returnedArray = data.getParcelableArrayListExtra(INTENT_TO_LIST_RETURNED_LISTITEMS);
                controller.updateListFromProducts(returnedArray);
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (getView() != null) {
            getView().setKeepScreenOn(sPref.getBoolean(SD.PREFS_DISPLAY_NO_OFF, true));
            getView().postDelayed(new Runnable() {
                @Override
                public void run() {
                    showActionButton();
                }
            }, 300);
        }
    }

    public void onBackPressed() {
        if (layoutWithFields.getVisibility() == View.VISIBLE) {
            hideLayoutWithFields();
            return;
        }
        if (controller.closeActionMode()) {
            return;
        }
        backToLists();
    }


    private void hideLayoutWithFields() {
        listView.postDelayed(new Runnable() {
            @Override
            public void run() {
                animateFields(activity, R.anim.list_fields_to_top, new AnimationListener() {
                    @Override
                    public void onAnimationEnd(Animation animation) {
                        layoutWithFields.setVisibility(View.GONE);
                        clearFields();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }

                    @Override
                    public void onAnimationStart(Animation animation) {
                    }
                });
                if (controller.getEditedItem() != null) {
                    controller.setEditedItem(null);
                    animationChangeFabFromEditToAdd();
                }
                touchListener.setIgnore(false);
            }
        }, 200);
    }

    private boolean addOrEditItemFromFields() {
        if (!editTextForName.getText().toString().isEmpty()) {
            String shop = (String) spinnerForShop.getSelectedItem();
            if (spinnerForShop.getSelectedItemPosition() == adapterForSpinnerShop.getCount() - 1) {
                shop = "";
            }
            boolean result = controller.addNewListItem(
                    editTextForName.getText().toString(),
                    editTextForCount.getText().toString(),
                    (String) spinnerForEdizm.getSelectedItem(),
                    editTextForCoast.getText().toString(),
                    (String) spinnerForCategory.getSelectedItem(),
                    shop,
                    editTextForComment.getText().toString(),
                    String.valueOf(checkBoxForImportant.isChecked()));
            if (!result) {
                showToast(R.string.notify_listitem_already_exists);
                return false;
            }
        } else {
            inputLayout.setError(getString(R.string.notify_enter_name_of_listitem));
            return false;
        }
        return true;
    }

    private void showToast(int stringToastRes) {
        controller.showToast(stringToastRes);
    }

    private void showLayoutWithFields() {
        clearFields();
        layoutWithFields.setVisibility(View.VISIBLE);
        animateFields(activity, R.anim.list_fields_from_top, null);
        touchListener.setIgnore(true);
    }

    public void showListView() {
        progressbar.setVisibility(View.GONE);
        textViewEmpty.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    public void showEmpty() {
        progressbar.setVisibility(View.GONE);
        textViewEmpty.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
    }

    private void goToProducts() {
        AnimationListener listener = new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                controller.removeItemsFromTmpArray();
                Intent intent = new Intent(activity, ProductsActivity.class);
                ArrayList<Product> selectedItems = controller.getCurrentList().getSelectedItems();
                intent.putParcelableArrayListExtra(ProductsActivity.INTENT_TO_PRODUCTS_LISTITEMS, selectedItems);
                startActivityForResult(intent, REQUEST_CODE_FOR_INTENT_TO_PRODUCTS);
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        };
        hideActionButton(listener);
    }

    private void putItemInFields(ListItem item) {
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
        checkBoxForImportant.setChecked(item.isImportant());
    }

    public void startEditingItem(ListItem item) {
        putItemInFields(item);
        if (layoutWithFields.getVisibility() == View.GONE) {
            animateFields(activity, R.anim.list_fields_for_editing, new AnimationListener() {
                @Override
                public void onAnimationEnd(Animation animation) {
                    layoutWithFields.setVisibility(View.VISIBLE);
                    touchListener.setIgnore(true);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }

                @Override
                public void onAnimationStart(Animation animation) {
                }
            });
        }
        floatingButton.setImageResource(R.drawable.ic_create_white_24dp);
    }

    private void clearFields() {
        editTextForName.setText("");
        inputLayout.setError(null);
        editTextForCount.setText("1");
        spinnerForEdizm.setSelection(getPositionForEdizm(sPref.getString(SD.PREFS_DEF_EDIZM, getResources().getString(R.string.default_unit_one))));
        editTextForCoast.setText("");
        spinnerForCategory.setSelection(adapterForSpinnerCategory.getCount() - 1);
        spinnerForShop.setSelection(getPositionForShop(shopesForSpinner.get(shopesForSpinner.size() - 1)));
        editTextForComment.setText("");
        checkBoxForImportant.setChecked(false);
        textviewCurrency.setText(controller.getCurrentList().getCurrency());
    }

    public void showActionMode() {
        backgroundTotals.setBackgroundResource(R.color.action_mode);
        ((MainActivity) activity).showActionMode();
    }

    public void hideActionMode() {
        backgroundTotals.setBackgroundColor(sPref.getInt(SD.PREFS_COLOR_PRIMARY, activity.getResources().getColor(R.color.primary)));
        ((MainActivity) activity).hideActionMode();
    }

    private void animationChangeFabFromEditToAdd() {
        AnimationListener listener = new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                floatingButton.setImageResource(R.drawable.ic_add_white_24dp);
                floatingButton.setShowAnimation(ActionButton.Animations.SCALE_UP);
                floatingButton.show();
            }

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        hideActionButton(listener);
    }

    private void animateFields(AppCompatActivity context, int animationResource, AnimationListener listener) {
        Animation animForFields = AnimationUtils.loadAnimation(context, animationResource);
        animForFields.setAnimationListener(listener);
        layoutWithFields.setAnimation(animForFields);
        layoutWithFields.startAnimation(animForFields);
    }


    public void showTotals(String totalBuyed, String total, String size) {
        textviewForTotalBuyedCount.setText(totalBuyed + " " + controller.getCurrentList().getCurrency());
        textviewForTotal.setText(getString(R.string.content_total).replace("%0", size));
        textviewForTotalCount.setText(total + " " + controller.getCurrentList().getCurrency());
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

    public void setTitle(String newName) {
        toolBar.setTitle(newName);
    }

    public void backToLists() {
        AnimationListener listener = new AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                controller.removeItemsFromTmpArray();
                setTitle(activity.getString(R.string.string_lists));
                Fragment fragment = new MenuFragment();
                String tag = MenuFragment.TAG;

                FragmentManager fragmentManager = activity.getFragmentManager();
                fragmentManager.beginTransaction().replace(R.id.activity_main_content_frame, fragment, tag).commit();
            }

            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        };
        hideActionButton(listener);
    }


    public void hideFilterMenuItem() {
        menu.findItem(R.id.action_filter).setVisible(false);
    }

    public void showFilterMenuItem(ArrayList<String> filterShops, String selectedShop) {
        int groupId = 12;
        MenuItem item = menu.findItem(R.id.action_filter);
        item.getSubMenu().clear();
        for (String shop : filterShops) {
            MenuItem addedItem = item.getSubMenu().add(groupId, shop.hashCode(), Menu.NONE, shop);
            if (shop.equals(selectedShop)) {
                addedItem.setChecked(true);
            }
        }
        item.getSubMenu().setGroupCheckable(groupId, true, true);
        item.getSubMenu().setGroupEnabled(groupId, true);
        item.setVisible(true);
    }

    public void showActionButton() {
        if (floatingButton.getAnimation() != null) {
            if (!floatingButton.getAnimation().hasEnded()) {
                return;
            }
        }
        floatingButton.setShowAnimation(ActionButton.Animations.SCALE_UP);
        floatingButton.show();
    }

    public void hideActionButton(AnimationListener listener) {
        if (floatingButton.isHidden()) {
            listener.onAnimationEnd(floatingButton.getHideAnimation());
        } else {
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

    public void hideActionButtonToRight() {
        if (floatingButton.getAnimation() != null) {
            if (!floatingButton.getAnimation().hasEnded()) {
                return;
            }
        }
        floatingButton.setHideAnimation(ActionButton.Animations.ROLL_TO_RIGHT);
        floatingButton.hide();
    }

    public void showActionButtonFromRight() {
        listView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (floatingButton.getAnimation() != null) {
                    if (!floatingButton.getAnimation().hasEnded()) {
                        return;
                    }
                }
                floatingButton.setShowAnimation(ActionButton.Animations.ROLL_FROM_RIGHT);
                floatingButton.show();
            }
        }, 200);
    }


    private void findViewsById(ViewGroup rootView) {
        listView = (RecyclerView) rootView.findViewById(R.id.fragment_list_listview);
        progressbar = (ProgressBar) rootView.findViewById(R.id.fragment_list_progressbar);
        textViewEmpty = (TextView) rootView.findViewById(R.id.fragment_list_textview_empty);
        layoutWithFields = (LinearLayout) rootView.findViewById(R.id.fragment_list_fields_layout);
        editTextForName = (AutoCompleteTextView) rootView.findViewById(R.id.content_list_fields_edittext_name);
        inputLayout = (TextInputLayout) rootView.findViewById(R.id.content_list_fields_input_layout);
        editTextForCount = (EditText) rootView.findViewById(R.id.content_list_fields_edittext_count);
        spinnerForEdizm = (Spinner) rootView.findViewById(R.id.content_list_fields_spinner_edizm);
        editTextForCoast = (EditText) rootView.findViewById(R.id.content_list_fields_edittext_coast);
        spinnerForCategory = (Spinner) rootView.findViewById(R.id.content_list_fields_spinner_category);
        spinnerForShop = (Spinner) rootView.findViewById(R.id.content_list_fields_spinner_shop);
        editTextForComment = (EditText) rootView.findViewById(R.id.content_list_fields_edittext_comment);
        checkBoxForImportant = (CheckBox) rootView.findViewById(R.id.content_list_fields_checkbox_important);
        textviewForTotal = (TextView) rootView.findViewById(R.id.fragment_list_textview_total);
        textviewForTotalBuyedCount = (TextView) rootView.findViewById(R.id.fragment_list_textview_total_buyed_count);
        textviewForTotalCount = (TextView) rootView.findViewById(R.id.fragment_list_textview_total_count);
        textviewCurrency = (TextView) rootView.findViewById(R.id.content_list_fields_currency);
        floatingButton = (ActionButton) rootView.findViewById(R.id.fragment_list_fab);
        layoutForSnackBar = (FrameLayout) rootView.findViewById(R.id.fragment_list_layout_snackbar);
        toolBar = (Toolbar) activity.findViewById(R.id.activity_main_toolbar);

        backgroundTotals = (LinearLayout) rootView.findViewById(R.id.fragment_list_totals_background);
        backgroundTotals.setBackgroundColor(sPref.getInt(SD.PREFS_COLOR_PRIMARY, activity.getResources().getColor(R.color.primary)));

        toolBar.setTitle(getArguments().getString(INTENT_TO_LIST_TITLE));
        LinearLayoutManager mLayoutManager = new LinearLayoutManager(activity);
        listView.setLayoutManager(mLayoutManager);

        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        listView.setItemAnimator(itemAnimator);
        listView.addItemDecoration(new ItemShadowDecorator((NinePatchDrawable) getResources().getDrawable(R.drawable.abc_list_pressed_holo_light)));

        touchListener = new ShowHideOnScroll(floatingButton);
        listView.setOnTouchListener(touchListener);

        ImageView buttonForVoice = (ImageView) rootView.findViewById(R.id.content_list_fields_button_voice);
        ImageView buttonCountPlus = (ImageView) rootView.findViewById(R.id.content_list_fields_count_plus);
        ImageView buttonCountMinus = (ImageView) rootView.findViewById(R.id.content_list_fields_count_minus);

        buttonForVoice.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                displaySpeechRecognizer();
            }
        });

        buttonCountPlus.setOnClickListener(new OnClickListener() {
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

        buttonCountMinus.setOnClickListener(new OnClickListener() {
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

        floatingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (layoutWithFields.getVisibility() == View.GONE) {
                    showLayoutWithFields();
                } else {
                    if (addOrEditItemFromFields()) {
                        hideLayoutWithFields();
                    }
                }
            }
        });
    }


    public void setAutoCompleteAdapter() {
        editTextForName.setAdapter(controller.getAdapterWithProducts());
        editTextForName.setOnItemClickListener(new OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                ListItem item = controller.getSelectedItem(position);
                if (item != null) {
                    putItemInFields(item);
                }
            }
        });
    }

    private void initializeEdizms() {
        edizmsForSpinner = new ArrayList<>(sPref.getStringSet(SD.PREFS_EDIZMS, new LinkedHashSet<String>()));

        adapterForSpinnerEdizm = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, edizmsForSpinner);
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

        adapterForSpinnerCategory = new CategoriesAdapter(activity, categories, colors);
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

        adapterForSpinnerShop = new ArrayAdapter<>(activity, android.R.layout.simple_spinner_item, shopesForSpinner);
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
}