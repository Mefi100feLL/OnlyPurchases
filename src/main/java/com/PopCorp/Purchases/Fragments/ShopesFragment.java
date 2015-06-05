package com.PopCorp.Purchases.Fragments;

import android.app.Fragment;
import android.app.LoaderManager;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.StaggeredGridLayoutManager;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.PopCorp.Purchases.Activities.MainActivity;
import com.PopCorp.Purchases.BuildConfig;
import com.PopCorp.Purchases.Controllers.ShopesController;
import com.PopCorp.Purchases.Data.Shop;
import com.PopCorp.Purchases.Loaders.ShopesInternetLoader;
import com.PopCorp.Purchases.PurchasesApplication;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;

public class ShopesFragment extends Fragment {
	
	public static final String TAG = ShopesFragment.class.getSimpleName();
	
	private RecyclerView gridView;
	private ProgressBar progress;

	private ShopesController controller;
	private SharedPreferences sPref;

	private MainActivity activity;
	private Menu menu;
	private TextView textViewEmpty;
	private LinearLayout layoutWithSpinnerForCity;
	private Button buttonOk;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_shopes, container, false);
		activity = (MainActivity) getActivity();
		sPref = PreferenceManager.getDefaultSharedPreferences(activity);
		setHasOptionsMenu(true);

		gridView = (RecyclerView) rootView.findViewById(R.id.fragment_shopes_gridview);
		progress = (ProgressBar) rootView.findViewById(R.id.fragment_shopes_progress);
		textViewEmpty = (TextView) rootView.findViewById(R.id.fragment_shopes_textview_empty);
		layoutWithSpinnerForCity = (LinearLayout) rootView.findViewById(R.id.fragment_shopes_city_layout);
		buttonOk = (Button) rootView.findViewById(R.id.fragment_shopes_button_ok);
		
		controller = new ShopesController(this, activity);
		
		StaggeredGridLayoutManager layoutManager = new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL);
		layoutManager.setGapStrategy(StaggeredGridLayoutManager.GAP_HANDLING_MOVE_ITEMS_BETWEEN_SPANS);
		
		gridView.setLayoutManager(layoutManager);
		gridView.setAdapter(controller.getAdapter());
		RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
		gridView.setItemAnimator(itemAnimator);

		if (!sPref.getBoolean(SD.PREFS_FIRST_SELECT_CITY, false) && sPref.getString(SD.PREFS_CITY, "-1").equals("-1")){
			layoutWithSpinnerForCity.setVisibility(View.VISIBLE);
			progress.setVisibility(View.INVISIBLE);
		} else{
			hideLayoutWithSpinnerCity();
		}
		if (!BuildConfig.DEBUG) {
			Tracker t = ((PurchasesApplication) getActivity().getApplication()).getTracker(PurchasesApplication.TrackerName.APP_TRACKER);
			t.setScreenName(this.getClass().getSimpleName());
			t.send(new HitBuilders.AppViewBuilder().build());
		}
		return rootView;
	}

	private void hideLayoutWithSpinnerCity() {
		layoutWithSpinnerForCity.setVisibility(View.INVISIBLE);
		progress.setVisibility(View.VISIBLE);
	}

	@Override
	public void onResume(){
		super.onResume();
		if (getView()!=null) {
			getView().setKeepScreenOn(sPref.getBoolean(SD.PREFS_DISPLAY_NO_OFF, true));
		}
	}

	public void startLoaderFromNet(){
		getLoaderManager().initLoader(ShopesController.ID_FOR_CREATE_SHOPES_LOADER_FROM_NET, new Bundle(), new CallBackForShopesFromNet());
		Loader<ArrayList<Shop>> shopesLoaderFromNET = getLoaderManager().getLoader(ShopesController.ID_FOR_CREATE_SHOPES_LOADER_FROM_NET);
		shopesLoaderFromNET.forceLoad();
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		menu.clear();
		inflater.inflate(R.menu.menu_for_shopes, menu);
		this.menu = menu;
		if (!sPref.getBoolean(SD.PREFS_FIRST_SELECT_CITY, false) && sPref.getString(SD.PREFS_CITY, "-1").equals("-1")){
			showSelectorCityAndHideMenu();
		} else{
			startLoaderFromDb();
		}
	}

	private void showSelectorCityAndHideMenu() {
		buttonOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                controller.showDialogWithCities();
            }
        });
		Handler handler = new Handler();
		handler.postDelayed(new Runnable() {
			@Override
			public void run() {
				hideMenu();
			}
		}, 300);
	}

	public void hideMenu(){
		menu.findItem(R.id.action_shopes_filter).setVisible(false);
	}

	public void showMenu(){
		menu.findItem(R.id.action_shopes_filter).setVisible(true);
	}

	public void startLoaderFromDb() {
		hideLayoutWithSpinnerCity();
		getLoaderManager().initLoader(ShopesController.ID_FOR_CREATE_SHOPES_LOADER_FROM_DB, new Bundle(), controller);
		Loader<Cursor> shopesLoaderFromDB = getLoaderManager().getLoader(ShopesController.ID_FOR_CREATE_SHOPES_LOADER_FROM_DB);
		shopesLoaderFromDB.forceLoad();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_help){
			controller.showHelp();
		} else {
			item.setChecked(true);
			controller.filter(item.getItemId());
		}
		return super.onOptionsItemSelected(item);
	}

	public void checkMenuItem(int id) {
		menu.findItem(R.id.action_shopes_filter).getSubMenu().findItem(id).setChecked(true);
	}

	public void showListView(){
		progress.setVisibility(View.GONE);
		textViewEmpty.setVisibility(View.GONE);
		gridView.setVisibility(View.VISIBLE);
	}

	public void showEmpty(int textEmpty, int drawableEmptyRes){
		progress.setVisibility(View.GONE);
		textViewEmpty.setVisibility(View.VISIBLE);
		textViewEmpty.setText(textEmpty);
		textViewEmpty.setCompoundDrawablesWithIntrinsicBounds(0, drawableEmptyRes, 0, 0);
		gridView.setVisibility(View.GONE);
	}
	
	public class CallBackForShopesFromNet implements LoaderManager.LoaderCallbacks<ArrayList<Shop>> {
		@Override
		public Loader<ArrayList<Shop>> onCreateLoader(int id, Bundle args) {
			Loader<ArrayList<Shop>> result = null;
			if (id == ShopesController.ID_FOR_CREATE_SHOPES_LOADER_FROM_NET){
				result = new ShopesInternetLoader(activity, args, sPref.getString("city", "1"));
			}
			return result;
		}

		@Override
		public void onLoadFinished(Loader<ArrayList<Shop>> loader, ArrayList<Shop> shopes) {
			controller.updateShopes(shopes);
		}

		@Override
		public void onLoaderReset(Loader<ArrayList<Shop>> loader) {
			
		}
	}
}
