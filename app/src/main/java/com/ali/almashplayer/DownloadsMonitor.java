package com.ali.almashplayer;

import android.app.DownloadManager;
import android.content.Context;
import android.database.Cursor;
import android.util.Log;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

/**
 * كلاس لمراقبة التحميلات من DownloadManager وتحديث قاعدة البيانات
 */
public class DownloadsMonitor {

    private static final String TAG = "DownloadsMonitor";

    private final Context appContext;
    private final DownloadManager downloadManager;
    private final DownloadDao downloadDao;
    private Timer timer;

    public DownloadsMonitor(Context context) {
        this.appContext = context.getApplicationContext();
        this.downloadManager =
                (DownloadManager) appContext.getSystemService(Context.DOWNLOAD_SERVICE);
        AppDatabase db = AppDatabase.getInstance(appContext);
        this.downloadDao = db.downloadDao();
    }

    public void start() {
        stop();
        timer = new Timer();
        timer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                try {
                    updateAll();
                } catch (Exception e) {
                    Log.e(TAG, "updateAll error", e);
                }
            }
        }, 0, 3000); // كل 3 ثوانٍ
    }

    public void stop() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void updateAll() {
        // تحميلات نشطة (جاري أو متوقفة مؤقتاً)
        List<DownloadEntity> list =
                downloadDao.getActiveDownloads(
                        DownloadItem.STATUS_DOWNLOADING,
                        DownloadItem.STATUS_PAUSED
                );

        for (DownloadEntity entity : list) {
            long id = entity.downloadId;
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(id);
            Cursor c = downloadManager.query(query);
            if (c != null && c.moveToFirst()) {
                int statusIndex = c.getColumnIndex(DownloadManager.COLUMN_STATUS);
                int reasonIndex = c.getColumnIndex(DownloadManager.COLUMN_REASON);
                int soFarIndex = c.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR);
                int totalIndex = c.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES);

                int dmStatus = c.getInt(statusIndex);
                int dmReason = c.getInt(reasonIndex);
                long soFar = c.getLong(soFarIndex);
                long total = c.getLong(totalIndex);

                int appStatus = entity.status;
                int progress = entity.progress;

                if (total > 0) {
                    progress = (int) ((soFar * 100L) / total);
                }

                if (dmStatus == DownloadManager.STATUS_SUCCESSFUL) {
                    appStatus = DownloadItem.STATUS_COMPLETED;
                    progress = 100;
                } else if (dmStatus == DownloadManager.STATUS_FAILED) {
                    appStatus = DownloadItem.STATUS_FAILED;
                } else {
                    appStatus = DownloadItem.STATUS_DOWNLOADING;
                }

                c.close();

                boolean changed = (appStatus != entity.status || progress != entity.progress);

                if (changed) {
                    entity.status = appStatus;
                    entity.progress = progress;
                    downloadDao.update(entity);

                    // حدّث نسخة الذاكرة أيضاً لتنعكس على UI
                    synchronized (DownloadsRepository.DOWNLOADS) {
                        for (DownloadItem item : DownloadsRepository.DOWNLOADS) {
                            if (item.getId() == entity.id) {
                                item.setStatus(entity.status);
                                item.setProgress(entity.progress);
                                item.setDownloadId(entity.downloadId);
                                item.setFilePath(entity.filePath);
                                break;
                            }
                        }
                    }
                }
            } else if (c != null) {
                c.close();
            }
        }
    }
}
