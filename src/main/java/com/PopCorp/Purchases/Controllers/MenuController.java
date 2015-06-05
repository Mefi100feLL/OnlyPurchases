package com.PopCorp.Purchases.Controllers;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.PopupMenu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.Adapters.MenuAdapter;
import com.PopCorp.Purchases.Data.List;
import com.PopCorp.Purchases.DataBase.DB;
import com.PopCorp.Purchases.Fragments.ListFragment;
import com.PopCorp.Purchases.Fragments.MenuFragment;
import com.PopCorp.Purchases.Loaders.LoaderItemsFromSMS;
import com.PopCorp.Purchases.Loaders.LoaderItemsFromSMS.CallbackForLoadingSMS;
import com.PopCorp.Purchases.Loaders.MenuLoader;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.afollestad.materialdialogs.MaterialDialog;
import com.nispok.snackbar.Snackbar;
import com.nispok.snackbar.SnackbarManager;
import com.nispok.snackbar.listeners.ActionClickListener;
import com.nispok.snackbar.listeners.EventListener;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;

public class MenuController implements LoaderCallbacks<Cursor>, CallbackForLoadingSMS, DialogController.CallbackForEditingList {

    public static final int ID_FOR_CREATE_LOADER_FROM_DB = 1;

    private final SharedPreferences sPref;

    private final AppCompatActivity activity;
    private final DB db;

    private final ArrayList<List> lists = new ArrayList<>();
    private MenuAdapter adapter;

    private LoaderItemsFromSMS loadingSms;
    private final ViewGroup layoutForSnackBar;
    private List removedList;
    private final MenuFragment fragment;

    public MenuController(AppCompatActivity activity, ViewGroup layoutForSnackBar, MenuFragment fragment) {
        this.activity = activity;
        this.layoutForSnackBar = layoutForSnackBar;
        this.fragment = fragment;
        sPref = PreferenceManager.getDefaultSharedPreferences(this.activity);
        db = ((MainActivity) activity).getDB();

        setAdapter(new MenuAdapter(activity, lists, this));
    }

    @Override
    public void addNewList(String newName, String currency) {
        String datelist = String.valueOf(Calendar.getInstance().getTimeInMillis());
        long id = db.addRec(DB.TABLE_LISTS, DB.COLUMNS_LISTS, new String[]{newName, datelist, "", currency});
        List newList = new List(db, id, newName, datelist, "", currency);
        addListAndOpen(newList);
    }

    public void addNewListFromJSON(String json) {
        List newList = new List(db, json);
        addListAndOpen(newList);
    }

    private void addListAndOpen(List newList) {
        lists.add(newList);
        adapter.update();
        openListWithAnimation(newList.getName(), newList.getDatelist());
    }

    public void openListWithAnimation(final String title, final String datelist) {
        if (title == null || datelist == null) {
            return;
        }
        Animation.AnimationListener listener = new Animation.AnimationListener() {
            @Override
            public void onAnimationEnd(Animation animation) {
                openList(title, datelist);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }

            @Override
            public void onAnimationStart(Animation animation) {
            }
        };
        fragment.hideActionButton(listener);
    }

    public void openList(String title, String datelist) {
        Fragment fragment = new ListFragment();
        Bundle args = new Bundle();
        args.putString(ListFragment.INTENT_TO_LIST_TITLE, title);
        args.putString(ListFragment.INTENT_TO_LIST_DATELIST, datelist);
        fragment.setArguments(args);
        FragmentManager fragmentManager = activity.getFragmentManager();
        FragmentTransaction transaction = fragmentManager.beginTransaction();
        transaction.replace(R.id.activity_main_content_frame, fragment, ListFragment.TAG).commit();
    }

