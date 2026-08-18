package com.ali.almashplayer;

import android.content.Context;
import android.util.Log;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.InputStream;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

public class PlexMultiPartDownloader {

    private static final String TAG = "PlexMultiPartDownloader";
    private static final int PART_COUNT = 4;

    public static void startDownload(Context context, DownloadItem item, File destFile) {
        new Thread(() -> runMultiPartDownload(context, item, destFile)).start();
    }

    private static void runMultiPartDownload(Context context, DownloadItem item, File destFile) {
        try {
            OkHttpClient client = AlmashApplication.getInstance().getHttpClient();
            String url = item.getUrl();

            // 1) نحصل حجم الملف أولاً برأس واحد بدون Range
            Request headReq = new Request.Builder()
                    .url(url)
                    .addHeader("Connection", "close")
                    .addHeader("Accept-Encoding", "identity")
                    .build();

            Response headResp = client.newCall(headReq).execute();
            if (!headResp.isSuccessful() || headResp.body() == null) {
                Log.e(TAG, "HEAD/size request failed, code=" + headResp.code());
                item.setStatus(DownloadItem.STATUS_FAILED);
                DownloadsRepository.updateDownloadAsync(context, item, null);
                return;
            }

            long totalBytes = headResp.body().contentLength();
            headResp.close();

            if (totalBytes <= 0) {
                Log.e(TAG, "invalid content length: " + totalBytes);
                item.setStatus(DownloadItem.STATUS_FAILED);
                DownloadsRepository.updateDownloadAsync(context, item, null);
                return;
            }

            item.setTotalBytes(totalBytes);
            item.setStatus(DownloadItem.STATUS_DOWNLOADING);
            DownloadsRepository.updateDownloadAsync(context, item, null);

            // 2) تجهيز الملف بالحجم الكامل
            RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
            raf.setLength(totalBytes);
            raf.close();

            // 3) تقسيم الملف إلى أجزاء
            long partSize = totalBytes / PART_COUNT;
            DownloadPartThread[] threads = new DownloadPartThread[PART_COUNT];

            for (int i = 0; i < PART_COUNT; i++) {
                long start = i * partSize;
                long end = (i == PART_COUNT - 1) ? (totalBytes - 1) : (start + partSize - 1);

                threads[i] = new DownloadPartThread(
                        context,
                        url,
                        destFile,
                        start,
                        end,
                        item
                );
                threads[i].start();
            }

            // 4) انتظار كل الأجزاء
            for (DownloadPartThread t : threads) {
                t.join();
            }

            // التحقق النهائي: كل الأجزاء انتهت بدون فشل
            if (item.getStatus() == DownloadItem.STATUS_FAILED) {
                Log.e(TAG, "multipart download failed");
                return;
            }

            item.setDownloadedBytes(totalBytes);
            item.setProgress(100);
            item.setStatus(DownloadItem.STATUS_COMPLETED);
            item.setFilePath(destFile.getAbsolutePath());
            DownloadsRepository.updateDownloadAsync(context, item, null);
            Log.d(TAG, "multipart download completed: " + destFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "multipart download error", e);
            item.setStatus(DownloadItem.STATUS_FAILED);
            DownloadsRepository.updateDownloadAsync(context, item, null);
        }
    }

    /**
     * ثريد لتحميل جزء واحد من الملف عبر Range
     */
    private static class DownloadPartThread extends Thread {

        private final Context context;
        private final String url;
        private final File destFile;
        private final long start;
        private final long end;
        private final DownloadItem item;

        DownloadPartThread(Context context, String url, File destFile,
                           long start, long end, DownloadItem item) {
            this.context = context.getApplicationContext();
            this.url = url;
            this.destFile = destFile;
            this.start = start;
            this.end = end;
            this.item = item;
        }

        @Override
        public void run() {
            OkHttpClient client = AlmashApplication.getInstance().getHttpClient();
            String rangeHeader = "bytes=" + start + "-" + end;
            Log.d(TAG, "part range=" + rangeHeader);

            try {
                Request request = new Request.Builder()
                        .url(url)
                        .addHeader("Connection", "close")
                        .addHeader("Accept-Encoding", "identity")
                        .addHeader("Range", rangeHeader)
                        .build();

                Response response = client.newCall(request).execute();
                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "part HTTP error code=" + response.code());
                    item.setStatus(DownloadItem.STATUS_FAILED);
                    DownloadsRepository.updateDownloadAsync(context, item, null);
                    return;
                }

                ResponseBody body = response.body();
                InputStream in = body.byteStream();

                RandomAccessFile raf = new RandomAccessFile(destFile, "rw");
                raf.seek(start);

                byte[] buffer = new byte[64 * 1024];
                int read;
                long partDownloaded = 0;

                while ((read = in.read(buffer)) != -1) {
                    raf.write(buffer, 0, read);
                    partDownloaded += read;

                    // تحديث إجمالي التقدم بشكل تقريبي
                    synchronized (item) {
                        long globalDownloaded = item.getDownloadedBytes() + read;
                        item.setDownloadedBytes(globalDownloaded);

                        long totalBytes = item.getTotalBytes();
                        if (totalBytes > 0) {
                            int progress = (int) ((globalDownloaded * 100) / totalBytes);
                            if (progress != item.getProgress()) {
                                item.setProgress(progress);
                                item.setStatus(DownloadItem.STATUS_DOWNLOADING);
                                DownloadsRepository.updateDownloadAsync(context, item, null);
                                Log.d(TAG, "multipart progress=" + progress + "%");
                            }
                        }
                    }
                }

                raf.close();
                in.close();
                body.close();

                Log.d(TAG, "part finished: " + rangeHeader + ", bytes=" + partDownloaded);

            } catch (Exception e) {
                Log.e(TAG, "part error: " + rangeHeader, e);
                synchronized (item) {
                    item.setStatus(DownloadItem.STATUS_FAILED);
                    DownloadsRepository.updateDownloadAsync(context, item, null);
                }
            }
        }
    }
}
