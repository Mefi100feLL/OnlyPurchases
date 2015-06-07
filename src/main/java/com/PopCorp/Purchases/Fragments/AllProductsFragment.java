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
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

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

import java.util.ArrayList;

public class AllProductsFragment extends Fragment {

    public static final String TAG = AllProductsFragment.class.getSimpleName();

    private static final int REQUEST_CODE_FOR_INTENT_SPEECH = 2;

    private SharedPreferences sPref;

    private RecyclerView listView;
    private ActionButton floatingButton;
    private AllProductsController controller;
    private ProgressBar progressbar;
    private TextView textViewEmpty;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_products, container, false);

        Activity context = getActivity();
        sPref = PreferenceManager.getDefaultSharedPreferences(context);
        setHasOptionsMenu(true);

        listView = (RecyclerView) rootView.findViewById(R.id.fragment_products_listview);
        progressbar = (ProgressBar) rootView.findViewById(R.id.fragment_products_progressbar);
        textViewEmpty = (TextView) rootView.findViewById(R.id.fragment_products_textview_empty);
        floatingButton = (ActionButton) rootView.findViewById(R.id.fragment_products_fab);
        FrameLayout layoutForSnackBar = (FrameLayout) rootView.findViewById(R.id.fragment_products_layout_snackbar);

        controller = new AllProductsController(context, this, layoutForSnackBar);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(context);
        listView.setLayoutManager(mLayoutManager);
        listView.setAdapter(controller.getAdapter());

        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        listView.setItemAnimator(itemAnimator);
        ShowHideOnScroll touchListener = new ShowHideOnScroll(floatingButton);
        listView.setOnTouchListener(touchListener);

        floatingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.showDailogForNewItem();
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
        }
        super.onActivityResult(requestCode, resultCode, data);
    }
}