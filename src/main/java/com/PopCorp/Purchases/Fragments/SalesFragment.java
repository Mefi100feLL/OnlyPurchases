package com.PopCorp.Purchases.Fragments;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.LoaderManager.LoaderCallbacks;
import android.content.Loader;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.PopCorp.Purchases.BuildConfig;
import com.PopCorp.Purchases.Controllers.SalesController;
import com.PopCorp.Purchases.Data.Sale;
import com.PopCorp.Purchases.Loaders.SalesInternetLoader;
import com.PopCorp.Purchases.PurchasesApplication;
import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.SD;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.Tracker;

import java.util.ArrayList;

public class SalesFragment extends Fragment {
	
	public static final String TAG = SalesFragment.class.getSimpleName();
	public static final String CURRENT_SHOP_ID_TO_SALES_FRAGMENT = "currentShopID";
	public static final String CURRENT_SHOP_NAME_TO_SALES_FRAGMENT = "currentShopName";
	
	private RecyclerView gridView;
	private ProgressBar progress;
	private TextView textViewEmpty;
	private Toolbar toolBar;

	private SalesController controller;
	private SharedPreferences sPref;

	private AppCompatActivity context;

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_sales, container, false);
		context = (AppCompatActivity) getActivity();
		sPref = PreferenceManager.getDefaultSharedPreferences(context);
		setHasOptionsMenu(true);

		gridView = (RecyclerView) rootView.findViewById(R.id.fragment_sales_gridview);
		progress = (ProgressBar) rootView.findViewById(R.id.fragment_sales_progress);
		textViewEmpty = (TextView) rootView.findViewById(R.id.fragment_sales_textview_empty);

		String currentShopId = getArguments().getString(CURRENT_SHOP_ID_TO_SALES_FRAGMENT);
		String currentShopName = getArguments().getString(CURRENT_SHOP_NAME_TO_SALES_FRAGMENT);
		
		toolBar = (Toolbar) getActivity().findViewById(R.id.activity_main_toolbar);
		toolBar.setTitle(currentShopName);
		
		controller = new SalesController(this, context, currentShopId, currentShopName);

		GridLayoutManager layoutManager = new GridLayoutManager(context, 2);

		gridView.setLayoutManager(layoutManager);
		gridView.setAdapter(controller.getAdapter());
		RecyclerView.ItemAnimator itemAnimator = new DefaultItemAnimator();
		gridView.setItemAnimator(itemAnimator);

		getLoaderManager().initLoader(SalesController.ID_FOR_CREATE_SALES_LOADER_FROM_DB, new Bundle(), controller);
		Loader<Cursor> loaderFromDB = getLoaderManager().getLoader(SalesController.ID_FOR_CREATE_SALES_LOADER_FROM_DB);
		loaderFromDB.forceLoad();
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
		inflater.inflate(R.menu.menu_for_sales, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (item.getItemId() == R.id.action_help){
			controller.showHelp();
		}
		return super.onOptionsItemSelected(item);
	}

	public void loadSalesFromNet(){
		getLoaderManager().initLoader(SalesController.ID_FOR_CREATE_SALES_LOADER_FROM_NET, new Bundle(), new SalesLoaderCallbacks());
		Loader<String> loaderFromNet = getLoaderManager().getLoader(SalesController.ID_FOR_CREATE_SALES_LOADER_FROM_NET);
		loaderFromNet.forceLoad();
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

	@Override
	public void onResume(){
		super.onResume();
		if (getView()!=null) {
			getView().setKeepScreenOn(sPref.getBoolean(SD.PREFS_DISPLAY_NO_OFF, true));
		}
	}
	
	public class SalesLoaderCallbacks implements LoaderCallbacks<ArrayList<Sale>>{
		@Override
		public Loader<ArrayList<Sale>> onCreateLoader(int id, Bundle args) {
			Loader<ArrayList<Sale>> result = null;
			if (id == SalesController.ID_FOR_CREATE_SALES_LOADER_FROM_NET){
				result = new SalesInternetLoader(context, controller.getCurrentShop());
			}
			return result;
		}

		@Override
		public void onLoadFinished(Loader<ArrayList<Sale>> loader, ArrayList<Sale> data) {
			controller.updateSales(data);
		}

		@Override
		public void onLoaderReset(Loader<ArrayList<Sale>> loader) {

		}
	}

	public void onBackPressed() {
		if (getView()!=null) {
			ImageView expandedImageView = (ImageView) getView().findViewById(R.id.fragment_sales_zoomed_image);
			if (expandedImageView.getVisibility() == View.VISIBLE) {
				controller.unzoomSale(expandedImageView);
				return;
			}
		}
		Fragment fragment = new ShopesFragment();
		String tag = ShopesFragment.TAG;
		toolBar.setTitle(R.string.menu_sales);

		FragmentManager fragmentManager = context.getFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.activity_main_content_frame, fragment, tag).commit();
	}

	@Override
	public void onStop(){
		super.onStop();
		int primaryColor = sPref.getInt(SD.PREFS_COLOR_PRIMARY, context.getResources().getColor(R.color.primary));
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			context.getWindow().setNavigationBarColor(primaryColor);
		}
	}
}
