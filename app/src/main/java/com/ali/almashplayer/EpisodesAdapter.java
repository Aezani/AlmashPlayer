package com.ali.almashplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

/**
 * يعرض قائمة حلقات موسم واحد
 */
public class EpisodesAdapter extends RecyclerView.Adapter<EpisodesAdapter.EpisodeViewHolder> {

    private List<EpisodeInfo> episodes;
    private SeasonsAdapter.OnEpisodeClickListener listener;

    public EpisodesAdapter(List<EpisodeInfo> episodes,
                           SeasonsAdapter.OnEpisodeClickListener listener) {
        this.episodes = episodes;
        this.listener = listener;
    }

    @NonNull
    @Override
    public EpisodeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_episode, parent, false);
        return new EpisodeViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull EpisodeViewHolder holder, int position) {
        EpisodeInfo ep = episodes.get(position);
        holder.bind(ep);
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    class EpisodeViewHolder extends RecyclerView.ViewHolder {

        TextView txtEpisodeTitle;
        TextView txtEpisodeSub;

        EpisodeViewHolder(@NonNull View itemView) {
            super(itemView);
            txtEpisodeTitle = itemView.findViewById(R.id.txtEpisodeTitle);
            txtEpisodeSub = itemView.findViewById(R.id.txtEpisodeSub);

            itemView.setOnClickListener(v -> {
                int pos = getBindingAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onEpisodeClicked(episodes.get(pos));
                }
            });
        }

        void bind(EpisodeInfo ep) {
            String title = ep.getTitle();
            if (title == null || title.isEmpty()) {
                title = "الحلقة " + ep.getIndex();
            }
            txtEpisodeTitle.setText(title);

            String sub = "الحلقة " + ep.getIndex();
            if (ep.getDuration() != null && !ep.getDuration().isEmpty()) {
                sub += " • " + ep.getDuration();
            }
            txtEpisodeSub.setText(sub);

        }
    }
}
