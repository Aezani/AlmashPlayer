package com.ali.almashplayer;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * مدير تحميل داخلي يعتمد OkHttp، يحفظ مباشرة في:
 * /data/user/0/com.ali.almashplayer/files/AlmashDownloads/mash/<filename>.mp4
 * (مساحة التطبيق الداخلية الخاصة)
 * مع تشفير AES/GCM للملف، ويحدّث DownloadsRepository بالتقدّم والحالة.
 * لا يستخدم Range Downloader ولا الاستئناف المتقدّم.
 */
public class PlexDownloadManager {

    private static final String TAG = "PlexDownload";

    /**
     * يبدأ تحميل فيديو Plex باستخدام OkHttp في ثريد منفصل.
     * لا يستخدم ExoPlayer DownloadService ولا PlexRangeDownloader.
     */
    public static long startDownload(Activity activity, DownloadItem item) {
        Context context = activity.getApplicationContext();

        String fileName = sanitizeFileName(item.getTitle()) + ".mp4";

        // مجلد داخلي خاص بالتطبيق:
        // /data/user/0/com.ali.almashplayer/files/AlmashDownloads/mash
        File appRoot = new File(context.getFilesDir(), "AlmashDownloads");
        Log.d(TAG, "appRoot(before) exists=" + appRoot.exists()
                + ", path=" + appRoot.getAbsolutePath());
        if (!appRoot.exists()) {
            boolean appOk = appRoot.mkdirs();
            Log.d(TAG, "appRoot.mkdirs() returned=" + appOk
                    + ", exists(after)=" + appRoot.exists());
        }

        File mashDir = new File(appRoot, "mash");
        Log.d(TAG, "mashDir(before) exists=" + mashDir.exists()
                + ", path=" + mashDir.getAbsolutePath());
        if (!mashDir.exists()) {
            boolean mashOk = mashDir.mkdirs();
            Log.d(TAG, "mashDir.mkdirs() returned=" + mashOk
                    + ", exists(after)=" + mashDir.exists());
        }

        File destFile = new File(mashDir, fileName);
        String fullPath = destFile.getAbsolutePath();

        item.setFilePath(fullPath);
        item.setStatus(DownloadItem.STATUS_DOWNLOADING);
        item.setProgress(0);
        if (destFile.exists()) {
            item.setDownloadedBytes(destFile.length());
        } else {
            item.setDownloadedBytes(0);
        }

        Log.d(TAG, "startDownload (Encrypted): url=" + item.getUrl());
        Log.d(TAG, "startDownload (Encrypted): path=" + fullPath);

        DownloadsRepository.updateDownloadAsync(
                context,
                item,
                null
        );

        // تشغيل التحميل المشفَّر في ثريد منفصل
        new Thread(() -> performDownload(context, item, destFile)).start();

        item.setDownloadId(0);
        return 0;
    }

    /**
     * التنفيذ الفعلي للتحميل باستخدام OkHttp + تشفير AES/GCM.
     * صيغة الملف الناتج:
     * [أول 12 بايت = IV] + [بيانات الفيديو مشفّرة AES/GCM/NoPadding]
     */
    static void performDownload(Context context, DownloadItem item, File destFile) {
        OkHttpClient client = AlmashApplication.getInstance().getHttpClient();
        String url = item.getUrl();

        Request request = new Request.Builder()
                .url(url)
                .build();

        long totalBytes = 0;
        long downloadedBytes = 0;

        try {
            Call call = client.newCall(request);
            Response response = call.execute();

            if (!response.isSuccessful()) {
                Log.e(TAG, "HTTP error: " + response.code());
                item.setStatus(DownloadItem.STATUS_FAILED);
                DownloadsRepository.updateDownloadAsync(context, item, null);
                return;
            }

            ResponseBody body = response.body();
            if (body == null) {
                Log.e(TAG, "Empty response body");
                item.setStatus(DownloadItem.STATUS_FAILED);
                DownloadsRepository.updateDownloadAsync(context, item, null);
                return;
            }

            totalBytes = body.contentLength();
            item.setTotalBytes(totalBytes);

            InputStream in = body.byteStream();

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                // أجهزة قديمة: حفظ بدون تشفير
                Log.w(TAG, "SDK < 23، سيتم الحفظ بدون تشفير");
                FileOutputStream out = new FileOutputStream(destFile);

                byte[] buffer = new byte[1024 * 256]; // 256KB
                int read;
                int lastProgress = 0;

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    downloadedBytes += read;
                    item.setDownloadedBytes(downloadedBytes);

                    if (totalBytes > 0) {
                        int progress = (int) ((downloadedBytes * 100) / totalBytes);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            item.setProgress(progress);
                            item.setStatus(DownloadItem.STATUS_DOWNLOADING);
                            DownloadsRepository.updateDownloadAsync(context, item, null);
                            Log.d(TAG, "download progress=" + progress + "%");
                        }
                    } else {
                        item.setProgress(0);
                    }
                }

                out.flush();
                out.close();
                in.close();

            } else {
                // أجهزة حديثة: تشفير AES/GCM مع Keystore
                byte[] iv = new byte[CryptoUtils.GCM_IV_LENGTH];
                Cipher cipher = CryptoUtils.createEncryptCipher(iv);

                // نكتب الـ IV في أول الملف، ثم البيانات المشفَّرة بعده
                FileOutputStream fileOut = new FileOutputStream(destFile);
                fileOut.write(iv); // أول 12 بايت IV

                CipherOutputStream out = new CipherOutputStream(fileOut, cipher);

                byte[] buffer = new byte[1024 * 256]; // 256KB
                int read;
                int lastProgress = 0;

                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                    downloadedBytes += read;
                    item.setDownloadedBytes(downloadedBytes);

                    if (totalBytes > 0) {
                        int progress = (int) ((downloadedBytes * 100) / totalBytes);
                        if (progress != lastProgress) {
                            lastProgress = progress;
                            item.setProgress(progress);
                            item.setStatus(DownloadItem.STATUS_DOWNLOADING);
                            DownloadsRepository.updateDownloadAsync(context, item, null);
                            Log.d(TAG, "download progress=" + progress + "%");
                        }
                    } else {
                        item.setProgress(0);
                    }
                }

                out.flush();
                out.close();
                in.close();
            }

            // اكتمل التحميل
            item.setStatus(DownloadItem.STATUS_COMPLETED);
            item.setProgress(100);
            item.setFilePath(destFile.getAbsolutePath());
            item.setDownloadedBytes(downloadedBytes);
            DownloadsRepository.updateDownloadAsync(context, item, null);

            Log.d(TAG, "download completed (encrypted/raw): " + destFile.getAbsolutePath());

        } catch (Exception e) {
            Log.e(TAG, "download error (OkHttp)", e);
            item.setStatus(DownloadItem.STATUS_FAILED);
            DownloadsRepository.updateDownloadAsync(context, item, null);
        }
    }

    private static String sanitizeFileName(String name) {
        if (name == null) return "video";
        return name.replaceAll("[\\\\/:*?\"<>|]", "_");
    }
}
