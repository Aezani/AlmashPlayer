package com.ali.almashplayer.download;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

import com.ali.almashplayer.AlmashApplication;
import com.ali.almashplayer.MainActivity;
import com.ali.almashplayer.R;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.offline.DownloadService;
import com.google.android.exoplayer2.scheduler.Scheduler;

import java.util.List;

public class AlmashDownloadService extends DownloadService {

    public static final int FOREGROUND_NOTIFICATION_ID = 1001;
    public static final String CHANNEL_ID = "almash_download_channel";

    public AlmashDownloadService() {
        super(
                FOREGROUND_NOTIFICATION_ID,
                DEFAULT_FOREGROUND_NOTIFICATION_UPDATE_INTERVAL,
                CHANNEL_ID,
                R.string.download_channel_name
        );
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
    }

    @Override
    protected DownloadManager getDownloadManager() {
        return AlmashApplication.getInstance().getDownloadManager();
    }

    @Override
    protected Scheduler getScheduler() {
        // لا نستخدم Scheduler خارجي حالياً
        return null;
    }

    /**
     * التوقيع المطلوب في نسختك من DownloadService:
     * getForegroundNotification(List<Download> downloads, int notMetRequirements)
     */
    @Override
    protected Notification getForegroundNotification(
            List<Download> downloads,
            int notMetRequirements
    ) {
        Context context = getApplicationContext();

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(
                context,
                0,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        Notification.Builder builder;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            builder = new Notification.Builder(context, CHANNEL_ID);
        } else {
            builder = new Notification.Builder(context);
        }

        // نص بسيط، يمكنك لاحقاً استخدام downloads لتجميع عدد/اسم الملف، إلخ
        String title = context.getString(R.string.download_in_progress);
        if (downloads != null && !downloads.isEmpty()) {
            title = title + " (" + downloads.size() + ")";
        }

        builder
                .setContentTitle(title)
                .setContentText(context.getString(R.string.download_in_progress))
                .setSmallIcon(R.drawable.ic_download) // أو أيقونة أخرى
                .setContentIntent(pi)
                .setOngoing(true);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Context context = getApplicationContext();
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.download_channel_name),
                    NotificationManager.IMPORTANCE_LOW
            );
            NotificationManager nm =
                    (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            if (nm != null) {
                nm.createNotificationChannel(channel);
            }
        }
    }
}
