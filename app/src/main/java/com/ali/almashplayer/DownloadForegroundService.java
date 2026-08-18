package com.ali.almashplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.IBinder;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;

public class DownloadForegroundService extends Service {

    public static final String CHANNEL_ID = "almash_download_channel";
    public static final int NOTIF_ID = 1002;

    // آخر نسبة معروضة (لتقليل التحديثات)
    private int lastProgress = -1;

    public static void start(Context context, DownloadItem item) {
        Intent i = new Intent(context, DownloadForegroundService.class);
        i.putExtra("id", item.getId());
        i.putExtra("title", item.getTitle());
        context.startForegroundService(i);
    }

    public static void stop(Context context) {
        Intent i = new Intent(context, DownloadForegroundService.class);
        context.stopService(i);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createChannel();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        long id = intent != null ? intent.getLongExtra("id", 0) : 0;
        String title = intent != null ? intent.getStringExtra("title") : "تحميل";

        Notification notif = buildNotification(title, 0, false);
        startForeground(NOTIF_ID, notif);
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel ch = new NotificationChannel(
                    CHANNEL_ID,
                    "تحميلات Almash",
                    NotificationManager.IMPORTANCE_LOW
            );
            ch.setDescription("إشعارات تحميل الفيديوهات");
            NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            nm.createNotificationChannel(ch);
        }
    }

    private Notification buildNotification(String title, int progress, boolean indeterminate) {
        NotificationCompat.Builder b =
                new NotificationCompat.Builder(this, CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.stat_sys_download)
                        .setContentTitle(title)
                        .setContentText(indeterminate ? "جاري التحميل..." : progress + "%")
                        .setOnlyAlertOnce(true)
                        .setOngoing(true)
                        .setProgress(100, indeterminate ? 0 : progress, indeterminate);

        return b.build();
    }

    public void updateProgress(String title, int progress, boolean indeterminate) {
        if (progress == lastProgress && !indeterminate) return;
        lastProgress = progress;

        Notification notif = buildNotification(title, progress, indeterminate);
        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, notif);
    }

    public void complete(String title) {
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_sys_download_done)
                .setContentTitle(title)
                .setContentText("اكتمل التحميل")
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, notif);
        stopForeground(true);
        stopSelf();
    }

    public void error(String title) {
        Notification notif = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.stat_notify_error)
                .setContentTitle(title)
                .setContentText("فشل التحميل")
                .setAutoCancel(true)
                .build();

        NotificationManager nm = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        nm.notify(NOTIF_ID, notif);
        stopForeground(true);
        stopSelf();
    }
}
