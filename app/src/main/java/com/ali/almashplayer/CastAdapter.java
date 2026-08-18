package com.ali.almashplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class CastAdapter extends RecyclerView.Adapter<CastAdapter.ViewHolder> {

    private final List<CastRole> items = new ArrayList<>();

    public void setItems(List<CastRole> newItems) {
        items.clear();
        if (newItems != null) {
            items.addAll(newItems);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_cast, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        CastRole c = items.get(position);

        holder.txtName.setText(c.getName());
        holder.txtRole.setText(c.getRole() != null ? c.getRole() : "");

        String thumb = c.getThumb();

        if (thumb != null && !thumb.isEmpty()) {
            String url;

            if (thumb.startsWith("http")) {
                // صورة خارجية (TMDB أو غيره) لا تحتاج BaseUrl أو توكن Plex
                url = thumb;
            } else {
                // صورة من Plex تحتاج BaseUrl + التوكن
                url = Config.getBaseUrl() + thumb + Config.getTokenQuery();
            }

            Glide.with(holder.itemView.getContext())
                    .load(url)
                    .centerCrop()
                    .into(holder.imgPhoto);
        } else {
            holder.imgPhoto.setImageResource(android.R.color.darker_gray);
        }
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        ImageView imgPhoto;
        TextView txtName;
        TextView txtRole;

        ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPhoto = itemView.findViewById(R.id.imgCastPhoto);
            txtName  = itemView.findViewById(R.id.txtCastName);
            txtRole  = itemView.findViewById(R.id.txtCastRole);
        }
    }
}
