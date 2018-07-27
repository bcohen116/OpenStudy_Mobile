package com.example.ben.open_study;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import java.util.ArrayList;

public class RecyclerViewAdapter extends RecyclerView.Adapter<RecyclerViewAdapter.ViewHolder>{
    private ArrayList<Room> mData;
    private LayoutInflater mInflater;
    private ItemClickListener mClickListener;
    private Context mContext;

    // data is passed into the constructor
    RecyclerViewAdapter(Context context, ArrayList<Room> data) {
        this.mInflater = LayoutInflater.from(context);
        this.mData = data;
        this.mContext = context;
    }

    // inflates the cell layout from xml when needed
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = mInflater.inflate(R.layout.list_item_template, parent, false);
        return new ViewHolder(view);
    }

    // binds the data to the textview in each cell
    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.myTextView.setText(mData.get(position).getName());
        //Set the room list images
        Drawable roomAvailableImg;
        if (mData.get(position).getAvailability()){
            //room is available, change image to display that info
            roomAvailableImg = ContextCompat.getDrawable(mContext,R.drawable.baseline_meeting_room_green_48dp);
        }
        else{
            //room is not available
            roomAvailableImg = ContextCompat.getDrawable(mContext,R.drawable.baseline_no_meeting_room_red_48dp);
        }
        Bitmap bitmapImg = ((BitmapDrawable) roomAvailableImg).getBitmap();
        Drawable resizedImg = new BitmapDrawable(mContext.getResources(), Bitmap.createScaledBitmap(bitmapImg, 150, 150, true));
        holder.myTextView.setCompoundDrawablesWithIntrinsicBounds(resizedImg,null,null,null);
    }

    // total number of cells
    @Override
    public int getItemCount() {
        return mData.size();
    }


    // stores and recycles views as they are scrolled off screen
    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
        TextView myTextView;

        ViewHolder(View itemView) {
            super(itemView);
            myTextView = (TextView) itemView.findViewById(R.id.roomNumberListing);
            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
            if (mClickListener != null){
                mClickListener.onItemClick(view, getAdapterPosition());
                Log.i("Info: ","Item Clicked .......");
            }
            else
                Log.i("Info: ","Item Clicked, no listener .......");

        }
    }

    // convenience method for getting data at click position
    Room getItem(int id) {
        return mData.get(id);
    }

    // allows clicks events to be caught
    void setClickListener(ItemClickListener itemClickListener) {
        this.mClickListener = itemClickListener;
    }

    // parent activity will implement this method to respond to click events
    public interface ItemClickListener {
        void onItemClick(View view, int position);
    }

}
