package com.ali.almashplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * يعرض قائمة المواسم، وكل موسم يحتوي RecyclerView فرعي للحلقات
 */
public class SeasonsAdapter extends RecyclerView.Adapter<SeasonsAdapter.SeasonViewHolder> {

    public interface OnEpisodeClickListener {
        void onEpisodeClicked(EpisodeInfo episode);
    }

    private List<SeasonInfo> seasons;
    private Map<String, List<EpisodeInfo>> episodesMap = new HashMap<>();
    private OnEpisodeClickListener listener;
    private ShowDetailsActivity hostActivity;

    public SeasonsAdapter(List<SeasonInfo> seasons, ShowDetailsActivity hostActivity) {
        this.seasons = seasons;
        this.hostActivity = hostActivity;
        if (hostActivity instanceof OnEpisodeClickListener) {
            this.listener = (OnEpisodeClickListener) hostActivity;
        }
    }

    @NonNull
    @Override
    public SeasonViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_season, parent, false);
        return new SeasonViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull SeasonViewHolder holder, int position) {
        SeasonInfo season = seasons.get(position);
        holder.bind(season);
    }

    @Override
    public int getItemCount() {
        return seasons.size();
    }

    /**
     * تُستدعى من ShowDetailsActivity بعد جلب الحلقات لموسم معيّن
     */
    public void setEpisodesForSeason(String seasonRatingKey,
                                     List<EpisodeInfo> episodes,
                                     SeasonViewHolder holder) {
        episodesMap.put(seasonRatingKey, episodes);
        holder.setEpisodes(episodes);
    }

    class SeasonViewHolder extends RecyclerView.ViewHolder {

        TextView txtSeasonTitle;
        ImageButton btnToggle;
        RecyclerView recyclerEpisodes;
        LinearLayout layoutHeader;
        ImageView imgSeasonThumb;

        boolean expanded = false;
        EpisodesAdapter episodesAdapter;
        List<EpisodeInfo> episodes = new ArrayList<>();

        SeasonInfo seasonInfo;

        SeasonViewHolder(@NonNull View itemView) {
            super(itemView);
            txtSeasonTitle = itemView.findViewById(R.id.txtSeasonTitle);
            btnToggle = itemView.findViewById(R.id.btnToggleSeason);
            recyclerEpisodes = itemView.findViewById(R.id.recyclerEpisodes);
            layoutHeader = itemView.findViewById(R.id.layoutSeasonHeader);
            imgSeasonThumb = itemView.findViewById(R.id.imgSeasonThumb);

            recyclerEpisodes.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
            episodesAdapter = new EpisodesAdapter(episodes, listener);
            recyclerEpisodes.setAdapter(episodesAdapter);

            View.OnClickListener toggleClick = v -> toggleExpanded();
            layoutHeader.setOnClickListener(toggleClick);
            btnToggle.setOnClickListener(toggleClick);
        }

        void bind(SeasonInfo season) {
            this.seasonInfo = season;

            // عنوان الموسم بدون كلمة Season
            String title = season.getTitle();
            int index = season.getIndex();

            if (title != null && !title.isEmpty()) {
                // نحاول استخراج رقم الموسم من العنوان الإنجليزية مثل "Season 1"
                String lower = title.toLowerCase();
                if (lower.startsWith("season")) {
                    // نستخدم الرقم الموجود أو index من Plex
                    if (index > 0) {
                        title = "الموسم " + index;
                    } else {
                        // نحاول أخذ آخر رقم في العنوان
                        String num = title.replaceAll("\\D+", "");
                        if (!num.isEmpty()) {
                            title = "الموسم " + num;
                        } else {
                            title = "الموسم";
                        }
                    }
                }
            } else {
                // لو ما فيه عنوان أصلاً نركّب واحد من index
                if (index > 0) {
                    title = "الموسم " + index;
                } else {
                    title = "الموسم";
                }
            }

            txtSeasonTitle.setText(title);

            // صورة الموسم إن وجدت
            if (season.getThumb() != null && !season.getThumb().isEmpty()) {
                String url = Config.getBaseUrl()
                        + season.getThumb()
                        + Config.getTokenQuery();
                Glide.with(itemView.getContext())
                        .load(url)
                        .placeholder(android.R.color.darker_gray)
                        .centerCrop()
                        .into(imgSeasonThumb);
            } else {
                imgSeasonThumb.setImageResource(android.R.color.darker_gray);
            }

            // لو الحلقات محمّلة مسبقاً
            List<EpisodeInfo> cached = episodesMap.get(season.getRatingKey());
            if (cached != null) {
                setEpisodes(cached);
            } else {
                episodes.clear();
                episodesAdapter.notifyDataSetChanged();
            }

            expanded = false;
            recyclerEpisodes.setVisibility(View.GONE);
            btnToggle.setRotation(0f);
        }

        void toggleExpanded() {
            expanded = !expanded;
            recyclerEpisodes.setVisibility(expanded ? View.VISIBLE : View.GONE);
            btnToggle.setRotation(expanded ? 90f : 0f);

            if (expanded && episodes.isEmpty()) {
                hostActivity.loadEpisodesForSeason(seasonInfo, this);
            }
        }

        void setEpisodes(List<EpisodeInfo> eps) {
            episodes.clear();
            episodes.addAll(eps);
            episodesAdapter.notifyDataSetChanged();
        }
    }
}
