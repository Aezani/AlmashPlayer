package com.ali.almashplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.OptIn;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import androidx.media3.common.MediaItem;
import androidx.media3.common.Player;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.ui.PlayerView;
import androidx.media3.datasource.DefaultDataSource;
import androidx.media3.datasource.DefaultHttpDataSource;
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {

    private static final String TAG = "PlayerActivity";
    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private TextView txtEpisodeTitleOverlay;
    private ArrayList<String> titles;
    private int currentIndex;
    private ArrayList<String> localPaths;

    // retry config
    private int retryCount = 0;
    private static final int MAX_RETRIES = 3;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.playerView);
        txtEpisodeTitleOverlay = findViewById(R.id.txtEpisodeTitleOverlay);

        ArrayList<String> urls = getIntent().getStringArrayListExtra("episode_urls");
        titles = getIntent().getStringArrayListExtra("episode_titles");
        currentIndex = getIntent().getIntExtra("current_index", 0);
        localPaths = getIntent().getStringArrayListExtra("episode_local_paths");

        if ((urls == null || urls.isEmpty())
                && (localPaths == null || localPaths.isEmpty())) {
            Toast.makeText(this, "قائمة الحلقات فارغة", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        int size = (urls != null && !urls.isEmpty()) ? urls.size()
                : (localPaths != null ? localPaths.size() : 0);

        if (currentIndex < 0 || currentIndex >= size) {
            currentIndex = 0;
        }

        // إعداد LoadControl (تقدر تضبط القيم إذا لاحظت rebuffering كثير)
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        5_000,   // minBufferMs
                        15_000,  // maxBufferMs
                        1_500,   // bufferForPlaybackMs
                        3_000    // bufferForPlaybackAfterRebufferMs
                )
                .build();

        // --- إعداد DataSource HTTP مع تهيئات مفيدة للـ Plex (redirects, user-agent, timeouts)
        DefaultHttpDataSource.Factory httpFactory = new DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent("AlmashPlayer/1.0")
                .setConnectTimeoutMs(10_000)
                .setReadTimeoutMs(10_000);

        DefaultDataSource.Factory dataSourceFactory = new DefaultDataSource.Factory(this, httpFactory);

        DefaultMediaSourceFactory mediaSourceFactory = new DefaultMediaSourceFactory(dataSourceFactory);

        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .setMediaSourceFactory(mediaSourceFactory)
                .build();

        playerView.setPlayer(exoPlayer);

        List<MediaItem> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Uri uriToPlay = null;
            if (localPaths != null
                    && i < localPaths.size()
                    && localPaths.get(i) != null
                    && !localPaths.get(i).isEmpty()) {
                String path = localPaths.get(i);
                File f = new File(path);
                if (f.exists()) {
                    uriToPlay = Uri.fromFile(f);
                }
            }
            if (uriToPlay == null && urls != null && i < urls.size()) {
                String url = urls.get(i);
                if (url != null && !url.isEmpty()) {
                    uriToPlay = Uri.parse(url);
                }
            }
            if (uriToPlay != null) {
                MediaItem mediaItem = MediaItem.fromUri(uriToPlay);
                items.add(mediaItem);
            }
        }

        if (items.isEmpty()) {
            Toast.makeText(this, "لا يوجد مصدر صالح للتشغيل", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        exoPlayer.setMediaItems(items, currentIndex, 0);
        exoPlayer.prepare();
        // استعمل play() الحديثة لضمان التشغيل
        exoPlayer.setPlayWhenReady(true);
        exoPlayer.play();

        updateEpisodeOverlay(currentIndex);

        // إضافة Listener لرصد الأخطاء وحالات التحضير
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_BUFFERING:
                        Log.d(TAG, "STATE_BUFFERING");
                        // يمكن إظهار مؤشر تحميل هنا إذا لم يكن ظاهراً في PlayerView
                        break;
                    case Player.STATE_READY:
                        Log.d(TAG, "STATE_READY, isPlaying=" + exoPlayer.isPlaying());
                        retryCount = 0; // نجاح => إعادة تعيين العدّاد
                        break;
                    case Player.STATE_ENDED:
                        Log.d(TAG, "STATE_ENDED");
                        break;
                    case Player.STATE_IDLE:
                        Log.d(TAG, "STATE_IDLE");
                        break;
                }
            }

            @Override
            public void onIsPlayingChanged(boolean isPlaying) {
                Log.d(TAG, "onIsPlayingChanged: " + isPlaying);
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                Log.e(TAG, "Playback error: " + error.getMessage(), error);
                handlePlaybackError(error);
            }

            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                int newIndex = exoPlayer.getCurrentMediaItemIndex();
                updateEpisodeOverlay(newIndex);
            }
        });

        playerView.setControllerVisibilityListener(
                new PlayerView.ControllerVisibilityListener() {
                    @Override
                    public void onVisibilityChanged(int visibility) {
                        if (visibility == View.VISIBLE) {
                            txtEpisodeTitleOverlay.setVisibility(View.VISIBLE);
                        } else {
                            txtEpisodeTitleOverlay.setVisibility(View.GONE);
                        }
                    }
                }
        );
    }

    private void handlePlaybackError(PlaybackException error) {
        // محاولة اعادة تحميل تلقائية محدودة
        if (retryCount < MAX_RETRIES) {
            retryCount++;
            Log.i(TAG, "Retry playback, attempt " + retryCount + "/" + MAX_RETRIES);
            Toast.makeText(this, "حدثت مشكلة في التحميل، جاري إعادة المحاولة... (" + retryCount + ")", Toast.LENGTH_SHORT).show();

            int index = exoPlayer.getCurrentMediaItemIndex();
            exoPlayer.setPlayWhenReady(false);
            exoPlayer.stop();
            exoPlayer.clearMediaItems();

            // إعادة تعيين نفس القائمة: نستخدم media items من المتغيرات الأولى بإعادة بنائها
            // للحصول على قائمة ثابتة، قد نخزنها عند الإنشاء؛ هنا نعيد بناء سريعًا من الإنتنت
            ArrayList<String> urls = getIntent().getStringArrayListExtra("episode_urls");
            ArrayList<String> localPaths = getIntent().getStringArrayListExtra("episode_local_paths");
            List<MediaItem> items = new ArrayList<>();
            int size = (urls != null && !urls.isEmpty()) ? urls.size()
                    : (localPaths != null ? localPaths.size() : 0);
            for (int i = 0; i < size; i++) {
                Uri uriToPlay = null;
                if (localPaths != null
                        && i < localPaths.size()
                        && localPaths.get(i) != null
                        && !localPaths.get(i).isEmpty()) {
                    String path = localPaths.get(i);
                    File f = new File(path);
                    if (f.exists()) {
                        uriToPlay = Uri.fromFile(f);
                    }
                }
                if (uriToPlay == null && urls != null && i < urls.size()) {
                    String url = urls.get(i);
                    if (url != null && !url.isEmpty()) {
                        uriToPlay = Uri.parse(url);
                    }
                }
                if (uriToPlay != null) {
                    MediaItem mediaItem = MediaItem.fromUri(uriToPlay);
                    items.add(mediaItem);
                }
            }

            if (items.isEmpty()) {
                Toast.makeText(this, "لا يوجد مصدر صالح لإعادة المحاولة", Toast.LENGTH_LONG).show();
                return;
            }

            exoPlayer.setMediaItems(items, Math.max(0, Math.min(index, items.size() - 1)), 0);
            exoPlayer.prepare();
            exoPlayer.setPlayWhenReady(true);
            exoPlayer.play();
        } else {
            Log.e(TAG, "Max retries reached. Showing error to user.");
            Toast.makeText(this, "فشل تشغيل الفيديو. تأكد من اتصال الشبكة أو جرب مصدر آخر.", Toast.LENGTH_LONG).show();
            // هنا يمكن إظهار حوار مفصل مع خيار "إعادة المحاولة" يدوياً
        }
    }

    private void updateEpisodeOverlay(int index) {
        String text;
        if (titles != null && !titles.isEmpty()
                && index >= 0 && index < titles.size()) {
            String title = titles.get(index);
            if (title == null || title.isEmpty()) {
                text = "بدون عنوان";
            } else {
                text = title;
            }
        } else {
            text = "بدون عنوان";
        }
        txtEpisodeTitleOverlay.setText(text);
        setTitle(text);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null) {
            exoPlayer.setPlayWhenReady(true);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (exoPlayer != null) {
            exoPlayer.release();
            exoPlayer = null;
        }
    }
}
