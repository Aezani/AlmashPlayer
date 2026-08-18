package com.ali.almashplayer;

import androidx.appcompat.app.AppCompatActivity;

import android.net.Uri;
import android.os.Bundle;
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
import androidx.media3.common.util.UnstableApi;
import androidx.media3.exoplayer.DefaultLoadControl;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.LoadControl;
import androidx.media3.ui.PlayerView;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * مشغّل داخلي للحلقات/الأفلام باستخدام ExoPlayer (Media3)
 * يدعم:
 * - التشغيل من روابط شبكة (episode_urls)
 * - التشغيل من ملفات محلية داخل التطبيق (episode_local_paths)
 */
@OptIn(markerClass = UnstableApi.class)
public class PlayerActivity extends AppCompatActivity {

    private ExoPlayer exoPlayer;
    private PlayerView playerView;
    private TextView txtEpisodeTitleOverlay;
    private ArrayList<String> titles;
    private int currentIndex;

    // مسارات محلية اختيارية (Offline)
    private ArrayList<String> localPaths;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);

        // وضع ملء الشاشة (إخفاء أشرطة النظام)
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        WindowInsetsControllerCompat controller =
                new WindowInsetsControllerCompat(getWindow(), getWindow().getDecorView());
        controller.hide(WindowInsetsCompat.Type.systemBars());
        controller.setSystemBarsBehavior(
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);

        // منع إطفاء الشاشة أثناء المشاهدة
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        playerView = findViewById(R.id.playerView);
        txtEpisodeTitleOverlay = findViewById(R.id.txtEpisodeTitleOverlay);

        ArrayList<String> urls = getIntent().getStringArrayListExtra("episode_urls");
        titles = getIntent().getStringArrayListExtra("episode_titles");
        currentIndex = getIntent().getIntExtra("current_index", 0);

        // قراءة قائمة المسارات المحلية (إن وُجدت)
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

        // إعداد LoadControl لتقليل زمن الانتظار قبل بدء التشغيل
        LoadControl loadControl = new DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                        5_000,   // minBufferMs
                        15_000,  // maxBufferMs
                        1_500,   // bufferForPlaybackMs
                        3_000    // bufferForPlaybackAfterRebufferMs
                )
                .build();

        exoPlayer = new ExoPlayer.Builder(this)
                .setLoadControl(loadControl)
                .build();
        playerView.setPlayer(exoPlayer);

        List<MediaItem> items = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Uri uriToPlay = null;

            // 1) إذا كان هناك مسار محلي صالح، نعطيه أولوية
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

            // 2) لو لم يوجد ملف محلي أو غير موجود، نرجع لـ URL الشبكة
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

        // إعداد البلاي ليست مع موضع العنصر الحالي
        exoPlayer.setMediaItems(items, currentIndex, 0);
        exoPlayer.prepare();
        exoPlayer.setPlayWhenReady(true);

        // تعيين عنوان المادة الحالية في الـOverlay
        updateEpisodeOverlay(currentIndex);

        // تحديث العنوان تلقائياً عند الانتقال للعنصر التالي / السابق
        exoPlayer.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(MediaItem mediaItem, int reason) {
                int newIndex = exoPlayer.getCurrentMediaItemIndex();
                updateEpisodeOverlay(newIndex);
            }
        });

        // ربط ظهور/اختفاء العنوان مع كنترول PlayerView
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

    /**
     * تحديث نص الـOverlay ليعرض فقط اسم المادة (فيلم أو حلقة)
     */
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
            // إيقاف مؤقت عند الخروج المؤقت (SMS، اتصال، إلخ)
            exoPlayer.setPlayWhenReady(false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (exoPlayer != null) {
            // استئناف التشغيل عند الرجوع للتطبيق
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
