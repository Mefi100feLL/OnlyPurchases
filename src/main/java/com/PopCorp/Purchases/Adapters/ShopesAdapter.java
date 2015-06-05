package com.PopCorp.Purchases.Adapters;

import java.util.ArrayList;
import java.util.Arrays;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import com.PopCorp.Purchases.R;
import com.PopCorp.Purchases.Controllers.ShopesController;
import com.PopCorp.Purchases.Data.Shop;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

public class ShopesAdapter extends RecyclerView.Adapter<ShopesAdapter.ViewHolder> implements Filterable {

	public static final String FILTER_TYPE_FAVORITE = "FILTER_TYPE_FAVORITE";
	public static final String FILTER_TYPE_ALL = "FILTER_TYPE_ALL";

	private Context context;
	private ArrayList<Shop> items;
	private SortedList<Shop> publishItems;
	private DisplayImageOptions options;
	private ImageLoader imageLoader;
	private ShopesController controller;
	private int textEmpty = R.string.string_no_favorite_shopes;
	private int drawableEmptyRes = R.drawable.ic_no_favorite_shopes;

	public ShopesAdapter(Context context, ArrayList<Shop> array, ShopesController controller) {
		this.context = context;
		this.items = array;
		this.controller = controller;
		
		imageLoader = ImageLoader.getInstance();
		
		options = new DisplayImageOptions.Builder()
				.showImageOnLoading(R.drawable.back_right)
				.showImageForEmptyUri(R.drawable.ic_error_image)
				.showImageOnFail(R.drawable.ic_no_internet)
		.cacheInMemory(true)
		.cacheOnDisk(true)
		.considerExifParams(true)
		.bitmapConfig(Bitmap.Config.RGB_565)
		.build();

		publishItems = new SortedList<>(Shop.class, new SortedList.Callback<Shop>() {
			@Override
			public boolean areContentsTheSame(Shop oneItem, Shop twoItem) {
				return Arrays.equals(oneItem.getFields(), twoItem.getFields());
			}

			@Override
			public boolean areItemsTheSame(Shop oneItem, Shop twoItem) {
				return oneItem.equals(twoItem);
			}

			@Override
			public int compare(Shop oneItem, Shop twoItem) {
				return oneItem.getName().compareToIgnoreCase(twoItem.getName());
			}

			@Override
			public void onChanged(int position, int count) {
				notifyItemRangeChanged(position, count);
			}

			@Override
			public void onInserted(int position, int count) {
				notifyItemRangeInserted(position, count);
			}

			@Override
			public void onMoved(int fromPosition, int toPosition) {
				notifyItemMoved(fromPosition, toPosition);
			}

			@Override
			public void onRemoved(int position, int count) {
				notifyItemRangeRemoved(position, count);
			}
		});
	}

	public SortedList<Shop> getPublishItems() {
		return publishItems;
	}

	public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
		public View view;
		public ImageView image;
		public ImageView favorite;
		public TextView name;
		public TextView count;
		private ClickListener clickListener;
		
		public ViewHolder(View view) {
			super(view);
			this.view = view;
			image = (ImageView) view.findViewById(R.id.content_shop_image);
			favorite = (ImageView) view.findViewById(R.id.content_shop_image_favorite);
			name = (TextView) view.findViewById(R.id.content_shop_textview_name);
			count = (TextView) view.findViewById(R.id.content_shop_count_sales);
			if (view.getId()==R.id.content_shop_main_layout){
				view.setOnClickListener(this);
			} else{
				view.findViewById(R.id.content_shop_main_layout).setOnClickListener(this);
			}
		}
		
		public interface ClickListener {
	        void onClick(View v, int position);
	    }
		
		public void setClickListener(ClickListener clickListener) {
	        this.clickListener = clickListener;
	    }
		
		@Override
		public void onClick(View v) {
			clickListener.onClick(v, getAdapterPosition());
		}
	}

	@Override
	public int getItemCount() {
		return publishItems.size();
	}

	@Override
	public void onBindViewHolder(ViewHolder holder, int position) {
		Shop shop = publishItems.get(position);

		holder.name.setText(shop.getName());
		holder.count.setText(context.getString(R.string.string_count_of_sales) + " " + shop.getCountSales());

		imageLoader.displayImage(shop.getImageUrl(), holder.image, options);
		holder.setClickListener(new ViewHolder.ClickListener() {
			@Override
			public void onClick(View view, int position) {
				controller.openShop(position);
			}
		});

		holder.favorite.setTag(shop);
		if (shop.isFavorite()){
			holder.favorite.setImageResource(R.drawable.ic_star_yellow_24dp);
		} else{
			holder.favorite.setImageResource(R.drawable.ic_star_outline_yellow_24dp);
		}
		holder.favorite.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				Shop shop = (Shop) v.getTag();
				if (shop.isFavorite()){
					controller.shopFromFavorites(shop);
					((ImageView) v).setImageResource(R.drawable.ic_star_outline_yellow_24dp);
					publishItems.updateItemAt(publishItems.indexOf(shop), shop);
				} else{
					controller.shopToFavorites(shop);
					((ImageView) v).setImageResource(R.drawable.ic_star_yellow_24dp);
					publishItems.updateItemAt(publishItems.indexOf(shop), shop);
				}
			}
		});
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_shop, parent, false);
		
		ViewHolder viewHolder = new ViewHolder(v);
		return viewHolder;
	}

	@Override
	public Filter getFilter() {
		Filter filter = new Filter() {
			@Override
			protected void publishResults(CharSequence constraint, FilterResults results) {
				ArrayList<Shop> newItems = (ArrayList<Shop>) results.values;
				ArrayList<Shop> itemsForRemove = new ArrayList<>();
				for (int i = 0; i < publishItems.size(); i++) {
					if (!newItems.contains(publishItems.get(i))) {
						itemsForRemove.add(publishItems.get(i));
					}
				}
				for (Shop item : itemsForRemove) {
					publishItems.remove(item);
				}
				for (Shop item : newItems) {
					if (publishItems.indexOf(item) == -1) {
						publishItems.add(item);
					}
				}
				if (publishItems.size()==0){
					controller.showEmpty(textEmpty, drawableEmptyRes);
				} else{
					controller.showListView();
				}
			}

			@Override
			protected FilterResults performFiltering(CharSequence constraint) {
				FilterResults results = new FilterResults();
				ArrayList<Shop> FilteredArrayNames = new ArrayList<>();

				if (constraint.equals(FILTER_TYPE_ALL)) {
					results.count = items.size();
					results.values = items;
					textEmpty = R.string.string_no_shopes;
					drawableEmptyRes = R.drawable.ic_no_shopes;
					return results;
				}

				for (int i = 0; i < items.size(); i++) {
					Shop item = items.get(i);
					if (item.isFavorite()) {
						FilteredArrayNames.add(item);
					}
				}

				results.count = FilteredArrayNames.size();
				results.values = FilteredArrayNames;
				textEmpty = R.string.string_no_favorite_shopes;
				drawableEmptyRes = R.drawable.ic_no_favorite_shopes;
				return results;
			}
		};

		return filter;
	}
}
