package com.PopCorp.Purchases.Adapters;

import android.content.Context;
import android.support.v7.util.SortedList;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.PopCorp.Purchases.Comparators.MenuComparator;
import com.PopCorp.Purchases.Controllers.MenuController;
import com.PopCorp.Purchases.Data.List;
import com.PopCorp.Purchases.R;

import java.util.ArrayList;

public class MenuAdapter extends RecyclerView.Adapter<MenuAdapter.ViewHolder>{

	private final ArrayList<List> lists;
	private final MenuController controller;
	private final Context context;
	private final SortedList<List> publishItems;
	
	public MenuAdapter(Context context, ArrayList<List> lists, MenuController controller){
		super();
		this.context = context;
		this.lists = lists;
		this.controller = controller;
		publishItems = new SortedList<>(List.class, new SortedList.Callback<List>() {
			@Override
			public boolean areContentsTheSame(List oneItem, List twoItem) {
				return oneItem.getName().equals(twoItem.getName()) && oneItem.getItems().equals(twoItem.getItems());
			}

			@Override
			public boolean areItemsTheSame(List oneItem, List twoItem) {
				return oneItem == twoItem;
			}

			@Override
			public int compare(List oneItem, List twoItem) {
				return new MenuComparator(MenuAdapter.this.context).compare(oneItem, twoItem);
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
		for (List list : lists){
			publishItems.add(list);
		}
	}
	
	public static class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{
		public final View view;
		public final TextView name;
		public final TextView items;
		public final TextView count;
		public final ImageView overflow;
		public final View divider;
		public List list;
		private ClickListener clickListener;
		
		public ViewHolder(View view) {
			super(view);
			this.view = view;
			name = (TextView) view.findViewById(R.id.content_item_list_textview_name);
			items = (TextView) view.findViewById(R.id.content_item_list_textview_items);
			count = (TextView) view.findViewById(R.id.content_item_list_textview_total_count);
			overflow = (ImageView) view.findViewById(R.id.content_item_list_image_overflow);
			divider = view.findViewById(R.id.content_item_list_divider);
			view.findViewById(R.id.content_item_list_clickable_view).setOnClickListener(this);
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
	public void onBindViewHolder(final ViewHolder holder, int position) {
		List list = publishItems.get(position);
		
		holder.name.setText(list.getName());
		holder.count.setText(String.valueOf(list.getItems().size()));
		if (list.getItems().size()==0){
			holder.items.setVisibility(View.GONE);
			holder.divider.setVisibility(View.GONE);
		} else{
			holder.items.setVisibility(View.VISIBLE);
			holder.divider.setVisibility(View.VISIBLE);
			holder.items.setText(list.getSpannableStringBuilder());
		}

		holder.overflow.setTag(list);
		holder.overflow.setOnClickListener(new OnClickListener(){
			@Override
			public void onClick(View v) {
				controller.showPopupMenu(v, (List) v.getTag());
			}
		});
		holder.setClickListener(new ViewHolder.ClickListener() {
	        @Override
	        public void onClick(View view, int position) {
				List list = publishItems.get(position);
	            controller.openListWithAnimation(list.getName(), list.getDatelist());
	        }
	    });
	}

	@Override
	public ViewHolder onCreateViewHolder(ViewGroup parent, int position) {
		View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);

		return new ViewHolder(v);
	}

	public void update(){
		for (int i=0; i<publishItems.size(); i++){
			if (!lists.contains(publishItems.get(i))){
				publishItems.removeItemAt(i);
			}
		}
		for (List list : lists){
			if (publishItems.indexOf(list)==-1){
				publishItems.add(list);
			}
		}
	}

	public SortedList<List> getPublishItems(){
		return publishItems;
	}
}
