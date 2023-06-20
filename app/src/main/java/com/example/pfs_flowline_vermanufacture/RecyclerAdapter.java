package com.example.pfs_flowline_vermanufacture;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class RecyclerAdapter extends RecyclerView.Adapter <RecyclerAdapter.logViewHolder>{

    LayoutInflater inflater;
    private  List<HashMap<String, String>> data;
    private OnItemListener monItemListener;


    RecyclerAdapter(Context context,  ArrayList<HashMap<String, String>> data, OnItemListener onItemListener){
        this.inflater = LayoutInflater.from(context);
        this.data = data;
        this.monItemListener = onItemListener;

    }

    @NonNull
    @Override
    public logViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = inflater.inflate(R.layout.list_item_row, parent, false);
        return new logViewHolder(view, monItemListener);
    }

    @Override
    public void onBindViewHolder(@NonNull logViewHolder holder, int position) {

        holder.setIsRecyclable(false);
        holder.stationNum.setText(data.get(position).get("station"));
        holder.username.setText(data.get(position).get("userName"));
        //holder.start.setText(data.get(position).get("start"));
        holder.end.setText(data.get(position).get("end"));
        holder.duration.setText(data.get(position).get("duration"));
        //holder.chargerCode.setText(data.get(position).get("chargerCode"));
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class logViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener
    {
        public TextView stationNum, username, duration, start, end,chargerCode, status;
        OnItemListener onItemListener;

        public logViewHolder(@NonNull View itemView, OnItemListener onItemListener) {
            super(itemView);
            stationNum = itemView.findViewById(R.id.tv_station_num);
            username = itemView.findViewById(R.id.tv_user_name);
            duration = itemView.findViewById(R.id.tv_best_duration);
            end = itemView.findViewById(R.id.tv_end);
            this.onItemListener = onItemListener;

            itemView.setOnClickListener(this);
        }
        @Override
        public void onClick(View v) {
            onItemListener.onItemClick(getAdapterPosition());
        }
    }
    public interface OnItemListener{
        void onItemClick(int position);
    }
}
