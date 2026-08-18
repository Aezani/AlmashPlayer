package com.ali.almashplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LibraryAdapter extends RecyclerView.Adapter<LibraryAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(LibrarySection section);
    }

    private List<LibrarySection> sections;
    private OnItemClickListener listener;

    public LibraryAdapter(List<LibrarySection> sections, OnItemClickListener listener) {
        this.sections = sections;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_library, parent, false);
        return new ViewHolder(v);
    }

    @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LibrarySection section = sections.get(position);
        holder.bind(section, listener);
    }

    @Override
    public int getItemCount() {
        return sections.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        TextView txtTitle;
        ImageView imgPoster;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            txtTitle = itemView.findViewById(R.id.txtLibraryTitle);
            imgPoster = itemView.findViewById(R.id.imgLibraryPoster);
        }

        public void bind(final LibrarySection section, final OnItemClickListener listener) {
            txtTitle.setText(section.getTitle());

            // مؤقتاً نغيّر لون الخلفية حسب نوع المكتبة
            if ("movie".equals(section.getType())) {
                imgPoster.setBackgroundColor(0xFF4444AA); // أفلام = أزرق غامق
            } else if ("show".equals(section.getType())) {
                imgPoster.setBackgroundColor(0xFF44AA44); // مسلسلات = أخضر
            } else {
                imgPoster.setBackgroundColor(0xFF666666); // أنواع أخرى
            }

            itemView.setOnClickListener(v -> listener.onItemClick(section));
        }
    }
}
