package com.PopCorp.Purchases.Fragments;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.NinePatchDrawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.RecognizerIntent;
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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Activities.ProductsActivity;
import com.PopCorp.Purchases.BuildConfig;
import com.PopCorp.Purchases.Controllers.ListController;
import com.PopCorp.Purchases.Data.Product;
import com.PopCorp.Purchases.PurchasesApplication;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.PopCorp.Purchases.Utilites.ShowHideOnScroll;
import com.PopCorp.Purchases.Views.ItemShadowDecorator;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.software.shell.fab.ActionButton;

import java.util.ArrayList;

public class ListFragment extends Fragment {

    public static final String TAG = ListFragment.class.getSimpleName();

    public static final String INTENT_TO_LIST_TITLE = "title";
    public static final String INTENT_TO_LIST_DATELIST = "datelist";

    public static final String INTENT_TO_LIST_RETURNED_LISTITEMS = "array";

    private static final int REQUEST_CODE_FOR_INTENT_TO_PRODUCTS = 1;
    private static final int REQUEST_CODE_FOR_INTENT_SPEECH = 2;

    private SharedPreferences sPref;
    private AppCompatActivity activity;

    private Toolbar toolBar;

    private ShowHideOnScroll touchListener;

    private RecyclerView listView;
    private ActionButton floatingButton;
    private ProgressBar progressbar;
    private TextView textViewEmpty;
    private FrameLayout layoutForSnackBar;
    private TextView textviewForTotal;
    private TextView textviewForTotalBuyedCount;
    private TextView textviewForTotalCount;

    private ListController controller;
    private Menu menu;
    private LinearLayout backgroundTotals;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_list, container, false);
        activity = (AppCompatActivity) getActivity();
        sPref = PreferenceManager.getDefaultSharedPreferences(activity);

        setHasOptionsMenu(true);
        findViewsById(rootView);

        controller = new ListController(this, getArguments().getString(INTENT_TO_LIST_DATELIST), layoutForSnackBar, touchListener);
        listView.setAdapter(controller.getAdapter());

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
                    controller.getDialogForNewItem().setNameItem(spokenText);
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
        if (controller.closeActionMode()) {
            return;
        }
        backToLists();
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

    public void showActionMode() {
        backgroundTotals.setBackgroundResource(R.color.action_mode);
        ((MainActivity) activity).showActionMode();
    }

    public void hideActionMode() {
        backgroundTotals.setBackgroundColor(sPref.getInt(SD.PREFS_COLOR_PRIMARY, activity.getResources().getColor(R.color.primary)));
        ((MainActivity) activity).hideActionMode();
    }

    public void showTotals(String totalBuyed, String total, String size) {
        textviewForTotalBuyedCount.setText(totalBuyed + " " + controller.getCurrentList().getCurrency());
        textviewForTotal.setText(getString(R.string.content_total).replace("%0", size));
        textviewForTotalCount.setText(total + " " + controller.getCurrentList().getCurrency());
    }

    public void displaySpeechRecognizer() {
        try {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            startActivityForResult(intent, REQUEST_CODE_FOR_INTENT_SPEECH);
        } catch (Exception e) {
            if (controller.getDialogForNewItem()!=null) {
                controller.getDialogForNewItem().showError(R.string.notify_no_application_for_record_voice);
            }
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
        textviewForTotal = (TextView) rootView.findViewById(R.id.fragment_list_textview_total);
        textviewForTotalBuyedCount = (TextView) rootView.findViewById(R.id.fragment_list_textview_total_buyed_count);
        textviewForTotalCount = (TextView) rootView.findViewById(R.id.fragment_list_textview_total_count);
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

        floatingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.showDailogForNewItem();
            }
        });
    }
}