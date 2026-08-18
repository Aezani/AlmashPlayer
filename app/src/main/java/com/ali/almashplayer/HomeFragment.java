package com.ali.almashplayer;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.ali.almashplayer.data.HomeRepository;
import com.ali.almashplayer.data.HomeRowEntity;
import com.ali.almashplayer.data.HomeItemEntity;
import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";

    private ImageView imgHero;
    private TextView txtHeroTitle, txtHeroSubtitle;
    private Button btnHeroPlay;
    private RecyclerView recyclerHomeRows;

    // طبقة الخطأ
    private View errorOverlay;
    private Button btnRetry;

    private final List<HomeRow> rows = new ArrayList<>();
    private HomeParentAdapter homeAdapter;

    private HomeRepository homeRepository;

    private HomeItem currentHeroItem = null;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_home, container, false);

        imgHero = v.findViewById(R.id.imgHero);
        txtHeroTitle = v.findViewById(R.id.txtHeroTitle);
        txtHeroSubtitle = v.findViewById(R.id.txtHeroSubtitle);
        btnHeroPlay = v.findViewById(R.id.btnHeroPlay);
        recyclerHomeRows = v.findViewById(R.id.recyclerHomeRows);

        // عناصر شاشة الخطأ
        errorOverlay = v.findViewById(R.id.errorOverlay);
        btnRetry = v.findViewById(R.id.btnRetry);

        if (btnRetry != null) {
            btnRetry.setOnClickListener(view -> {
                // إخفاء شاشة الخطأ مؤقتًا
                if (errorOverlay != null) {
                    errorOverlay.setVisibility(View.GONE);
                }
                // طلب إعادة تحميل الكونفيج والـ Home من الـ Activity
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).retryLoadConfigAndHome();
                }
            });
        }

        recyclerHomeRows.setLayoutManager(
                new LinearLayoutManager(getContext(), LinearLayoutManager.VERTICAL, false));

        homeAdapter = new HomeParentAdapter(rows, item -> {
            if (getContext() == null) return;

            if ("movie".equals(item.getType())) {
                Intent i = new Intent(getContext(), MovieDetailsActivity.class);
                i.putExtra(MovieDetailsActivity.EXTRA_RATING_KEY, item.getRatingKey());

                if (item.getSectionKey() != null) {
                    Log.d(TAG, "open movie from home: ratingKey="
                            + item.getRatingKey() + " sectionKey=" + item.getSectionKey());
                    i.putExtra(MovieDetailsActivity.EXTRA_SECTION_KEY, item.getSectionKey());
                } else {
                    Log.w(TAG, "open movie from home: sectionKey is NULL for ratingKey="
                            + item.getRatingKey());
                }

                startActivity(i);

            } else if ("show".equals(item.getType())) {
                Log.d(TAG, "open show from home: title=" + item.getTitle()
                        + " ratingKey=" + item.getRatingKey()
                        + " sectionKey=" + item.getSectionKey());

                Intent i = new Intent(getContext(), ShowDetailsActivity.class);
                i.putExtra(ShowDetailsActivity.EXTRA_SHOW_RATING_KEY, item.getRatingKey());
                startActivity(i);

            } else if ("episode".equals(item.getType())) {
                Toast.makeText(getContext(),
                        "حلقة: " + item.getTitle(),
                        Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(getContext(),
                        "تشغيل: " + item.getTitle(),
                        Toast.LENGTH_SHORT).show();
            }
        });
        recyclerHomeRows.setAdapter(homeAdapter);

        btnHeroPlay.setOnClickListener(view -> {
            if (currentHeroItem == null || getContext() == null) return;

            if ("movie".equals(currentHeroItem.getType())) {
                Intent i = new Intent(getContext(), MovieDetailsActivity.class);
                i.putExtra(MovieDetailsActivity.EXTRA_RATING_KEY, currentHeroItem.getRatingKey());
                if (currentHeroItem.getSectionKey() != null) {
                    Log.d(TAG, "hero play movie: ratingKey="
                            + currentHeroItem.getRatingKey()
                            + " sectionKey=" + currentHeroItem.getSectionKey());
                    i.putExtra(MovieDetailsActivity.EXTRA_SECTION_KEY, currentHeroItem.getSectionKey());
                } else {
                    Log.w(TAG, "hero play movie: sectionKey is NULL for ratingKey="
                            + currentHeroItem.getRatingKey());
                }
                startActivity(i);

            } else if ("show".equals(currentHeroItem.getType())) {
                Log.d(TAG, "hero open show: title=" + currentHeroItem.getTitle()
                        + " ratingKey=" + currentHeroItem.getRatingKey()
                        + " sectionKey=" + currentHeroItem.getSectionKey());

                Intent i = new Intent(getContext(), ShowDetailsActivity.class);
                i.putExtra(ShowDetailsActivity.EXTRA_SHOW_RATING_KEY, currentHeroItem.getRatingKey());
                startActivity(i);

            } else {
                Toast.makeText(getContext(),
                        "تشغيل: " + currentHeroItem.getTitle(),
                        Toast.LENGTH_SHORT).show();
            }
        });

        if (getContext() != null) {
            homeRepository = new HomeRepository(getContext().getApplicationContext());
        }

        loadHomeData();

        return v;
    }

    // تُستدعى من MainActivity بعد أن يصبح Config جاهز (بعد Retry ناجح)
    public void reloadAfterConfigReady() {
        loadHomeData();
    }

    private void loadHomeData() {
        Log.d(TAG, "loadHomeData START");

        // عند كل محاولة تحميل، نخفي شاشة الخطأ (سيتم إظهارها فقط عند الخطأ)
        if (isAdded() && errorOverlay != null) {
            requireActivity().runOnUiThread(() -> errorOverlay.setVisibility(View.GONE));
        }

        new Thread(() -> {
            if (homeRepository == null || !isAdded()) {
                Log.w(TAG, "loadHomeData: homeRepository null or fragment not added");
                return;
            }

            // 1) تحميل من القاعدة أولاً (إن وُجد شيء)
            List<HomeRepository.HomeRowWithItems> dbRows = homeRepository.loadHomeFromDb();
            Log.d(TAG, "loadHomeData: initial dbRows size=" + dbRows.size());
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> applyRowsFromDb(dbRows));

            try {
                // 2) مزامنة من Plex
                Log.d(TAG, "loadHomeData: syncHomeFromPlex call");
                homeRepository.syncHomeFromPlex();

                // 3) بعد المزامنة، إعادة القراءة من القاعدة وتحديث الواجهة
                List<HomeRepository.HomeRowWithItems> freshRows = homeRepository.loadHomeFromDb();
                Log.d(TAG, "loadHomeData: fresh dbRows size=" + freshRows.size());
                if (!isAdded()) return;

                requireActivity().runOnUiThread(() -> {
                    applyRowsFromDb(freshRows);
                    // عند نجاح المزامنة، تأكد من إخفاء شاشة الخطأ
                    if (errorOverlay != null) {
                        errorOverlay.setVisibility(View.GONE);
                    }
                });
            } catch (Exception e) {
                Log.e(TAG, "loadHomeData: syncHomeFromPlex error", e);
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (!isAdded()) return;

                    // إظهار شاشة الخطأ فقط بدون أي Toast
                    if (errorOverlay != null) {
                        errorOverlay.setVisibility(View.VISIBLE);
                    }
                });
            }
        }).start();
    }

    private void applyRowsFromDb(List<HomeRepository.HomeRowWithItems> dbRows) {
        Log.d(TAG, "applyRowsFromDb: rowsWithItems=" + (dbRows != null ? dbRows.size() : 0));

        rows.clear();

        List<HomeItem> allItemsForHero = new ArrayList<>();

        if (dbRows != null) {
            for (HomeRepository.HomeRowWithItems rowWithItems : dbRows) {
                HomeRowEntity rowEntity = rowWithItems.row;
                List<HomeItemEntity> itemEntities = rowWithItems.items;

                Log.d(TAG, "applyRowsFromDb: rowTitle=" + rowEntity.title
                        + " items=" + (itemEntities != null ? itemEntities.size() : 0));

                List<HomeItem> rowItems = new ArrayList<>();
                if (itemEntities != null) {
                    for (HomeItemEntity ie : itemEntities) {
                        HomeItem hi = new HomeItem(ie.ratingKey, ie.title);
                        hi.setType(ie.itemType);
                        hi.setThumb(ie.thumb);
                        hi.setSectionKey(ie.sectionKey);

                        Log.d(TAG, "create HomeItem: title=" + ie.title
                                + " type=" + ie.itemType
                                + " sectionKey=" + ie.sectionKey
                                + " ratingKey=" + ie.ratingKey);

                        rowItems.add(hi);
                        allItemsForHero.add(hi);
                    }
                }

                if (!rowItems.isEmpty()) {
                    rows.add(new HomeRow(rowEntity.title, rowItems));
                }
            }
        }

        Log.d(TAG, "applyRowsFromDb: final rows size=" + rows.size()
                + " allItemsForHero=" + allItemsForHero.size());

        homeAdapter.notifyDataSetChanged();

        setupHeroFromItems(allItemsForHero);
    }

    // Hero من الأفلام فقط
    private void setupHeroFromItems(List<HomeItem> allItems) {
        Log.d(TAG, "setupHeroFromItems: count=" + (allItems == null ? 0 : allItems.size()));

        if (!isAdded()) {
            return;
        }

        if (allItems == null || allItems.isEmpty()) {
            currentHeroItem = null;
            imgHero.setImageResource(android.R.color.darker_gray);
            txtHeroTitle.setText("");
            txtHeroSubtitle.setText("");
            return;
        }

        // فلترة العناصر لتكون أفلام فقط
        List<HomeItem> movieItems = new ArrayList<>();
        for (HomeItem item : allItems) {
            if ("movie".equals(item.getType())) {
                movieItems.add(item);
            }
        }

        if (movieItems.isEmpty()) {
            // لا توجد أفلام لاختيار Hero منها
            currentHeroItem = null;
            imgHero.setImageResource(android.R.color.darker_gray);
            txtHeroTitle.setText("");
            txtHeroSubtitle.setText("");
            Log.d(TAG, "setupHeroFromItems: no movies found for hero");
            return;
        }

        // اختيار فيلم عشوائي فقط من قائمة الأفلام
        Random rnd = new Random();
        currentHeroItem = movieItems.get(rnd.nextInt(movieItems.size()));

        String heroTitle = currentHeroItem.getTitle();
        String heroThumb = currentHeroItem.getThumb();

        txtHeroTitle.setText(heroTitle != null ? heroTitle : "");
        txtHeroSubtitle.setText("فيلم");

        if (heroThumb != null) {
            String thumbUrl = Config.getBaseUrl()
                    + heroThumb
                    + Config.getTokenQuery();
            Log.d(TAG, "setupHeroFromItems: heroThumbUrl=" + thumbUrl);
            Glide.with(requireContext())
                    .load(thumbUrl)
                    .centerCrop()
                    .into(imgHero);
        } else {
            imgHero.setImageResource(android.R.color.darker_gray);
        }
    }
}
