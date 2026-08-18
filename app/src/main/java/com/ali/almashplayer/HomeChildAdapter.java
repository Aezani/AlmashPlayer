package com.ali.almashplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class HomeChildAdapter extends RecyclerView.Adapter<HomeChildAdapter.ChildViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(HomeItem item);
    }

    private List<HomeItem> items;
    private OnItemClickListener listener;

    public HomeChildAdapter(List<HomeItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ChildViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_home_child, parent, false);
        return new ChildViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ChildViewHolder holder, int position) {
        HomeItem item = items.get(position);

        // العنوان (فيلم أو مسلسل)
        holder.txtTitle.setText(item.getTitle());

        // تحميل البوستر من Plex
        String thumb = item.getThumb();
        if (thumb != null && !thumb.isEmpty()) {
            String imageUrl = Config.getBaseUrl() + thumb + Config.getTokenQuery();
            Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .centerCrop()
                    .into(holder.imgPoster);
        } else {
            holder.imgPoster.setImageResource(android.R.color.darker_gray);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(item);
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ChildViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView txtTitle;

        public ChildViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgPoster);
            txtTitle = itemView.findViewById(R.id.txtTitle);
        }
    }
}
