package com.ali.almashplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import androidx.core.app.NotificationCompat;

import java.io.File;
import java.io.InputStream;
import java.io.RandomAccessFile;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Downloader يدعم الاستئناف عبر HTTP Range + دعم الإيقاف وتحديث الواجهة لحظياً
 * مع إشعار في شريط الحالة (Notification) يوضح تقدّم التحميل.
 */
public class PlexRangeDownloader {

    private static final String TAG = "PlexRangeDownloader";

    // عدد المحاولات القصوى لكل تحميل
    private static final int MAX_RETRY = 3;

    // حفظ الـ Call الحالي لإمكانية إلغائه فوراً
    private static volatile Call currentCall;

    // إعدادات الإشعار
    private static final int NOTIF_ID = 1002;
    private static final String CHANNEL_ID = "almash_download_channel";

    public static void startOrResumeDownload(Context context, DownloadItem item, File destFile) {
        // عند الاستئناف نتأكد أن الفلاغ ملغي
        item.setCancelled(false);
        createChannelIfNeeded(context);

        // تأكد من بدء الخدمة في foreground لحماية التنزيل من قتل النظام
        try {
            DownloadForegroundService.start(context, item);
        } catch (Exception ex) {
            Log.w(TAG, "Failed to start foreground service", ex);
        }

        new Thread(() -> runDownloadLoop(context, item, destFile)).start();
    }

    // تستدعى من زر الإيقاف لإلغاء اتصال الشبكة
    public static void cancelCurrentCall() {
        Call c = currentCall;
        Log.d(TAG, "cancelCurrentCall: currentCall=" + c);
        if (c != null) {
            c.cancel();
        }
    }

