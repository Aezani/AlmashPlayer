package com.ali.almashplayer;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class SingleFileDownloader {

    public interface Listener {
        void onProgress(int progress);
        void onSuccess(File file);
        void onError(Throwable t);
    }

    private static final String TAG = "SingleFileDownloader";

    private static final OkHttpClient DEFAULT_CLIENT = new OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(300, TimeUnit.SECONDS)
            .writeTimeout(300, TimeUnit.SECONDS)
            .build();

    private final OkHttpClient client;
    private final Handler mainHandler;

    public SingleFileDownloader(OkHttpClient client) {
        this.client = client != null ? client : DEFAULT_CLIENT;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public void download(
            final String url,
            final File destinationFile,
            final Listener listener
    ) {
        new Thread(() -> {
            Call call = null;
            try {
                if (listener != null) {
                    postProgress(listener, 0);
                }

                // طلب HTTP مع ترويسات لتقليل مشاكل end of stream
                Request request = new Request.Builder()
                        .url(url)
                        .header("Connection", "close")          // تجنّب keep-alive
                        .header("Accept-Encoding", "identity")  // بدون gzip
                        .build();

                call = client.newCall(request);
                Response response = call.execute();

                if (!response.isSuccessful() || response.body() == null) {
                    throw new RuntimeException("HTTP error code: " + response.code());
                }

                long contentLength = response.body().contentLength();
                if (contentLength <= 0) {
                    contentLength = -1;
                }

                InputStream is = null;
                FileOutputStream fos = null;
                try {
                    is = response.body().byteStream();

                    File parent = destinationFile.getParentFile();
                    if (parent != null && !parent.exists()) {
                        boolean ok = parent.mkdirs();
                        Log.d(TAG, "mkdirs(" + parent.getAbsolutePath() + ") = " + ok);
                    }
                    Log.d(TAG, "parent exists = " + (parent != null && parent.exists()));

                    fos = new FileOutputStream(destinationFile);

                    byte[] buffer = new byte[8 * 1024];
                    long totalRead = 0;
                    int read;
                    int lastProgress = 0;

                    while ((read = is.read(buffer)) != -1) {
                        fos.write(buffer, 0, read);
                        totalRead += read;

                        if (contentLength > 0) {
                            int progress = (int) (totalRead * 100 / contentLength);
                            if (progress != lastProgress) {
                                lastProgress = progress;
                                if (listener != null) {
                                    postProgress(listener, progress);
                                }
                            }
                        }
                    }

                    fos.flush();

                    if (contentLength <= 0 && listener != null) {
                        postProgress(listener, 100);
                    }

                    if (listener != null) {
                        File finalFile = destinationFile;
                        mainHandler.post(() -> listener.onSuccess(finalFile));
                    }

                } finally {
                    try {
                        if (is != null) is.close();
                    } catch (Exception ignore) {}
                    try {
                        if (fos != null) fos.close();
                    } catch (Exception ignore) {}
                    response.close();
                }

            } catch (Throwable t) {
                Log.e(TAG, "download error", t);
                if (listener != null) {
                    Throwable finalT = t;
                    mainHandler.post(() -> listener.onError(finalT));
                }
                if (call != null) {
                    call.cancel();
                }
            }
        }).start();
    }

    private void postProgress(Listener listener, int progress) {
        mainHandler.post(() -> {
            try {
                listener.onProgress(progress);
            } catch (Exception e) {
                Log.e(TAG, "onProgress error", e);
            }
        });
    }
}
