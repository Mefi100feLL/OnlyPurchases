package com.PopCorp.Purchases.Fragments;

import android.app.Fragment;
import android.content.Context;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.PopCorp.Purchases.BuildConfig;
import com.PopCorp.Purchases.PurchasesApplication;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.Controllers.MenuController;
import com.PopCorp.Purchases.SD;
import com.PopCorp.Purchases.Utilites.ShowHideOnScroll;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;
import com.software.shell.fab.ActionButton;

public class MenuFragment extends Fragment{

	public static final String TAG = MenuFragment.class.getSimpleName();
	public static final String ARGS_TEXT_FROM_JSON = "ARGS_TEXT_FROM_JSON";

	private RecyclerView listView;
	private MenuController controller;
	private SharedPreferences sPref;

	private ActionButton floatingButton;
	private ProgressBar progressbar;
	private TextView textViewEmpty;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_menu, container, false);

		Context context = getActivity();
		sPref = PreferenceManager.getDefaultSharedPreferences(context);
		setHasOptionsMenu(true);

		progressbar = (ProgressBar) rootView.findViewById(R.id.fragment_menu_progressbar);
		textViewEmpty = (TextView) rootView.findViewById(R.id.fragment_menu_textview_empty);

		listView = (RecyclerView) rootView.findViewById(R.id.fragment_menu_listview);
		floatingButton = (ActionButton) rootView.findViewById(R.id.fragment_menu_fab);
		FrameLayout layoutForSnackBar = (FrameLayout) rootView.findViewById(R.id.fragment_menu_layout_snackbar);

		StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
		layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
		
		controller = new MenuController((AppCompatActivity) context, layoutForSnackBar, this);

		floatingButton.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				controller.showDialogForNewList();
			}
		});

		listView.setLayoutManager(layoutManager);
		listView.setAdapter(controller.getAdapter());
		RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
		listView.setItemAnimator(itemAnimator);
		listView.setOnTouchListener(new ShowHideOnScroll(floatingButton));

		getLoaderManager().initLoader(MenuController.ID_FOR_CREATE_LOADER_FROM_DB, new Bundle(), controller);
		
		Bundle args = getArguments();
		if (args!=null){
			String title = args.getString(ListFragment.INTENT_TO_LIST_TITLE);
			String datelist = args.getString(ListFragment.INTENT_TO_LIST_DATELIST);
			if (title!=null && datelist!=null){
				controller.openList(title, datelist);
			} else{
				String listFromJson = args.getString(ARGS_TEXT_FROM_JSON);
				if (listFromJson!=null){
					controller.addNewListFromJSON(listFromJson);
				}
			}
		}
		if (!BuildConfig.DEBUG) {
			Tracker t = ((PurchasesApplication) getActivity().getApplication()).getTracker(PurchasesApplication.TrackerName.APP_TRACKER);
			t.setScreenName(this.getClass().getSimpleName());
			t.send(new HitBuilders.AppViewBuilder().build());
		}
		return rootView;
	}

	public void showListView(){
		progressbar.setVisibility(View.INVISIBLE);
		textViewEmpty.setVisibility(View.INVISIBLE);
		listView.setVisibility(View.VISIBLE);
	}

	public void showEmpty(){
		progressbar.setVisibility(View.INVISIBLE);
		textViewEmpty.setVisibility(View.VISIBLE);
		listView.setVisibility(View.INVISIBLE);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
		inflater.inflate(R.menu.menu_for_menu, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_load_list) {
			controller.loadFromSMS();
			return true;
		}
		if (item.getItemId() == R.id.action_help){
			controller.showHelp();
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onResume(){
		super.onResume();
		if (getView()!=null) {
			getView().setKeepScreenOn(sPref.getBoolean(SD.PREFS_DISPLAY_NO_OFF, true));
			getView().postDelayed(new Runnable() {
				@Override
				public void run() {
					showActionButton(null);
				}
			}, 300);
		}
		getLoaderManager().restartLoader(MenuController.ID_FOR_CREATE_LOADER_FROM_DB, new Bundle(), controller);

		Loader<Cursor> loaderFromDB = getLoaderManager().getLoader(MenuController.ID_FOR_CREATE_LOADER_FROM_DB);
		loaderFromDB.forceLoad();
	}

	@Override
	public void onStop(){
		super.onStop();
		controller.removeRemovedList();
	}

	public void showActionButton(Animation.AnimationListener listener) {
		if (floatingButton.getAnimation() != null) {
			if (!floatingButton.getAnimation().hasEnded()) {
				return;
			}
		}
		floatingButton.setShowAnimation(ActionButton.Animations.SCALE_UP);
		floatingButton.getShowAnimation().setAnimationListener(listener);
		floatingButton.show();
	}

	public void hideActionButton(Animation.AnimationListener listener) {
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
}
