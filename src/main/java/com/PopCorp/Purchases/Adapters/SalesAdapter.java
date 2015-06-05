package com.PopCorp.Purchases.Adapters;

import android.graphics.Bitmap;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.PopCorp.Purchases.Controllers.SalesController;
import com.PopCorp.Purchases.Data.Sale;
import com.PopCorp.Purchases.R;
import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;

import java.util.ArrayList;

public class SalesAdapter extends RecyclerView.Adapter<SalesAdapter.ViewHolder> {

	private ArrayList<Sale> items;
	private SortedList<Sale> publishItems;
	private DisplayImageOptions options;
	private ImageLoader imageLoader;
	private SalesController controller;

	public SalesAdapter(SalesController controller, ArrayList<Sale> array) {
		this.controller = controller;
		this.items = array;
		
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

		publishItems = new SortedList<>(Sale.class, new SortedList.Callback<Sale>() {
			@Override
			public boolean areContentsTheSame(Sale oneItem, Sale twoItem) {
				return oneItem.equals(twoItem);
			}

			@Override
			public boolean areItemsTheSame(Sale oneItem, Sale twoItem) {
				return oneItem.equals(twoItem);
			}

			@Override
			public int compare(Sale oneItem, Sale twoItem) {
				return oneItem.getTitle().compareToIgnoreCase(twoItem.getTitle());
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
		for (Sale sale : items){
			publishItems.add(sale);
		}
	}

	public void update(){
		for (int i=0; i<publishItems.size(); i++){
			if (!items.contains(publishItems.get(i))){
				publishItems.removeItemAt(i);
			}
		}
		for (Sale sale : items){
			if (publishItems.indexOf(sale)==-1) {
				publishItems.add(sale);
			}
		}
	}

	public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
		public View view;
		public ImageView image;
		private ClickListener clickListener;

		public ViewHolder(View view) {
			super(view);
			this.view = view;
			image = (ImageView) view.findViewById(R.id.item_sale_image);
			image.setOnClickListener(this);
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
		Sale sale = publishItems.get(position);

		imageLoader.displayImage(sale.getImageUrl(), holder.image, options);

		holder.setClickListener(new ViewHolder.ClickListener(){
			@Override
			public void onClick(View v, int position) {
				controller.zoomSale(v, publishItems.get(position));
			}
		});
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_sale, parent, false);

		return new ViewHolder(v);
	}
}