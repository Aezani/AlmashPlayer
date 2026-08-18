package com.ali.almashplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class HomeParentAdapter extends RecyclerView.Adapter<HomeParentAdapter.ParentViewHolder> {

    private List<HomeRow> rows;
    private HomeChildAdapter.OnItemClickListener childClickListener;

    public HomeParentAdapter(List<HomeRow> rows,
                             HomeChildAdapter.OnItemClickListener childClickListener) {
        this.rows = rows;
        this.childClickListener = childClickListener;
    }

    @NonNull
    @Override
    public ParentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_row, parent, false);
        return new ParentViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ParentViewHolder holder, int position) {
        HomeRow row = rows.get(position);
        holder.txtRowTitle.setText(row.getTitle());

        HomeChildAdapter childAdapter = new HomeChildAdapter(row.getItems(), childClickListener);
        holder.recyclerRowItems.setLayoutManager(
                new LinearLayoutManager(holder.itemView.getContext(),
                        LinearLayoutManager.HORIZONTAL, false));
        holder.recyclerRowItems.setAdapter(childAdapter);
    }

    @Override
    public int getItemCount() {
        return rows.size();
    }

    static class ParentViewHolder extends RecyclerView.ViewHolder {
        TextView txtRowTitle;
        RecyclerView recyclerRowItems;

        public ParentViewHolder(@NonNull View itemView) {
            super(itemView);
            txtRowTitle = itemView.findViewById(R.id.txtRowTitle);
            recyclerRowItems = itemView.findViewById(R.id.recyclerRowItems);
        }
    }
}
