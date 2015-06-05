package com.PopCorp.Purchases.Activities;

import java.util.ArrayList;

import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.PopCorp.Purchases.BuildConfig;
import com.PopCorp.Purchases.Controllers.PreferencesController;
import com.PopCorp.Purchases.PurchasesApplication;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.Controllers.MenuController;
import com.PopCorp.Purchases.Controllers.ProductsController;
import com.PopCorp.Purchases.Data.Product;
import com.PopCorp.Purchases.Fragments.ListFragment;
import com.PopCorp.Purchases.SD;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.software.shell.fab.ActionButton;

public class ProductsActivity extends AppCompatActivity {

    public static final String INTENT_TO_PRODUCTS_LISTITEMS = "array";

    private RecyclerView listView;
    private ActionButton floatingButton;
    private ProgressBar progress;
    private TextView textViewEmpty;

    private ProductsController controller;
    private SharedPreferences sPref;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        setContentView(R.layout.activity_products);

        if (!BuildConfig.DEBUG) {
            Tracker t = ((PurchasesApplication) this.getApplication()).getTracker(PurchasesApplication.TrackerName.APP_TRACKER);
            t.setScreenName(getClass().getSimpleName());
            t.send(new HitBuilders.AppViewBuilder().build());
        }

        Toolbar toolBar = (Toolbar) findViewById(R.id.activity_products_toolbar);
        progress = (ProgressBar) findViewById(R.id.activity_products_progressbar);
        textViewEmpty = (TextView) findViewById(R.id.activity_products_textview_empty);
        setSupportActionBar(toolBar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setHomeButtonEnabled(true);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        updateTheme();

        listView = (RecyclerView) findViewById(R.id.activity_products_listview);
        floatingButton = (ActionButton) findViewById(R.id.activity_products_fab);
        floatingButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                hideFab(new AnimationListener() {
                    @Override
                    public void onAnimationStart(Animation animation) {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation) {
                        applyAndGoBack();
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation) {
                    }
                });
            }
        });

        ArrayList<Product> array = getIntent().getParcelableArrayListExtra(INTENT_TO_PRODUCTS_LISTITEMS);
        controller = new ProductsController(this, array);

        LinearLayoutManager mLayoutManager = new LinearLayoutManager(this);
        listView.setLayoutManager(mLayoutManager);
        listView.setAdapter(controller.getAdapter());

        RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
        listView.setItemAnimator(itemAnimator);

        getSupportLoaderManager().initLoader(MenuController.ID_FOR_CREATE_LOADER_FROM_DB, new Bundle(), controller);

        Loader<Cursor> loaderFromDB = getSupportLoaderManager().getLoader(MenuController.ID_FOR_CREATE_LOADER_FROM_DB);
        loaderFromDB.forceLoad();
        floatingButton.setKeepScreenOn(sPref.getBoolean(SD.PREFS_DISPLAY_NO_OFF, true));
    }

    private void applyAndGoBack() {
        Intent intent = new Intent();
        intent.putParcelableArrayListExtra(ListFragment.INTENT_TO_LIST_RETURNED_LISTITEMS, controller.apply());
        setResult(RESULT_OK, intent);
        finish();
    }

    private void setTheme() {
        int theme = sPref.getInt(SD.PREFS_THEME, R.style.AppTheme);
        setTheme(theme);
    }

    private void updateTheme() {
        int primaryColor = sPref.getInt(SD.PREFS_COLOR_PRIMARY, getResources().getColor(R.color.primary));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(primaryColor));
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setStatusBarColor(PreferencesController.shiftColor(primaryColor));
            getWindow().setNavigationBarColor(primaryColor);
        }
    }

    @Override
    public void onPostResume() {
        super.onPostResume();
        listView.postDelayed(new Runnable() {
            @Override
            public void run() {
                showActionButton();
            }
        }, 300);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_for_products, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        hideFab(new AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                finish();
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void hideFab(AnimationListener listener) {
        if (floatingButton.getAnimation() != null) {
            if (!floatingButton.getAnimation().hasEnded()) {
                return;
            }
        }
        floatingButton.setHideAnimation(ActionButton.Animations.SCALE_DOWN);
        floatingButton.getHideAnimation().setAnimationListener(listener);
        floatingButton.hide();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home: {
                onBackPressed();
                break;
            }
            default: {
                item.setChecked(true);
                controller.sort(item.getItemId());
            }
        }
        return super.onOptionsItemSelected(item);
    }

    public void showListView() {
        progress.setVisibility(View.GONE);
        textViewEmpty.setVisibility(View.GONE);
        listView.setVisibility(View.VISIBLE);
    }

    public void showEmpty() {
        progress.setVisibility(View.GONE);
        textViewEmpty.setVisibility(View.VISIBLE);
        listView.setVisibility(View.GONE);
    }

    @Override
    public boolean onKeyUp(int keyCode, @NonNull KeyEvent event) {
        return keyCode == KeyEvent.KEYCODE_MENU || super.onKeyUp(keyCode, event);
    }

    private void showActionButton() {
        if (floatingButton.getAnimation() != null) {
            if (!floatingButton.getAnimation().hasEnded()) {
                return;
            }
        }
        floatingButton.setShowAnimation(ActionButton.Animations.SCALE_UP);
        floatingButton.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        controller.closeDB();
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (!BuildConfig.DEBUG) {
            GoogleAnalytics.getInstance(this).reportActivityStart(this);
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
        if (!BuildConfig.DEBUG) {
            GoogleAnalytics.getInstance(this).reportActivityStop(this);
        }
    }
}
