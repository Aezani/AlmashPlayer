package com.ali.almashplayer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * شاشة تفاصيل المسلسل + المواسم + الحلقات
 * تستقبل showRatingKey من SeriesFragment
 */
public class ShowDetailsActivity extends AppCompatActivity
        implements SeasonsAdapter.OnEpisodeClickListener {

    public static final String EXTRA_SHOW_RATING_KEY = "show_rating_key";

    private ImageView imgHeader;
    private TextView txtTitle, txtMeta, txtSummary;
    private ImageButton btnPlay, btnDownload;
    private RecyclerView recyclerSeasons;

    private PlexApiClient plexApiClient = new PlexApiClient();
    private ShowsRepository showsRepository;

    private ShowInfo showInfo;
    private List<SeasonInfo> seasons = new ArrayList<>();
    private SeasonsAdapter seasonsAdapter;

    // الحلقة المختارة حالياً (لتشغيلها أو تحميلها)
    private EpisodeInfo currentEpisode;

    // خريطة لحفظ حلقات كل موسم حتى نستخدمها في الـPlaylist
    private Map<String, List<EpisodeInfo>> seasonEpisodesMap = new HashMap<>();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_show_details);

        showsRepository = new ShowsRepository(this);

        imgHeader = findViewById(R.id.imgHeader);
        txtTitle = findViewById(R.id.txtTitle);
        txtMeta = findViewById(R.id.txtMeta);
        txtSummary = findViewById(R.id.txtSummary);
        btnPlay = findViewById(R.id.btnPlay);
        btnDownload = findViewById(R.id.btnDownload);
        recyclerSeasons = findViewById(R.id.recyclerSeasons);

        recyclerSeasons.setLayoutManager(new LinearLayoutManager(this));
        seasonsAdapter = new SeasonsAdapter(seasons, this);
        recyclerSeasons.setAdapter(seasonsAdapter);

        String showRatingKey = getIntent().getStringExtra(EXTRA_SHOW_RATING_KEY);
        if (showRatingKey == null || showRatingKey.isEmpty()) {
            Toast.makeText(this, "لا يوجد مسلسل محدد", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        btnPlay.setOnClickListener(v -> playCurrentEpisode());
        btnDownload.setOnClickListener(v -> downloadCurrentEpisode());

        // إخفاء أزرار الحلقة حتى يختار المستخدم حلقة
        btnPlay.setVisibility(View.GONE);
        btnDownload.setVisibility(View.GONE);

        loadShowAll(showRatingKey);
    }

    private void loadShowAll(String initialKey) {
        new AsyncTask<String, Void, Boolean>() {

            private Exception error;
            private List<SeasonInfo> loadedSeasons;

            @Override
            protected Boolean doInBackground(String... params) {
                String key = params[0];
                try {
                    // 1. جلب بيانات المسلسل أولاً
                    // هذه الدالة ستستخدم parseShowDetailsXml الذكية التي عدلناها
                    // وستعيد لنا showInfo يحتوي على الـ ratingKey الصحيح للمسلسل (الأب)
                    showInfo = showsRepository.getShowSync(key);

                    if (showInfo != null && showInfo.getRatingKey() != null) {
                        String actualShowKey = showInfo.getRatingKey();

                        android.util.Log.d("Fix", "الرقم الأصلي: " + key + " | الرقم الصحيح للمسلسل: " + actualShowKey);

                        // 2. طلب المواسم باستخدام الرقم الصحيح (الذي استخرجه السيرفر)
                        // الآن getShowSeasons ستستلم 27404 بدلاً من 28550
                        loadedSeasons = plexApiClient.getShowSeasons(actualShowKey);
                        return true;
                    } else {
                        return false;
                    }
                } catch (Exception e) {
                    error = e;
                    return false;
                }
            }

            @Override
            protected void onPostExecute(Boolean ok) {
                if (!ok || showInfo == null) {
                    Toast.makeText(ShowDetailsActivity.this,
                            "خطأ في جلب بيانات المسلسل أو المواسم",
                            Toast.LENGTH_LONG).show();
                    if (error != null) error.printStackTrace();
                    finish();
                    return;
                }

                seasons.clear();
                if (loadedSeasons != null) {
                    seasons.addAll(loadedSeasons);
                }
                seasonsAdapter.notifyDataSetChanged();

                bindShowHeader();
            }
        }.execute(initialKey);
    }

    private void bindShowHeader() {
        txtTitle.setText(showInfo.getTitle());

        String meta = "";
        if (showInfo.getYear() != null && !showInfo.getYear().isEmpty()) {
            meta += showInfo.getYear();
        }
        if (showInfo.getDuration() != null && !showInfo.getDuration().isEmpty()) {
            if (!meta.isEmpty()) meta += " • ";
            meta += showInfo.getDuration();
        }
        if (showInfo.getSeasonCount() > 0) {
            if (!meta.isEmpty()) meta += " • ";
            meta += showInfo.getSeasonCount() + " موسم";
        }
        txtMeta.setText(meta);

        txtSummary.setText(showInfo.getSummary() != null ? showInfo.getSummary() : "");

        String imagePath = showInfo.getThumb() != null && !showInfo.getThumb().isEmpty()
                ? showInfo.getThumb()
                : showInfo.getArt();

        if (imagePath != null && !imagePath.isEmpty()) {
            String url = Config.getBaseUrl() + imagePath + Config.getTokenQuery();
            Glide.with(this)
                    .load(url)
                    .placeholder(android.R.color.darker_gray)
                    .centerCrop()
                    .into(imgHeader);
        } else {
            imgHeader.setImageResource(android.R.color.darker_gray);
        }
    }

    // يتم استدعاؤها من SeasonsAdapter عندما يفتح المستخدم موسم معيّن
    public void loadEpisodesForSeason(SeasonInfo season, SeasonsAdapter.SeasonViewHolder holder) {
        new AsyncTask<String, Void, List<EpisodeInfo>>() {

            private Exception error;

            @Override
            protected List<EpisodeInfo> doInBackground(String... params) {
                try {
                    return plexApiClient.getSeasonEpisodes(params[0]);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<EpisodeInfo> episodes) {
                if (episodes == null || error != null) {
                    Toast.makeText(ShowDetailsActivity.this,
                            "خطأ في جلب الحلقات",
                            Toast.LENGTH_LONG).show();
                    return;
                }
                // حفظ الحلقات في الكاش حسب الموسم (في الذاكرة، ليس Room)
                seasonEpisodesMap.put(season.getRatingKey(), episodes);
                seasonsAdapter.setEpisodesForSeason(season.getRatingKey(), episodes, holder);
            }
        }.execute(season.getRatingKey());
    }

    // ======================================================
    // تنفيذ واجهة OnEpisodeClickListener من SeasonsAdapter
    // ======================================================

    @Override
    public void onEpisodeClicked(EpisodeInfo episode) {
        currentEpisode = episode;

        // تحديث الهيدر ليعرض معلومات الحلقة
        txtTitle.setText(episode.getTitle());
        String meta = "";
        if (episode.getDuration() != null && !episode.getDuration().isEmpty()) {
            meta = episode.getDuration();
        }
        txtMeta.setText(meta);
        txtSummary.setText(episode.getSummary() != null ? episode.getSummary() : "");

        String imagePath = episode.getThumb() != null && !episode.getThumb().isEmpty()
                ? episode.getThumb()
                : showInfo.getThumb();

        if (imagePath != null && !imagePath.isEmpty()) {
            String url = Config.getBaseUrl() + imagePath + Config.getTokenQuery();
            Glide.with(this)
                    .load(url)
                    .placeholder(android.R.color.darker_gray)
                    .centerCrop()
                    .into(imgHeader);
        }

        // إظهار أزرار التشغيل والتحميل
        btnPlay.setVisibility(View.VISIBLE);
        btnDownload.setVisibility(View.VISIBLE);
    }

    // ================= تشغيل الحلقة (مع Playlist داخل نفس الموسم) =================

    private void playCurrentEpisode() {
        if (currentEpisode == null || currentEpisode.getPartKey() == null) {
            Toast.makeText(this, "اختر حلقة أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        // نأتي بكل حلقات الموسم الحالي من الكاش
        String seasonKey = currentEpisode.getParentRatingKey();
        List<EpisodeInfo> seasonEpisodes = seasonEpisodesMap.get(seasonKey);

        if (seasonEpisodes == null || seasonEpisodes.isEmpty()) {
            Toast.makeText(this, "لا توجد قائمة حلقات لهذا الموسم", Toast.LENGTH_SHORT).show();
            return;
        }

        ArrayList<String> urls = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        int currentIndex = 0;

        for (int i = 0; i < seasonEpisodes.size(); i++) {
            EpisodeInfo ep = seasonEpisodes.get(i);
            if (ep.getPartKey() == null) continue;

            String url = Config.getBaseUrl()
                    + ep.getPartKey()
                    + Config.getTokenQuery();
            urls.add(url);

            String t = ep.getTitle();
            if (t == null || t.isEmpty()) {
                t = "الحلقة " + ep.getIndex();
            }
            titles.add(t);

            if (ep.getRatingKey().equals(currentEpisode.getRatingKey())) {
                currentIndex = i;
            }
        }

        if (urls.isEmpty()) {
            Toast.makeText(this, "لا توجد روابط صالحة للحلقات", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putStringArrayListExtra("episode_urls", urls);
        intent.putStringArrayListExtra("episode_titles", titles);
        intent.putExtra("current_index", currentIndex);
        startActivity(intent);
    }

    // ================= تحميل الحلقة (DownloadManager + Room + كاش خاص بالتطبيق) =================

    private void downloadCurrentEpisode() {
        if (currentEpisode == null || currentEpisode.getPartKey() == null) {
            Toast.makeText(this, "اختر حلقة أولاً", Toast.LENGTH_SHORT).show();
            return;
        }

        String showTitle = showInfo != null && showInfo.getTitle() != null
                ? showInfo.getTitle()
                : "مسلسل";

        int seasonNumber = 1; // لو EpisodeInfo عنده رقم موسم عدّله هنا
        int episodeNumber = currentEpisode.getIndex();

        String title = showTitle
                + " - موسم " + seasonNumber
                + " حلقة " + episodeNumber;

        String imagePath = currentEpisode.getThumb() != null && !currentEpisode.getThumb().isEmpty()
                ? currentEpisode.getThumb()
                : showInfo.getThumb();

        String thumbUrl = null;
        if (imagePath != null && !imagePath.isEmpty()) {
            thumbUrl = Config.getBaseUrl() + imagePath + Config.getTokenQuery();
        }

        // بدون URL الآن
        DownloadItem item = new DownloadItem(0, title, "", thumbUrl);

        RemoteConfigLoader.loadConfigAsync(getApplicationContext(), () -> {
            PlexConfig cfg = RemoteConfigLoader.getConfig();
            if (cfg == null) {
                runOnUiThread(() ->
                        Toast.makeText(this, "فشل تحميل إعدادات السيرفر", Toast.LENGTH_SHORT).show());
                return;
            }

            String baseUrl = cfg.serverUrl + ":" + cfg.nginxPort;
            String token   = cfg.token;

            String videoUrl = baseUrl
                    + currentEpisode.getPartKey()
                    + "?X-Plex-Token=" + token;

            item.setUrl(videoUrl);

            DownloadsRepository.startInternalDownloadAsync(ShowDetailsActivity.this, item, null);

            runOnUiThread(() ->
                    Toast.makeText(this, "بدأ التحميل وأُضيف إلى قائمة التحميلات", Toast.LENGTH_SHORT).show());
        });
    }
}
