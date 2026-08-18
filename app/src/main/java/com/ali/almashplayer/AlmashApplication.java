package com.ali.almashplayer;

import android.app.Application;
import android.util.Log;

import com.google.android.exoplayer2.database.ExoDatabaseProvider;
import com.google.android.exoplayer2.offline.Download;
import com.google.android.exoplayer2.offline.DownloadManager;
import com.google.android.exoplayer2.scheduler.Requirements;
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory;
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource;
import com.google.android.exoplayer2.upstream.cache.LeastRecentlyUsedCacheEvictor;
import com.google.android.exoplayer2.upstream.cache.SimpleCache;
import com.google.android.exoplayer2.util.Util;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.Executor;

import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AlmashApplication extends Application {

    private static AlmashApplication instance;

    // يُستخدم لإضافة هيدر X-MEDIA-KEY في OkHttp
    public static String DOWNLOAD_MEDIA_KEY = "";

    private OkHttpClient httpClient;
    private DownloadManager downloadManager;
    private DefaultDataSourceFactory dataSourceFactory;
    private SimpleCache simpleCache;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;

        initOkHttp();
        initDownloadManager();
    }

    public static AlmashApplication getInstance() {
        return instance;
    }

    // =========================
    //  OkHttp + Interceptor
    // =========================

    private void initOkHttp() {
        httpClient = new OkHttpClient.Builder()
                .addInterceptor(new DownloadInterceptor())
                .build();
    }

    public OkHttpClient getHttpClient() {
        return httpClient;
    }

    /**
     * إذا كان DOWNLOAD_MEDIA_KEY غير فارغ، يضيف هيدر X-MEDIA-KEY بقيمة key + ".mpd"
     */
    public static class DownloadInterceptor implements Interceptor {
        @Override
        public Response intercept(Chain chain) throws IOException {
            Request original = chain.request();
            Request request = original;
            try {
                if (DOWNLOAD_MEDIA_KEY != null && !DOWNLOAD_MEDIA_KEY.isEmpty()) {
                    String value = DOWNLOAD_MEDIA_KEY + ".mpd";
                    request = original.newBuilder()
                            .header("X-MEDIA-KEY", value)
                            .build();
                } else {
                    request = original.newBuilder().build();
                }
                return chain.proceed(request);
            } catch (Exception e) {
                e.printStackTrace();
                return chain.proceed(original);
            }
        }
    }

    // =========================
    //  ExoPlayer DownloadManager + SimpleCache داخلي
    // =========================

    private void initDownloadManager() {
        String userAgent = Util.getUserAgent(this, "AlmashPlayer");

        DefaultHttpDataSource.Factory httpDataSourceFactory =
                new DefaultHttpDataSource.Factory()
                        .setUserAgent(userAgent);

        dataSourceFactory = new DefaultDataSourceFactory(this, httpDataSourceFactory);

        ExoDatabaseProvider databaseProvider = new ExoDatabaseProvider(this);

        // كاش داخلي آمن: /data/data/com.ali.almashplayer/cache/exo_cache
        File cacheDir = new File(getCacheDir(), "exo_cache");
        if (!cacheDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            cacheDir.mkdirs();
        }

        LeastRecentlyUsedCacheEvictor evictor =
                new LeastRecentlyUsedCacheEvictor(100L * 1024L * 1024L); // تقريباً 100MB

        simpleCache = new SimpleCache(cacheDir, evictor, databaseProvider);

        // Executor للتحميلات
        Executor downloadExecutor = Runnable::run;

        downloadManager = new DownloadManager(
                this,
                databaseProvider,
                simpleCache,
                dataSourceFactory,
                downloadExecutor
        );

        // السماح بالتحميل مع أي شبكة متاحة (عدم تقييد المتطلبات)
        Requirements requirements = new Requirements(Requirements.NETWORK);
        downloadManager.setRequirements(requirements);

        // تأكد أن التحميلات ليست في حالة إيقاف
        //downloadManager.setDownloadsPaused(false);

        // Listener لمراقبة حالة التحميل في اللوج
        downloadManager.addListener(new DownloadManager.Listener() {
            @Override
            public void onDownloadChanged(
                    DownloadManager manager,
                    Download download,
                    Exception finalException
            ) {
                float percent;
                try {
                    percent = download.getPercentDownloaded();
                } catch (Exception e) {
                    percent = -1f;
                }

                Log.d(
                        "ExoDL",
                        "onDownloadChanged id=" + download.request.id
                                + " state=" + download.state
                                + " percent=" + percent
                                + " finalException=" + finalException
                );
            }

            @Override
            public void onDownloadsPausedChanged(DownloadManager manager, boolean downloadsPaused) {
                Log.d("ExoDL", "onDownloadsPausedChanged paused=" + downloadsPaused);
            }
        });
    }

    public DownloadManager getDownloadManager() {
        return downloadManager;
    }

    public DefaultDataSourceFactory getDataSourceFactory() {
        return dataSourceFactory;
    }

    public SimpleCache getSimpleCache() {
        return simpleCache;
    }
}