    private void removeList(List list) {
        removeRemovedList();
        removedList = list;

        lists.remove(list);
        adapter.update();
        if (lists.size() == 0) {
            fragment.showEmpty();
        }
        SnackbarManager.show(Snackbar.with(activity.getApplicationContext())
                .text(activity.getString(R.string.string_removed_list))
                .actionLabel(R.string.string_undo)
                .actionColor(sPref.getInt(SD.PREFS_COLOR_ACCENT, activity.getResources().getColor(R.color.accent)))
                .actionListener(new ActionClickListener() {
                    @Override
                    public void onActionClicked(Snackbar snackbar) {
                        if (removedList != null) {
                            lists.add(removedList);
                            adapter.update();
                            fragment.showListView();
                            removedList = null;
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
                        removeRemovedList();
                    }

                    @Override
                    public void onDismissByReplace(Snackbar snackbar) {

                    }

                    @Override
                    public void onDismissed(Snackbar snackbar) {

                    }
                }), layoutForSnackBar);
    }

    public void removeRemovedList() {
        if (removedList != null) {
            removedList.remove(db);
            removedList = null;
        }
    }

    private void loadListFromSMS(String sms) {
        List newList = List.getListFromSms(db, activity, sms);
        if (newList != null) {
            lists.add(newList);
            adapter.update();
            fragment.showListView();
        } else {
            showToast(R.string.notify_bad_sms);
        }
    }


    public void showPopupMenu(View view, final List list) {
        PopupMenu popupMenu = new PopupMenu(activity, view);
        popupMenu.inflate(R.menu.popup_menu_for_list);
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
                    case R.id.action_change_list: {
                        DialogController.showDialogForEditingList(activity, list, sPref, MenuController.this);
                        return true;
                    }
                    case R.id.action_remove_list: {
                        removeList(list);
                        return true;
                    }
                    case R.id.action_send_list: {
                        DialogController.showDialogForSendingList(activity, list, MenuController.this);
                        return true;
                    }
                    case R.id.action_put_alarm: {
                        DialogController.showDialogForAlarm(db, activity, list);
                        return true;
                    }
                    default:
                        return false;
                }
            }
        });
        popupMenu.show();
    }




    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        Loader<Cursor> result = null;
        if (id == ID_FOR_CREATE_LOADER_FROM_DB) {
            result = new MenuLoader(activity, args, db);
        }
        return result;
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor == null) {
            fragment.showEmpty();
            return;
        }
        if (cursor.moveToFirst()) {
            addListFromCursor(cursor);
            while (cursor.moveToNext()) {
                addListFromCursor(cursor);
            }
        }
        cursor.close();
        adapter.update();
        if (lists.size() == 0) {
            fragment.showEmpty();
        } else {
            fragment.showListView();
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {

    }

    private void addListFromCursor(Cursor cursor) {
        List newList = new List(db, cursor);
        newList.sort(activity);
        if (!lists.contains(newList)){
            lists.add(newList);
        }
    }

    ///////////////////////////////////////// setters and getters
    public DB getDb() {
        return db;
    }


    public MenuAdapter getAdapter() {
        return adapter;
    }

    private void setAdapter(MenuAdapter adapter) {
        this.adapter = adapter;
    }



    public void loadFromSMS() {
        if (loadingSms != null) {
            if (loadingSms.getStatus().equals(AsyncTask.Status.RUNNING)) {
                if (!loadingSms.isCancelled()) {
                    return;
                }
            }
        }
        loadingSms = new LoaderItemsFromSMS(activity, this);
        loadingSms.execute();
    }

    @Override
    public void showToast(int text) {
        SnackbarManager.show(Snackbar.with(activity.getApplicationContext()).actionColor(sPref.getInt(SD.PREFS_COLOR_ACCENT, activity.getResources().getColor(R.color.accent))).text(text), layoutForSnackBar);
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
        loadListFromSMS(sms);
    }

    @Override
    public void onSMSLoaded(ArrayList<HashMap<String, String>> loadedSms) {
        DialogController.showDialogWithSMS(activity, loadedSms, this);
    }


    @Override
    public void onListEdited(List list, String name, String currency) {
        int position = adapter.getPublishItems().indexOf(list);
        list.changeCurrency(db, currency);
        list.rename(db, name);
        adapter.getPublishItems().updateItemAt(position, list);
    }

    public void showDialogForNewList() {
        DialogController.showDialogForNewList(activity, sPref, this);
    }

    public void showHelp() {
        MaterialDialog dialog = new MaterialDialog.Builder(activity)
                .title(R.string.string_help)
                .content(R.string.string_help_menu)
                .positiveText(R.string.string_ok).build();
        dialog.setCanceledOnTouchOutside(false);
        dialog.show();
    }
}
