package com.ali.almashplayer.download;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;

import com.ali.almashplayer.download.AlmashDownloadService;
import com.google.android.exoplayer2.offline.DownloadRequest;
import com.google.android.exoplayer2.offline.DownloadService;

import java.util.HashMap;

public class DownloadTracker {

    private final Context context;
    private final HashMap<Uri, DownloadRequest> trackedRequests = new HashMap<>();

    public DownloadTracker(Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean isDownloaded(Uri uri) {
        return trackedRequests.containsKey(uri);
    }

    public void startDownload(final Activity activity,
                              final String url,
                              final String extension) {

        Uri uri = Uri.parse(url);

        // هوية التحميل = نفس الـ URL
        DownloadRequest request = new DownloadRequest.Builder(
                uri.toString(),   // id
                uri               // uri
        ).build();

        trackedRequests.put(uri, request);

        // اختر التوقيع المناسب لنسخة ExoPlayer لديك:
        // إذا كانت sendAddDownload بـ 4 بارامترات متاحة:
        DownloadService.sendAddDownload(
                context,
                AlmashDownloadService.class,
                request,
                true
        );

        // لو أعطى خطأ في السطر السابق، استعمل النسخة ذات 5 بارامترات بدلاً منها:
        /*
        DownloadService.sendAddDownload(
                context,
                AlmashDownloadService.class,
                request,
                0,      // stopReason
                true    // foreground
        );
        */
    }
}
