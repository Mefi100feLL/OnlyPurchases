package com.PopCorp.Purchases.Activities;

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ImageView;

import com.PopCorp.Purchases.BuildConfig;
import com.PopCorp.Purchases.Controllers.PreferencesController;
import com.PopCorp.Purchases.Data.List;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Fragments.AllProductsFragment;
import com.PopCorp.Purchases.Fragments.ListFragment;
import com.PopCorp.Purchases.Fragments.MenuFragment;
import com.PopCorp.Purchases.Fragments.PreferencesFragment;
import com.PopCorp.Purchases.Fragments.SalesFragment;
import com.PopCorp.Purchases.Fragments.ShopesFragment;
import com.PopCorp.Purchases.PurchasesApplication;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedHashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    public static final String TO_SETTINGS = "TO_SETTINGS";

    private Toolbar toolBar;
    private Drawer.Result drawer;
    private DB db;
    private SharedPreferences sPref;
    private SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sPref = PreferenceManager.getDefaultSharedPreferences(this);
        setTheme();
        setContentView(R.layout.activity_main);

        if (!BuildConfig.DEBUG) {
            Tracker t = ((PurchasesApplication) this.getApplication()).getTracker(PurchasesApplication.TrackerName.APP_TRACKER);
            t.setScreenName(getClass().getSimpleName());
            t.send(new HitBuilders.AppViewBuilder().build());
        }

        db = new DB(this);
        db.open();
        editor = sPref.edit();
        firstStart();

        toolBar = (Toolbar) findViewById(R.id.activity_main_toolbar);
        setSupportActionBar(toolBar);

        createNewDrawer();
        drawer.closeDrawer();
        updateTheme();

        if (getIntent().getData() != null || getIntent().getExtras() != null) {
            if (getIntent().getExtras() != null) {
                if (getIntent().getExtras().getBoolean(TO_SETTINGS, false)) {
                    drawer.setSelectionByIdentifier(R.string.menu_settings);
                    return;
                }
            }
            if (getJsonFromData(getIntent().getData())!=null) {
                drawer.setSelectionByIdentifier(R.string.menu_lists);
                return;
            }
        }
        drawer.setSelectionByIdentifier(R.string.menu_sales);
    }

    private void createNewDrawer() {
        drawer = new Drawer()
                .withActivity(this)
                .withToolbar(toolBar)
                .withHeader(R.layout.content_header)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.menu_sales).withIdentifier(R.string.menu_sales).withIcon(R.drawable.ic_sale_grey600_24dp).withIconTintingEnabled(true),
                        new PrimaryDrawerItem().withName(R.string.menu_lists).withIdentifier(R.string.menu_lists).withIcon(R.drawable.ic_dashboard_grey600_24dp).withIconTintingEnabled(true),
                        new PrimaryDrawerItem().withName(R.string.menu_all_products).withIdentifier(R.string.menu_all_products).withIcon(R.drawable.ic_view_list_grey600_24dp).withIconTintingEnabled(true),
                        new DividerDrawerItem(),
                        new PrimaryDrawerItem().withName(R.string.menu_settings).withIdentifier(R.string.menu_settings)
                )
                .withOnDrawerItemClickListener(new Drawer.OnDrawerItemClickListener() {
                    @Override
                    public void onItemClick(AdapterView<?> parent, View view, int position, long id, IDrawerItem drawerItem) {
                        Fragment fragment = null;
                        String tag = null;
                        switch (position) {
                            case 0: {
                                fragment = new ShopesFragment();
                                tag = ShopesFragment.TAG;
                                break;
                            }
                            case 1: {
                                fragment = new MenuFragment();
                                tag = MenuFragment.TAG;
                                break;
                            }
                            case 2: {
                                fragment = new AllProductsFragment();
                                tag = AllProductsFragment.TAG;
                                break;
                            }
                            case 4: {
                                fragment = new PreferencesFragment();
                                tag = PreferencesFragment.TAG;
                                break;
                            }
                        }
                        if (fragment != null) {
                            if (getIntent().getData() != null) {
                                Bundle args = new Bundle();
                                args.putString(MenuFragment.ARGS_TEXT_FROM_JSON, getJsonFromData(getIntent().getData()));
                                fragment.setArguments(args);
                            } else if (getIntent().getExtras() != null) {
                                fragment.setArguments(getIntent().getExtras());
                            }
                            FragmentManager fragmentManager = getFragmentManager();
                            fragmentManager.beginTransaction().replace(R.id.activity_main_content_frame, fragment, tag).commit();

                            toolBar.setTitle(getString(drawerItem.getIdentifier()));

                            drawer.closeDrawer();
                        }
                    }
                })
                .build();
        ((ImageView) drawer.getHeader().findViewById(R.id.content_header_image)).setImageResource(sPref.getInt(SD.PREFS_HEADER, R.drawable.teal));
    }

    public void updateTheme() {
        int primaryColor = sPref.getInt(SD.PREFS_COLOR_PRIMARY, getResources().getColor(R.color.primary));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setBackgroundDrawable(new ColorDrawable(primaryColor));
        }
        drawer.setFullscreen(true);
        drawer.setStatusBarColor(PreferencesController.shiftColor(primaryColor));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            getWindow().setNavigationBarColor(primaryColor);
        }
        if (drawer != null) {
            ((ImageView) drawer.getHeader().findViewById(R.id.content_header_image)).setImageResource(sPref.getInt(SD.PREFS_HEADER, R.drawable.teal));
        }
    }

    public void setTheme() {
        int theme = sPref.getInt(SD.PREFS_THEME, R.style.AppTheme);
        setTheme(theme);
        if (drawer != null) {
            int selection = drawer.getCurrentSelection();
            createNewDrawer();
            drawer.setSelection(selection, false);
        }
    }

    @Override
    protected void onNewIntent(Intent newIntent) {
        super.onNewIntent(newIntent);
        setIntent(newIntent);
        drawer.setSelectionByIdentifier(R.string.menu_lists);
    }

    private String getJsonFromData(Uri data) {
        try {
            final String scheme = data.getScheme();
            if (ContentResolver.SCHEME_CONTENT.equals(scheme) || ContentResolver.SCHEME_FILE.equals(scheme)) {
                ContentResolver cr = getContentResolver();
                InputStream is = cr.openInputStream(data);
                if (is == null) {
                    return null;
                }

                StringBuilder buf = new StringBuilder();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String str;
                while ((str = reader.readLine()) != null) {
                    buf.append(str).append("\n");
                }
                is.close();
                return buf.toString();
            }
        } catch (Exception e) {
            return null;
        }
        return null;
    }

    @Override
    public void onBackPressed() {
        if (drawer != null && drawer.isDrawerOpen()) {
            drawer.closeDrawer();
        }
        FragmentManager fragmentManager = getFragmentManager();
        Fragment findedFragment = fragmentManager.findFragmentByTag(ListFragment.TAG);
        if (findedFragment != null) {
            ((ListFragment) findedFragment).onBackPressed();
            return;
        }
        findedFragment = fragmentManager.findFragmentByTag(SalesFragment.TAG);
        if (findedFragment != null) {
            ((SalesFragment) findedFragment).onBackPressed();
            return;
        }
        findedFragment = fragmentManager.findFragmentByTag(AllProductsFragment.TAG);
        if (findedFragment != null) {
            if (((AllProductsFragment) findedFragment).onBackPressed()) {
                return;
            }
        }
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        File directory = Environment.getExternalStorageDirectory();
        if (List.isExternalStorageWritable()) {
            directory = new File(directory.getAbsolutePath() + "/Purchases");
            if (directory.exists()) {
                File[] files = directory.listFiles();
                for (File file : files) {
                    file.delete();
                }
            }
        }
        db.close();
        super.onDestroy();
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (toolBar.isOverflowMenuShowing()) {
                toolBar.hideOverflowMenu();
            } else {
                toolBar.showOverflowMenu();
            }
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_MENU) {
            if (toolBar.isOverflowMenuShowing()) {
                toolBar.hideOverflowMenu();
            } else {
                toolBar.showOverflowMenu();
            }
            return true;
        }
        return super.onKeyUp(keyCode, event);
    }

    public DB getDB() {
        return db;
    }

    public void showActionMode() {
        int primaryColor = getResources().getColor(R.color.action_mode);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawer.setFullscreen(true);
            drawer.setStatusBarColor(PreferencesController.shiftColor(primaryColor));
            getWindow().setNavigationBarColor(primaryColor);
        }
        if (drawer != null) {
            drawer.closeDrawer();
            DrawerLayout drawerLayout = drawer.getDrawerLayout();
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        }
    }

    public void hideActionMode() {
        int primaryColor = sPref.getInt(SD.PREFS_COLOR_PRIMARY, getResources().getColor(R.color.primary));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            drawer.setFullscreen(true);
            drawer.setStatusBarColor(PreferencesController.shiftColor(primaryColor));
            getWindow().setNavigationBarColor(primaryColor);
        }
        if (drawer != null) {
            DrawerLayout drawerLayout = drawer.getDrawerLayout();
            drawerLayout.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        }
    }

    private void firstStart() {
        if (!sPref.getBoolean(String.valueOf(BuildConfig.VERSION_CODE), false)) {
            if (BuildConfig.VERSION_CODE >= 18) {
                putCurrencys();
                putEdizms();
                editFontSizes();
            }
            editor.putBoolean(String.valueOf(BuildConfig.VERSION_CODE), true);
        }
    }

    private void editFontSizes() {
        editor.putString(SD.PREFS_LIST_ITEM_FONT_SIZE, "16").commit();
        editor.putString(SD.PREFS_LIST_ITEM_FONT_SIZE_SMALL, "14").commit();
    }

    private void putCurrencys() {
        Set<String> currencys = sPref.getStringSet(SD.PREFS_CURRENCYS, new LinkedHashSet<String>());
        if (currencys == null) {
            currencys = new LinkedHashSet<>();
        }
        if (!currencys.contains(getString(R.string.default_one_currency))) {
            currencys.add(getString(R.string.default_one_currency));
        }
        if (!currencys.contains(getString(R.string.default_two_currency))) {
            currencys.add(getString(R.string.default_two_currency));
        }
        if (!currencys.contains(getString(R.string.default_three_currency))) {
            currencys.add(getString(R.string.default_three_currency));
        }
        editor.putStringSet(SD.PREFS_CURRENCYS, currencys).commit();
    }

    private void putEdizms() {
        Set<String> units = sPref.getStringSet(SD.PREFS_EDIZMS, new LinkedHashSet<String>());
        if (units == null) {
            units = new LinkedHashSet<>();
        }
        if (!units.contains(getString(R.string.default_unit_one))) {
            units.add(getString(R.string.default_unit_one));
        }
        if (!units.contains(getString(R.string.default_unit_two))) {
            units.add(getString(R.string.default_unit_two));
        }
        if (!units.contains(getString(R.string.default_unit_three))) {
            units.add(getString(R.string.default_unit_three));
        }
        if (!units.contains(getString(R.string.default_unit_four))) {
            units.add(getString(R.string.default_unit_four));
        }
        if (!units.contains(getString(R.string.default_unit_five))) {
            units.add(getString(R.string.default_unit_five));
        }
        editor.putStringSet(SD.PREFS_EDIZMS, units).commit();
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
