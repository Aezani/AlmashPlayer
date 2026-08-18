package com.ali.almashplayer;

import android.app.Activity;
import android.content.Context;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class DownloadsRepository {

    public static final List<DownloadItem> DOWNLOADS = new ArrayList<>();

    public static void loadFromDbAsync(Context context, Runnable onDone) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
            List<DownloadEntity> entities = db.downloadDao().getAll();

            synchronized (DOWNLOADS) {
                DOWNLOADS.clear();
                for (DownloadEntity e : entities) {
                    DownloadItem item = new DownloadItem(
                            e.id,
                            e.title,
                            e.url,
                            e.thumbUrl
                    );
                    item.setStatus(e.status);
                    item.setProgress(e.progress);
                    item.setDownloadId(e.downloadId);
                    item.setFilePath(e.filePath);

                    item.setTotalBytes(e.totalBytes);
                    item.setDownloadedBytes(e.downloadedBytes);

                    DOWNLOADS.add(item);
                }
            }

            if (onDone != null) {
                onDone.run();
            }
        }).start();
    }

    public static void addDownloadAsync(Context context, DownloadItem item, Runnable onDone) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());

            DownloadEntity e = new DownloadEntity();
            e.title = item.getTitle();
            e.url = item.getUrl();
            e.thumbUrl = item.getThumbUrl();
            e.status = item.getStatus();
            e.progress = item.getProgress();
            e.downloadId = item.getDownloadId();
            e.filePath = item.getFilePath();
            e.totalBytes = item.getTotalBytes();
            e.downloadedBytes = item.getDownloadedBytes();

            long newId = db.downloadDao().insert(e);
            item.setId(newId);

            synchronized (DOWNLOADS) {
                DOWNLOADS.add(0, item);
            }

            if (onDone != null) {
                onDone.run();
            }
        }).start();
    }

    public static void updateDownloadAsync(Context context, DownloadItem item, Runnable onDone) {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());

            DownloadEntity e = new DownloadEntity();
            e.id = item.getId();
            e.title = item.getTitle();
            e.url = item.getUrl();
            e.thumbUrl = item.getThumbUrl();
            e.status = item.getStatus();
            e.progress = item.getProgress();
            e.downloadId = item.getDownloadId();
            e.filePath = item.getFilePath();
            e.totalBytes = item.getTotalBytes();
            e.downloadedBytes = item.getDownloadedBytes();

            db.downloadDao().update(e);

            if (onDone != null) {
                onDone.run();
            }
        }).start();
    }

    public static void deleteDownloadAsync(Context context, DownloadItem item, Runnable onDone) {
        new Thread(() -> {

            try {
                String path = item.getFilePath();
                if (path != null && !path.isEmpty()) {
                    File f = new File(path);
                    if (f.exists()) {
                        f.delete();
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());

            DownloadEntity e = new DownloadEntity();
            e.id = item.getId();
            e.title = item.getTitle();
            e.url = item.getUrl();
            e.thumbUrl = item.getThumbUrl();
            e.status = item.getStatus();
            e.progress = item.getProgress();
            e.downloadId = item.getDownloadId();
            e.filePath = item.getFilePath();
            e.totalBytes = item.getTotalBytes();
            e.downloadedBytes = item.getDownloadedBytes();

            db.downloadDao().delete(e);

            synchronized (DOWNLOADS) {
                DOWNLOADS.remove(item);
            }

            if (onDone != null) {
                onDone.run();
            }
        }).start();
    }

    public static List<DownloadEntity> getActiveDownloadsSync(Context context) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        return db.downloadDao().getActiveDownloads(
                DownloadItem.STATUS_DOWNLOADING,
                DownloadItem.STATUS_PAUSED
        );
    }

    public static void updateEntitySync(Context context, DownloadEntity e) {
        AppDatabase db = AppDatabase.getInstance(context.getApplicationContext());
        db.downloadDao().update(e);

        synchronized (DOWNLOADS) {
            for (DownloadItem item : DOWNLOADS) {
                if (item.getId() == e.id) {
                    item.setStatus(e.status);
                    item.setProgress(e.progress);
                    item.setDownloadId(e.downloadId);
                    item.setFilePath(e.filePath);
                    item.setTotalBytes(e.totalBytes);
                    item.setDownloadedBytes(e.downloadedBytes);
                    break;
                }
            }
        }
    }

    /**
     * بدء التحميل الداخلي عبر PlexRangeDownloader (يدعم الإيقاف والاستئناف والتشفير بعد الاكتمال)
     */
    public static void startInternalDownloadAsync(Activity activity, DownloadItem item, Runnable onDone) {
        new Thread(() -> {
            Context appCtx = activity.getApplicationContext();
            AppDatabase db = AppDatabase.getInstance(appCtx);

            // تجهيز الحالة الأولية
            if (item.getId() == 0) {
                item.setStatus(DownloadItem.STATUS_DOWNLOADING);
                item.setProgress(0);

                DownloadEntity e = new DownloadEntity();
                e.title = item.getTitle();
                e.url = item.getUrl();
                e.thumbUrl = item.getThumbUrl();
                e.status = item.getStatus();
                e.progress = item.getProgress();
                e.downloadId = 0;
                e.filePath = item.getFilePath();
                e.totalBytes = item.getTotalBytes();
                e.downloadedBytes = item.getDownloadedBytes();

                long newId = db.downloadDao().insert(e);
                item.setId(newId);

                synchronized (DOWNLOADS) {
                    DOWNLOADS.add(0, item);
                }
            }

            // مسار المجلد الداخلي:
            // /data/user/0/com.ali.almashplayer/files/AlmashDownloads/mash
            File appRoot = new File(appCtx.getFilesDir(), "AlmashDownloads");
            if (!appRoot.exists()) {
                //noinspection ResultOfMethodCallIgnored
                appRoot.mkdirs();
            }
            File mashDir = new File(appRoot, "mash");
            if (!mashDir.exists()) {
                //noinspection ResultOfMethodCallIgnored
                mashDir.mkdirs();
            }

            // اسم الملف النهائي المشفَّر
            String safeName = sanitizeFileName(item.getTitle()) + ".mp4";
            File destFile = new File(mashDir, safeName);

            // حفظ المسار في الـ item و DB
            item.setFilePath(destFile.getAbsolutePath());
            updateDownloadAsync(appCtx, item, null);

            if (onDone != null) {
                onDone.run();
            }

            // بدء التحميل/الاستئناف عبر PlexRangeDownloader
            PlexRangeDownloader.startOrResumeDownload(appCtx, item, destFile);
        }).start();
    }

    private static String sanitizeFileName(String name) {
        if (name == null) return "video";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