    private static void runDownloadLoop(Context context, DownloadItem item, File destFile) {
        OkHttpClient client = AlmashApplication.getInstance().getHttpClient();
        String url = item.getUrl();

        int attempt = 0;
        boolean success = false;

        // قراءة سرعة التحميل (KB/s) من DownloadConfig (سنعيد تحديثها دورياً)
        int speedKbps = DownloadConfig.getDownloadSpeedKbps();
        long lastSpeedConfigCheck = System.currentTimeMillis();

        // أول إشعار عند بدء / استئناف التحميل
        showOrUpdateNotification(context, item);

        // استخدم ملف جزئي .part للاستئناف ولضمان عدم استخدام ملف نهائي تالف
        File partFile = new File(destFile.getAbsolutePath() + ".part");

        while (attempt < MAX_RETRY && !success) {
            // حجم الملف الموجود حالياً على القرص
            long downloadedBytes = partFile.exists() ? partFile.length() : 0;
            item.setDownloadedBytes(downloadedBytes);

            // لو تم إلغاء التحميل قبل بدء المحاولة الحالية نخرج
            if (item.isCancelled()) {
                Log.d(TAG, "download cancelled before attempt " + (attempt + 1));
                cancelNotification(context, item, false);
                return;
            }

            attempt++;
            Log.d(TAG, "download attempt " + attempt + " for url=" + url);

            Response response = null;
            InputStream in = null;
            RandomAccessFile raf = null;

            try {
                long totalBytes = item.getTotalBytes();

                // نبني الطلب مع أو بدون Range حسب ما تم تحميله
                Request.Builder builder = new Request.Builder().url(url);
                if (downloadedBytes > 0) {
                    String range = "bytes=" + downloadedBytes + "-";
                    builder.header("Range", range);
                    Log.d(TAG, "using Range header: " + range);
                }

                Request request = builder.build();
                Call call = client.newCall(request);
                currentCall = call;

                response = call.execute();

                if (!response.isSuccessful()) {
                    Log.e(TAG, "HTTP error: " + response.code());
                    continue; // جرّب محاولة أخرى (ما دام لم يُلغَ)
                }

                ResponseBody body = response.body();
                if (body == null) {
                    Log.e(TAG, "Empty response body");
                    continue;
                }

                // contentLength هو حجم الجزء القادم فقط (في حالة Range)
                long contentLength = body.contentLength();
                if (contentLength > 0) {
                    if (downloadedBytes > 0) {
                        totalBytes = downloadedBytes + contentLength;
                    } else {
                        totalBytes = contentLength;
                    }
                    item.setTotalBytes(totalBytes); // مهم جداً لإظهار الحجم الكلي
                }

                in = body.byteStream();

                // نفتح الملف الجزئي بشكل يسمح بالاستئناف
                raf = new RandomAccessFile(partFile, "rw");
                raf.seek(downloadedBytes);

                byte[] buffer = new byte[1024 * 256]; // 256KB
                int read;
                int lastProgress = item.getProgress();

                // متغيرات حساب السرعة (للـ log فقط)
                long lastSpeedLogTime = System.currentTimeMillis();
                long lastSpeedBytes = downloadedBytes;

                item.setStatus(DownloadItem.STATUS_DOWNLOADING);
                postItemUpdate(context, item); // أول تحديث

                while (true) {
                    // read قد يرمي استثناء إذا تم cancel() على الـ Call
                    read = in.read(buffer);
                    if (read == -1) {
                        break;
                    }

                    // فحص الإيقاف بعد كل قراءة
                    if (item.isCancelled()) {
                        Log.d(TAG, "download cancelled in loop, downloaded=" + downloadedBytes);
                        cancelNotification(context, item, false);
                        return;
                    }

                    raf.write(buffer, 0, read);
                    downloadedBytes += read;
                    item.setDownloadedBytes(downloadedBytes);

                    long now = System.currentTimeMillis();

                    // حساب السرعة التقريبية كل ثانية
                    if (now - lastSpeedLogTime >= 1000) {
                        long deltaBytes = downloadedBytes - lastSpeedBytes;
                        double kbpsLog = deltaBytes / 1024.0;
                        Log.d(TAG, "download speed ~ " + kbpsLog + " KB/s");
                        lastSpeedBytes = downloadedBytes;
                        lastSpeedLogTime = now;
                    }

                    // تحديث سرعة التحميل من DownloadConfig كل 5 ثواني
                    if (now - lastSpeedConfigCheck >= 5000) {
                        speedKbps = DownloadConfig.getDownloadSpeedKbps();
                        lastSpeedConfigCheck = now;
                        Log.d(TAG, "updated speedKbps from DownloadConfig: " + speedKbps);
                    }

                    // تطبيق حد السرعة من DownloadConfig (إن وُجد)
                    if (speedKbps > 0) {
                        double chunkKb = read / 1024.0;
                        double secondsNeeded = chunkKb / speedKbps;
                        long sleepMs = (long) (secondsNeeded * 1000);
                        if (sleepMs > 0 && sleepMs < 2000) {
                            try {
                                Thread.sleep(sleepMs);
                            } catch (InterruptedException ignored) {}
                        }
                    }

                    if (totalBytes > 0) {
                        int progress = (int) ((downloadedBytes * 100L) / totalBytes);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            item.setProgress(progress);
                            item.setStatus(DownloadItem.STATUS_DOWNLOADING);
                            postItemUpdate(context, item); // تحديث DB + UI + إشعار
                            Log.d(TAG, "download progress=" + progress + "%");
                        }
                    } else {
                        // حتى لو لا نعرف totalBytes، حدّث القيمة المحمَّلة لكي لا تبقى 0 B
                        postItemUpdate(context, item);
                    }
                }

                // لو خرجنا من الحلقة بدون إلغاء، نتحقق من الاكتمال
                if (totalBytes > 0 && downloadedBytes < totalBytes && !item.isCancelled()) {
                    Log.w(TAG, "stream ended early: downloaded=" + downloadedBytes + " total=" + totalBytes);
                    continue;
                }

                // اكتمل التحميل (ولم يُلغَ)
                if (!item.isCancelled()) {
                    // تشفير الملف الجزئي إلى الملف النهائي إن كان الجهاز يدعم ذلك
                    try {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            File finalFile = destFile;
                            // ensure parent dir exists
                            File parent = finalFile.getParentFile();
                            if (parent != null && !parent.exists()) parent.mkdirs();

                            EncryptUtils.encryptFileToFile(partFile, finalFile);
                            // حذف الملف الجزئي بعد نجاح التشفير
                            if (partFile.exists()) partFile.delete();

                            item.setFilePath(finalFile.getAbsolutePath());
                        } else {
                            // أجهزة قديمة: نعيد تسمية الملف الجزئي إلى النهائي بدون تشفير
                            if (partFile.exists()) {
                                if (partFile.renameTo(destFile)) {
                                    item.setFilePath(destFile.getAbsolutePath());
                                }
                            }
                        }

                        item.setStatus(DownloadItem.STATUS_COMPLETED);
                        item.setProgress(100);
                        item.setDownloadedBytes(downloadedBytes);
                        postItemUpdate(context, item);
                        Log.d(TAG, "download completed: " + destFile.getAbsolutePath());
                        cancelNotification(context, item, true);
                    } catch (Exception encEx) {
                        Log.e(TAG, "encryption/rename failed", encEx);
                        item.setStatus(DownloadItem.STATUS_FAILED);
                        postItemUpdate(context, item);
                        cancelNotification(context, item, false);
                        return;
                    }
                }

                success = true;

            } catch (Exception e) {
                // إذا الإلغاء من المستخدم (cancelCurrentCall أو cancelled=true) نخرج بلا محاولات أخرى
                if (item.isCancelled() || e instanceof java.net.SocketException) {
                    Log.d(TAG, "download cancelled by user with exception: " + e);
                    cancelNotification(context, item, false);
                    return;
                }
                Log.e(TAG, "download error (Range)", e);
            } finally {
                currentCall = null;
                try { if (raf != null) raf.close(); } catch (Exception ignore) {}
                try { if (in != null) in.close(); } catch (Exception ignore) {}
                if (response != null) response.close();
            }
        }

        // لو خرجنا بسبب فشل حقيقي وليس إلغاء
        if (!success && !item.isCancelled()) {
            item.setStatus(DownloadItem.STATUS_FAILED);
            postItemUpdate(context, item);
            Log.e(TAG, "download failed after " + MAX_RETRY + " attempts");
            cancelNotification(context, item, false);
        }
    }

    // دالة مساعدة لتحديث الـ DB + UI لعنصر واحد + الإشعار
    private static void postItemUpdate(Context context, DownloadItem item) {
        DownloadsRepository.updateDownloadAsync(context, item, () -> {
            for (int i = 0; i < DownloadsRepository.DOWNLOADS.size(); i++) {
                DownloadItem it = DownloadsRepository.DOWNLOADS.get(i);
                if (it.getId() == item.getId()) {
                    it.setProgress(item.getProgress());
                    it.setStatus(item.getStatus());
                    it.setDownloadedBytes(item.getDownloadedBytes());
                    it.setTotalBytes(item.getTotalBytes());
                    it.setFilePath(item.getFilePath());
                    break;
                }
            }

            DownloadsAdapter adapter = DownloadsFragment.currentAdapter;

            Log.d(TAG, "postItemUpdate for id=" + item.getId()
                    + " progress=" + item.getProgress()
                    + " downloaded=" + item.getDownloadedBytes()
                    + " total=" + item.getTotalBytes()
                    + " adapter=" + adapter);

            if (adapter != null) {
                Handler h = new Handler(context.getMainLooper());
                h.post(() -> adapter.notifyItemChangedById(item.getId()));
            }

            showOrUpdateNotification(context, item);
        });
    }

    // ================== إشعارات التحميل ==================

    private static void createChannelIfNeeded(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm.getNotificationChannel(CHANNEL_ID) != null) return;

            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "تحميلات Almash",
                    NotificationManager.IMPORTANCE_LOW
            );
            ch.setDescription("إشعارات تحميل الفيديوهات");
            nm.createNotificationChannel(ch);
        }
    }

    private static PendingIntent buildDownloadsPendingIntent(Context context) {
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        intent.putExtra("open_downloads", true);

        int flags = PendingIntent.FLAG_UPDATE_CURRENT;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            flags |= PendingIntent.FLAG_IMMUTABLE;
        }

        return PendingIntent.getActivity(context, 0, intent, flags);
    }

    private static void showOrUpdateNotification(Context context, DownloadItem item) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        PendingIntent pi = buildDownloadsPendingIntent(context);

        NotificationCompat.Builder b =
                new NotificationCompat.Builder(context, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(item.getTitle() != null ? item.getTitle() : "تحميل")
                        .setOnlyAlertOnce(true)
                        .setOngoing(true)
                        .setContentIntent(pi);

        if (item.getStatus() == DownloadItem.STATUS_COMPLETED) {
            b.setSmallIcon(android.R.drawable.stat_sys_download_done)
                    .setContentText("اكتمل التحميل")
                    .setOngoing(false)
                    .setProgress(0, 0, false);
        } else if (item.getStatus() == DownloadItem.STATUS_FAILED) {
            b.setSmallIcon(android.R.drawable.stat_notify_error)
                    .setContentText("فشل التحميل")
                    .setOngoing(false)
                    .setProgress(0, 0, false);
        } else {
            int p = item.getProgress();
            if (item.getTotalBytes() > 0) {
                b.setContentText(p + "%")
                        .setProgress(100, p, false);
            } else {
                b.setContentText("جاري التحميل...")
                        .setProgress(0, 0, true);
            }
        }

        Notification notif = b.build();
        nm.notify(NOTIF_ID, notif);
    }

    private static void cancelNotification(Context context, DownloadItem item, boolean completed) {
        NotificationManager nm =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        if (completed) {
            PendingIntent pi = buildDownloadsPendingIntent(context);

            NotificationCompat.Builder b =
                    new NotificationCompat.Builder(context, CHANNEL_ID)
                            .setSmallIcon(android.R.drawable.stat_sys_download_done)
                            .setContentTitle(item.getTitle() != null ? item.getTitle() : "تحميل")
                            .setContentText("اكتمل التحميل")
                            .setAutoCancel(true)
                            .setContentIntent(pi);
            Notification n = b.build();
            nm.notify(NOTIF_ID, n);
        } else {
            nm.cancel(NOTIF_ID);
        }
    }
}
