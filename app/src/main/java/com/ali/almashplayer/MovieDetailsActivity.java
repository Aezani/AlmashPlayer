package com.ali.almashplayer;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

public class MovieDetailsActivity extends AppCompatActivity {

    public static final String EXTRA_RATING_KEY = "ratingKey";
    public static final String EXTRA_SECTION_KEY = "sectionKey";

    private ImageView imgBackdrop;
    private ImageView imgPoster;
    private TextView txtTitle;
    private TextView txtYear;
    private TextView txtDuration;
    private TextView txtContentRating;
    private TextView txtGenres;
    private TextView txtTagline;
    private TextView txtSummary;
    private RatingBar ratingBar;

    private RecyclerView recyclerCast;
    private RecyclerView recyclerSimilar;

    private CastAdapter castAdapter;
    private MovieGridAdapter similarAdapter;

    private PlexApiClient plexApiClient = new PlexApiClient();
    private String ratingKey;
    private String sectionKey;

    // لتخزين تفاصيل الفيلم الحالية لاستخدامها في أزرار التشغيل/التحميل
    private MovieDetails currentDetails;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_movie_details);

        ratingKey = getIntent().getStringExtra(EXTRA_RATING_KEY);
        sectionKey = getIntent().getStringExtra(EXTRA_SECTION_KEY);

        if (ratingKey == null) {
            Toast.makeText(this, "ratingKey مفقود", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        initViews();
        setupRecyclers();
        setupActions();   // ربط أزرار التشغيل والتحميل
        loadMovieDetails();
    }

    private void initViews() {
        imgBackdrop      = findViewById(R.id.imgBackdrop);
        imgPoster        = findViewById(R.id.imgPoster);
        txtTitle         = findViewById(R.id.txtTitle);
        txtYear          = findViewById(R.id.txtYear);
        txtDuration      = findViewById(R.id.txtDuration);
        txtContentRating = findViewById(R.id.txtContentRating);
        txtGenres        = findViewById(R.id.txtGenres);
        txtTagline       = findViewById(R.id.txtTagline);
        txtSummary       = findViewById(R.id.txtSummary);
        ratingBar        = findViewById(R.id.ratingBar);

        recyclerCast     = findViewById(R.id.recyclerCast);
        recyclerSimilar  = findViewById(R.id.recyclerSimilar);
    }

    private void setupRecyclers() {
        recyclerCast.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        castAdapter = new CastAdapter();
        recyclerCast.setAdapter(castAdapter);

        recyclerSimilar.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );
        // نهيّئ الـ adapter لاحقًا بعد جلب العناصر المشابهة
    }

    private void setupActions() {
        ImageButton btnPlayMovie = findViewById(R.id.btnPlayMovie);
        ImageButton btnDownloadMovie = findViewById(R.id.btnDownloadMovie);

        // تشغيل الفيلم
        btnPlayMovie.setOnClickListener(v -> {
            if (currentDetails == null || currentDetails.getPartKey() == null) {
                Toast.makeText(this, "بيانات الفيلم غير جاهزة بعد", Toast.LENGTH_SHORT).show();
                return;
            }

            // نفس منطق تشغيل الحلقات لكن بفيلم واحد كبلاي ليست
            String url = Config.getBaseUrl()
                    + currentDetails.getPartKey()
                    + Config.getTokenQuery(); // يحتوي X-Plex-Token. [file:11][web:146]

            ArrayList<String> urls = new ArrayList<>();
            ArrayList<String> titles = new ArrayList<>();
            urls.add(url);
            titles.add(currentDetails.getTitle() != null ? currentDetails.getTitle() : "فيلم");

            Intent intent = new Intent(this, PlayerActivity.class);
            intent.putStringArrayListExtra("episode_urls", urls);
            intent.putStringArrayListExtra("episode_titles", titles);
            intent.putExtra("current_index", 0);
            startActivity(intent);
        });

        // تحميل الفيلم
        btnDownloadMovie.setOnClickListener(v -> {
            if (currentDetails == null || currentDetails.getPartKey() == null) {
                Toast.makeText(this, "بيانات الفيلم غير جاهزة بعد", Toast.LENGTH_SHORT).show();
                return;
            }
            downloadCurrentMovie();
        });
    }

    private void loadMovieDetails() {
        new AsyncTask<Void, Void, MovieDetails>() {

            private Exception error;

            @Override
            protected MovieDetails doInBackground(Void... voids) {
                try {
                    return plexApiClient.getMovieDetails(ratingKey);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(MovieDetails details) {
                if (isFinishing() || isDestroyed()) return;

                if (error != null || details == null) {
                    Toast.makeText(MovieDetailsActivity.this,
                            "خطأ في جلب بيانات الفيلم",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                bindDetails(details);
                loadSimilarMovies(details);
            }
        }.execute();
    }

    private void bindDetails(MovieDetails d) {
        // خزِّن التفاصيل الحالية لاستخدامها مع أزرار التشغيل/التحميل
        currentDetails = d;

        // العنوان + سنة + تقييم
        txtTitle.setText(d.getTitle());
        txtYear.setText(d.getYear() != null ? d.getYear() : "");

        if (d.getDurationMs() > 0) {
            int minutes = (int) (d.getDurationMs() / 60000);
            txtDuration.setText(minutes + " دقيقة");
        } else {
            txtDuration.setText("");
        }

        txtContentRating.setText(d.getContentRating() != null ? d.getContentRating() : "");
        txtTagline.setText(d.getTagline() != null ? d.getTagline() : "");
        txtSummary.setText(d.getSummary() != null ? d.getSummary() : "");

        if (d.getGenres() != null && !d.getGenres().isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.getGenres().size(); i++) {
                if (i > 0) sb.append(" • ");
                sb.append(d.getGenres().get(i));
            }
            txtGenres.setText(sb.toString());
        } else {
            txtGenres.setText("");
        }

        if (d.getRating() > 0) {
            ratingBar.setRating((float) d.getRating() / 2f); // لو من 10 نحوله إلى 5 نجوم
        } else {
            ratingBar.setRating(0f);
        }

        // البوستر والخلفية
        if (d.getThumb() != null) {
            String urlPoster = Config.getBaseUrl() + d.getThumb() + Config.getTokenQuery();
            Glide.with(this)
                    .load(urlPoster)
                    .centerCrop()
                    .into(imgPoster);
        }

        if (d.getArt() != null) {
            String urlBackdrop = Config.getBaseUrl() + d.getArt() + Config.getTokenQuery();
            Glide.with(this)
                    .load(urlBackdrop)
                    .centerCrop()
                    .into(imgBackdrop);
        }

        // الممثلون
        castAdapter.setItems(d.getCastRoles());

        // في حال أردت جعل عنوان الـ Toolbar من اسم الفيلم:
        setTitle(d.getTitle());
    }

    private void loadSimilarMovies(MovieDetails d) {
        if (sectionKey == null || d.getGenres() == null || d.getGenres().isEmpty()) {
            return;
        }

        final String mainGenre = d.getGenres().get(0);

        new AsyncTask<Void, Void, List<LibraryItem>>() {

            private Exception error;

            @Override
            protected List<LibraryItem> doInBackground(Void... voids) {
                try {
                    return plexApiClient.getSimilarMovies(sectionKey, mainGenre, ratingKey);
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<LibraryItem> result) {
                if (isFinishing() || isDestroyed()) return;

                if (error != null || result == null || result.isEmpty()) {
                    return;
                }

                similarAdapter = new MovieGridAdapter(result, item -> {
                    // عند الضغط على فيلم مشابه نفتح نفس الـ Activity مع ratingKey جديد
                    startActivity(
                            getIntent()
                                    .putExtra(EXTRA_RATING_KEY, item.getRatingKey())
                                    .putExtra(EXTRA_SECTION_KEY, sectionKey)
                    );
                    finish();
                });
                recyclerSimilar.setAdapter(similarAdapter);
            }
        }.execute();
    }

    // ================= تحميل الفيلم (DownloadManager + نفس منطق المسلسلات) =================

    private void downloadCurrentMovie() {
        String movieTitle = currentDetails.getTitle() != null
                ? currentDetails.getTitle()
                : "فيلم";

        // اختيار صورة الغلاف للتحميل (اختياري)
        String imagePath = currentDetails.getThumb() != null && !currentDetails.getThumb().isEmpty()
                ? currentDetails.getThumb()
                : currentDetails.getArt();

        String thumbUrl = null;
        if (imagePath != null && !imagePath.isEmpty()) {
            thumbUrl = Config.getBaseUrl() + imagePath + Config.getTokenQuery(); // [file:11]
        }

        // DownloadItem بدون URL الآن
        DownloadItem item = new DownloadItem(0, movieTitle, "", thumbUrl);

        RemoteConfigLoader.loadConfigAsync(getApplicationContext(), () -> {
            PlexConfig cfg = RemoteConfigLoader.getConfig();
            if (cfg == null) {
                runOnUiThread(() ->
                        Toast.makeText(this, "فشل تحميل إعدادات السيرفر", Toast.LENGTH_SHORT).show());
                return;
            }

            String baseUrl = cfg.serverUrl + ":" + cfg.nginxPort; // مثل المسلسلات. [file:11][web:145]
            String token   = cfg.token;

            String videoUrl = baseUrl
                    + currentDetails.getPartKey()
                    + "?X-Plex-Token=" + token; // [web:146]

            item.setUrl(videoUrl);

            DownloadsRepository.startInternalDownloadAsync(MovieDetailsActivity.this, item, null); // [file:11]

            runOnUiThread(() ->
                    Toast.makeText(this, "بدأ تحميل الفيلم وأُضيف إلى قائمة التحميلات", Toast.LENGTH_SHORT).show());
        });
    }
}
