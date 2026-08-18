package com.ali.almashplayer;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class LiveChannelAdapter extends RecyclerView.Adapter<LiveChannelAdapter.LiveViewHolder> {

    public interface OnChannelActionListener {
        void onPlayInternal(LiveChannel channel);
        void onPlayWithMx(LiveChannel channel);
        void onPlayWithVlc(LiveChannel channel);
    }

    private List<LiveChannel> channels;
    private OnChannelActionListener listener;

    // لتحديد أي عنصر هو المختار حاليًا
    private int selectedPosition = RecyclerView.NO_POSITION;

    public LiveChannelAdapter(List<LiveChannel> channels, OnChannelActionListener listener) {
        this.channels = channels;
        this.listener = listener;
    }

    @NonNull
    @Override
    public LiveViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_live_channel, parent, false);
        return new LiveViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull LiveViewHolder holder, int position) {
        LiveChannel ch = channels.get(position);
        holder.tvName.setText(ch.getName());
        holder.tvInfo.setText(ch.getInfo() != null ? ch.getInfo() : "");

        // إظهار/إخفاء أزرار المشغل حسب العنصر المختار
        if (position == selectedPosition) {
            holder.layoutOptions.setVisibility(View.VISIBLE);
        } else {
            holder.layoutOptions.setVisibility(View.GONE);
        }

        // عند الضغط على عنصر القناة نفسه → تغيّر القناة المختارة
        holder.itemView.setOnClickListener(v -> {
            int oldPos = selectedPosition;
            selectedPosition = holder.getAdapterPosition();
            notifyItemChanged(oldPos);
            notifyItemChanged(selectedPosition);
        });

        // أزرار التشغيل
        holder.btnInternal.setOnClickListener(v -> {
            if (listener != null) listener.onPlayInternal(ch);
        });

        holder.btnMx.setOnClickListener(v -> {
            if (listener != null) listener.onPlayWithMx(ch);
        });

        holder.btnVlc.setOnClickListener(v -> {
            if (listener != null) listener.onPlayWithVlc(ch);
        });
    }

    @Override
    public int getItemCount() {
        return channels != null ? channels.size() : 0;
    }

    static class LiveViewHolder extends RecyclerView.ViewHolder {
        TextView tvName, tvInfo;
        LinearLayout layoutOptions;
        Button btnInternal, btnMx, btnVlc;

        LiveViewHolder(@NonNull View itemView) {
            super(itemView);
            tvName        = itemView.findViewById(R.id.tv_channel_name);
            tvInfo        = itemView.findViewById(R.id.tv_channel_info);
            layoutOptions = itemView.findViewById(R.id.layout_player_options);
            btnInternal   = itemView.findViewById(R.id.btn_play_internal);
            btnMx         = itemView.findViewById(R.id.btn_play_mx);
            btnVlc        = itemView.findViewById(R.id.btn_play_vlc);
        }
    }
}
