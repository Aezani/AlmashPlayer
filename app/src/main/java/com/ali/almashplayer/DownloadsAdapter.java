package com.ali.almashplayer;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.PopupMenu;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.io.File;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

public class DownloadsAdapter extends RecyclerView.Adapter<DownloadsAdapter.DownloadViewHolder> {

    public interface OnDownloadActionListener {
        void onResumeClicked(DownloadItem item);
        void onPauseClicked(DownloadItem item);
        void onDeleteClicked(DownloadItem item);
    }

    private List<DownloadItem> downloads;
    private OnDownloadActionListener listener;

    public DownloadsAdapter(List<DownloadItem> downloads, OnDownloadActionListener listener) {
        this.downloads = downloads;
        this.listener = listener;
    }

    @NonNull
    @Override
    public DownloadViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_download, parent, false);
        return new DownloadViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull DownloadViewHolder holder, int position) {
        DownloadItem item = downloads.get(position);
        holder.bind(item);
    }

    @Override
    public int getItemCount() {
        return downloads.size();
    }

    // تُستخدم لتحديث عنصر واحد أثناء التحميل
    public void notifyItemChangedById(long id) {
        for (int i = 0; i < downloads.size(); i++) {
            if (downloads.get(i).getId() == id) {
                notifyItemChanged(i);
                break;
            }
        }
    }

    class DownloadViewHolder extends RecyclerView.ViewHolder {

        ImageView imgThumb, btnMore;
        TextView txtTitle, txtStatus;

        DownloadViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
            txtTitle = itemView.findViewById(R.id.txtDownloadTitle);
            txtStatus = itemView.findViewById(R.id.txtDownloadStatus);
            btnMore = itemView.findViewById(R.id.btnMore);
        }

        void bind(DownloadItem item) {
            txtTitle.setText(item.getTitle());

            int progress = item.getProgress();
            long downloaded = item.getDownloadedBytes();
            long total = item.getTotalBytes();

            if (downloaded < 0) downloaded = 0;
            if (total < 0) total = 0;

            // نص الحالة
            String statusLabel;
            switch (item.getStatus()) {
                case DownloadItem.STATUS_DOWNLOADING:
                    statusLabel = "جاري التحميل";
                    break;
                case DownloadItem.STATUS_PAUSED:
                    statusLabel = "متوقف مؤقتاً";
                    break;
                case DownloadItem.STATUS_COMPLETED:
                    statusLabel = "اكتمل التحميل";
                    break;
                case DownloadItem.STATUS_FAILED:
                default:
                    statusLabel = "فشل التحميل";
                    break;
            }

            String statusText;

            if (item.getStatus() == DownloadItem.STATUS_COMPLETED) {
                // عند الاكتمال لا نظهر (المحمَّل - الكلي)، فقط الحالة أو النسبة 100%
                statusText = statusLabel;
            } else {
                // جزء الحجم (المحمَّل / الكلي)
                String sizePart;
                if (total > 0) {
                    // القيم الحقيقية المحفوظة في الـ DB تظهر هنا
                    sizePart = formatSize(downloaded) + " - " + formatSize(total);
                } else {
                    // لا نعرف الحجم الكلي بعد
                    sizePart = formatSize(downloaded) + " - —";
                }

                // الشكل النهائي:
                // (المحمَّل - الكلي)
                // 10% جاري التحميل
                statusText = "(" + sizePart + ")\n" + progress + "% " + statusLabel;
            }

            txtStatus.setText(statusText);

            String thumbUrl = item.getThumbUrl();
            if (thumbUrl != null && !thumbUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(thumbUrl)
                        .placeholder(android.R.color.darker_gray)
                        .centerCrop()
                        .into(imgThumb);
            } else {
                imgThumb.setImageResource(android.R.color.darker_gray);
            }

            // تشغيل Offline عند الضغط على العنصر
            itemView.setOnClickListener(v -> {
                if (item.getStatus() != DownloadItem.STATUS_COMPLETED) {
                    return;
                }

                String path = item.getFilePath();
                if (path == null || path.isEmpty()) {
                    return;
                }

                File f = new File(path);
                if (!f.exists()) {
                    return;
                }

                ArrayList<String> urls = new ArrayList<>();
                ArrayList<String> titles = new ArrayList<>();
                ArrayList<String> localPaths = new ArrayList<>();

                urls.add(item.getUrl() != null ? item.getUrl() : "");
                titles.add(item.getTitle() != null ? item.getTitle() : "");
                localPaths.add(path);

                Intent intent = new Intent(itemView.getContext(), PlayerActivity.class);
                intent.putStringArrayListExtra("episode_urls", urls);
                intent.putStringArrayListExtra("episode_titles", titles);
                intent.putStringArrayListExtra("episode_local_paths", localPaths);
                intent.putExtra("current_index", 0);

                itemView.getContext().startActivity(intent);
            });

            btnMore.setOnClickListener(v -> {
                PopupMenu popup = new PopupMenu(v.getContext(), btnMore);
                popup.getMenuInflater().inflate(R.menu.menu_download_item, popup.getMenu());

                // الحصول على عناصر القائمة
                android.view.Menu menu = popup.getMenu();
                android.view.MenuItem resumeItem = menu.findItem(R.id.action_resume);
                android.view.MenuItem pauseItem  = menu.findItem(R.id.action_pause);
                android.view.MenuItem deleteItem = menu.findItem(R.id.action_delete);

                // ضبط تفعيل/تعطيل العناصر حسب حالة التحميل
                switch (item.getStatus()) {
                    case DownloadItem.STATUS_DOWNLOADING:
                        // جاري التحميل: استئناف غير فعّالة، إيقاف + حذف فعّالة
                        if (resumeItem != null) resumeItem.setEnabled(false);
                        if (pauseItem  != null) pauseItem.setEnabled(true);
                        if (deleteItem != null) deleteItem.setEnabled(true);
                        break;

                    case DownloadItem.STATUS_PAUSED:
                        // متوقف مؤقتاً: إيقاف غير فعّالة، استئناف + حذف فعّالة
                        if (resumeItem != null) resumeItem.setEnabled(true);
                        if (pauseItem  != null) pauseItem.setEnabled(false);
                        if (deleteItem != null) deleteItem.setEnabled(true);
                        break;

                    case DownloadItem.STATUS_COMPLETED:
                        // اكتمل التحميل: فقط حذف فعّالة، استئناف + إيقاف غير فعّالة
                        if (resumeItem != null) resumeItem.setEnabled(false);
                        if (pauseItem  != null) pauseItem.setEnabled(false);
                        if (deleteItem != null) deleteItem.setEnabled(true);
                        break;

                    default:
                        // مضاف / فشل: استئناف + حذف فعّالة، إيقاف غير فعّالة
                        if (resumeItem != null) resumeItem.setEnabled(true);
                        if (pauseItem  != null) pauseItem.setEnabled(false);
                        if (deleteItem != null) deleteItem.setEnabled(true);
                        break;
                }

                popup.setOnMenuItemClickListener(menuItem -> {
                    if (listener == null || !menuItem.isEnabled()) return false;

                    int id = menuItem.getItemId();
                    if (id == R.id.action_resume) {
                        listener.onResumeClicked(item);
                        return true;
                    } else if (id == R.id.action_pause) {
                        listener.onPauseClicked(item);
                        return true;
                    } else if (id == R.id.action_delete) {
                        listener.onDeleteClicked(item);
                        return true;
                    }
                    return false;
                });

                popup.show();
            });
        }
    }

    // تحويل البايت إلى KB/MB/GB
    private String formatSize(long bytes) {
        if (bytes <= 0) return "0 B";
        final String[] units = new String[]{"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        double value = bytes / Math.pow(1024, digitGroups);
        DecimalFormat df = new DecimalFormat("#,##0.0");
        return df.format(value) + " " + units[digitGroups];
    }
}
