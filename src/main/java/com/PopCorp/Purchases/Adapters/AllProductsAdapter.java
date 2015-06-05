package com.PopCorp.Purchases.Adapters;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.PopCorp.Purchases.Controllers.AllProductsController;
import com.PopCorp.Purchases.Data.Product;
import com.PopCorp.Purchases.Fragments.AllProductsFragment;
import com.PopCorp.Purchases.R;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.ListIterator;

public class AllProductsAdapter extends RecyclerView.Adapter<AllProductsAdapter.ViewHolder> implements Filterable{

	public static final String FILTER_TYPE_CATEGORIES = "CATEGORIES";
	public static final String FILTER_TYPE_FAVORITE = "FAVORITE";

	private Context context;
	private ArrayList<Product> items;
	private ArrayList<Product> publishItems;
	private ArrayList<String> categories;
	private ArrayList<Integer> colors;
	private AllProductsController controller;
	private String currentSort;
	private AllProductsFragment fragment;
	private int textOfEmpty = R.string.string_list_of_products_clear;
	private int drawableEmpty = R.drawable.ic_no_products;

	public AllProductsAdapter(AllProductsFragment fragment, AllProductsController controller, ArrayList<Product> items, ArrayList<String> categories, ArrayList<Integer> colors){
		super();
		this.context = fragment.getActivity();
		this.fragment = fragment;
		this.controller = controller;
		this.items = items;
		this.categories = categories;
		this.colors = colors;
		publishItems = new ArrayList<>();
		publishItems.addAll(items);
	}

	public ArrayList<Product> getPublishItems() {
		return publishItems;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
		public View view;
		public TextView textName;
		public ImageView buttonMenu;
		public ImageView category;
		private ClickListener clickListener;

		public ViewHolder(View view) {
			super(view);
			this.view = view;
			textName = (TextView) view.findViewById(R.id.item_product_in_all_name);
			buttonMenu = (ImageView) view.findViewById(R.id.item_product_in_all_overflow);
			category = (ImageView) view.findViewById(R.id.item_product_in_all_image);
			view.setOnClickListener(this);
		}


		public void setClickListener(ClickListener clickListener) {
			this.clickListener = clickListener;
		}

		@Override
		public void onClick(View v) {
			clickListener.onClick(v, getAdapterPosition());
		}

		public interface ClickListener {
			void onClick(View v, int position);
		}
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@Override
	public int getItemCount() {
		return publishItems.size();
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		Product item = publishItems.get(position);

		holder.textName.setText(item.getName());
		if (categories.contains(item.getCategory())){
			int pos = categories.indexOf(item.getCategory());
			int color = colors.get(pos);
			if (color == context.getResources().getColor(android.R.color.transparent)){
				color = context.getResources().getColor(R.color.md_blue_grey_500);
			}
			holder.category.setBackgroundColor(color);
		} else{
			holder.category.setBackgroundColor(context.getResources().getColor(R.color.md_blue_grey_500));
		}
		holder.buttonMenu.setTag(item);
		holder.buttonMenu.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				controller.showPopupMenu(v, (Product) v.getTag());
			}
		});
		holder.setClickListener(new ViewHolder.ClickListener() {
			@Override
			public void onClick(View view, int position) {
				view.setTag(position);
			}
		});

	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_in_all, parent, false);

		return new ViewHolder(v);
	}


	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {
			@SuppressWarnings("unchecked")
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				ArrayList<Product> newItems = (ArrayList<Product>) results.values;
				ListIterator<Product> iterator = publishItems.listIterator();
				while (iterator.hasNext()){
					Product item = iterator.next();
					if (!newItems.contains(item)){
						iterator.remove();
					}
				}
				ArrayList<Product> tmpItems = new ArrayList<>(newItems);
				tmpItems.removeAll(publishItems);
				publishItems.addAll(tmpItems);

				sort();
				notifyDataSetChanged();
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				ArrayList<Product> FilteredArrayNames = new ArrayList<>();
				currentSort = (String) constraint;

				if (!constraint.equals(FILTER_TYPE_FAVORITE)){
					results.count = items.size();
					results.values = items;
					textOfEmpty = R.string.string_list_of_products_clear;
					drawableEmpty = R.drawable.ic_no_products;
					return results;
				}

				for (int i = 0; i < items.size(); i++) {
					Product item = items.get(i);
					if (item.isFavorite()){
						FilteredArrayNames.add(item);
					}
				}

				results.count = FilteredArrayNames.size();
				results.values = FilteredArrayNames;
				textOfEmpty = R.string.string_list_of_favorite_clear;
				drawableEmpty = R.drawable.ic_no_favorite_products;
				return results;
			}
		};
		return filter;
	}

	public void sort(){
		if (currentSort.equals(FILTER_TYPE_CATEGORIES)){
			Collections.sort(publishItems, new SortOnlyCategories());
		} else{
			Collections.sort(publishItems, new SortOnlyNames());
		}
		if (publishItems.size()==0){
			fragment.showEmpty(textOfEmpty, drawableEmpty);
		} else{
			fragment.showListView();
		}
	}

	public class SortOnlyNames implements Comparator<Product>{
		public int compare(Product p1, Product p2){
			return p1.getName().compareToIgnoreCase(p2.getName());
		}
	}

	public class SortOnlyCategories implements Comparator<Product>{
		public int compare(Product p1, Product p2){
			if ((p1.getCategory().equals(p2.getCategory()))){
				return p1.getName().compareToIgnoreCase(p2.getName());
			} else{
				if (categories.contains(p1.getCategory()) && categories.contains(p2.getCategory())){
					if (categories.indexOf(p1.getCategory()) > categories.indexOf(p2.getCategory())){
						return 1;
					}else{
						return -1;
					}
				} else if (categories.contains(p1.getCategory())){
					return 1;
				} else if (categories.contains(p2.getCategory())){
					return -1;
				} else{
					return p1.getCategory().compareToIgnoreCase(p2.getCategory());
				}
			}
		}
	}
}
