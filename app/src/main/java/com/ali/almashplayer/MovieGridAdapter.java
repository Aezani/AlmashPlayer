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

public class MovieGridAdapter extends RecyclerView.Adapter<MovieGridAdapter.ViewHolder> {

    public interface OnItemClickListener {
        void onItemClick(LibraryItem item);
    }

    private List<LibraryItem> items;
    private OnItemClickListener listener;

    public MovieGridAdapter(List<LibraryItem> items, OnItemClickListener listener) {
        this.items = items;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_movie_grid, parent, false);
        return new ViewHolder(v);
    }

    // --- داخل ملف MovieGridAdapter.java ---

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        LibraryItem item = items.get(position);

        // سجل لمراقبة البيانات في الـ Logcat
        android.util.Log.d("CheckData", "Title: " + item.getTitle() + " | Index: " + item.getIndex());

        if ("episode".equals(item.getType())) {
            // 1. جلب اسم المسلسل
            String seriesName = (item.getGrandparentTitle() != null && !item.getGrandparentTitle().isEmpty())
                    ? item.getGrandparentTitle()
                    : "";

            // 2. جلب رقم الحلقة فقط (إذا كان الـ index موجوداً نستخدمه، وإذا كان null نستخدم العنوان title)
            String episodeInfo;
            if (item.getIndex() != null && !item.getIndex().isEmpty()) {
                episodeInfo = "الحلقة " + item.getIndex();
            } else {
                // هنا نستخدم العنوان الذي ظهر في اللوج عندك (مثل "17 الحلقه")
                episodeInfo = item.getTitle();
            }

            // 3. دمج النص النهائي: "اسم المسلسل - الحلقة 168"
            String fullDisplayName = seriesName.isEmpty() ? episodeInfo : seriesName + " - " + episodeInfo;
            holder.txtTitle.setText(fullDisplayName);

            // 4. إظهار الرقم في الشارة (Badge)
            if (holder.txtEpisodeBadge != null) {
                holder.txtEpisodeBadge.setVisibility(View.VISIBLE);
                holder.txtEpisodeBadge.setText(episodeInfo);
            }

        } else {
            // الوضع الطبيعي للأفلام
            holder.txtTitle.setText(item.getTitle());
            if (holder.txtEpisodeBadge != null) {
                holder.txtEpisodeBadge.setVisibility(View.GONE);
            }
        }

        // --- منطق عرض السنة ---
        if (holder.txtMovieYear != null) {
            holder.txtMovieYear.setText(item.getYear());
        }

        // --- منطق الصورة (يبقى كما هو) ---
        String thumbPath = item.getThumb();
        if ("episode".equals(item.getType()) && item.getGrandparentThumb() != null) {
            thumbPath = item.getGrandparentThumb();
        }

        if (thumbPath != null && !thumbPath.isEmpty()) {
            String imageUrl = thumbPath.startsWith("http") ? thumbPath :
                    Config.getBaseUrl() + thumbPath + Config.getTokenQuery();

            com.bumptech.glide.Glide.with(holder.itemView.getContext())
                    .load(imageUrl)
                    .placeholder(android.R.color.darker_gray)
                    .error(android.R.color.darker_gray)
                    .centerCrop()
                    .into(holder.imgPoster);
        } else {
            holder.imgPoster.setImageResource(android.R.color.darker_gray);
        }

        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                // إذا كان نوع العنصر حلقة، نغير الـ ratingKey قبل إرساله لشاشة التفاصيل
                if ("episode".equals(item.getType()) && item.getGrandparentRatingKey() != null) {

                    // سجل للتأكد أننا نرسل الرقم الصحيح
                    android.util.Log.d("ClickCheck", "إرسال مفتاح المسلسل الأب: " + item.getGrandparentRatingKey());

                    // ننشئ كائن مؤقت يحمل هوية المسلسل لكي تفتح شاشة التفاصيل صح
                    LibraryItem showItem = new LibraryItem(
                            item.getGrandparentRatingKey(), // نستخدم مفتاح المسلسل هنا
                            item.getGrandparentTitle(),
                            item.getYear(),
                            item.getGrandparentThumb(),
                            "show" // نغير النوع إلى مسلسل
                    );
                    listener.onItemClick(showItem);
                } else {
                    // إذا كان فيلماً أو مسلسلاً عادياً، نرسله كما هو
                    listener.onItemClick(item);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgPoster;
        TextView txtTitle;
        TextView txtMovieYear;    // أضف هذا السطر
        TextView txtEpisodeBadge; // أضف هذا السطر أيضاً (لأنه مستخدم في الكود)

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgPoster = itemView.findViewById(R.id.imgMoviePoster);
            txtTitle = itemView.findViewById(R.id.txtMovieTitle);

            // ربط العناصر الجديدة بالـ ID الموجود في ملف item_movie.xml
            txtMovieYear = itemView.findViewById(R.id.txtMovieYear);
            txtEpisodeBadge = itemView.findViewById(R.id.txtEpisodeBadge);
        }
    }
}