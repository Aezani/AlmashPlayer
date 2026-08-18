package com.ali.almashplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.InputType;
import android.graphics.Typeface;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

public class MoviesFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView recyclerMovies;
    private ImageButton btnSearch, btnFilter;
    private TextView txtMoviesTitle;

    private MovieGridAdapter adapter;
    private List<LibraryItem> items = new ArrayList<>();

    // الأقسام (مكتبات Plex من نوع movie)
    private List<LibrarySection> movieSections = new ArrayList<>();

    private PlexApiClient plexApiClient = new PlexApiClient();
    private String currentSectionKey = null;

    private boolean isSearchMode = false;

    // للسحب بين التابات
    private GestureDetector gestureDetector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View v = inflater.inflate(R.layout.fragment_movies, container, false);

        tabLayout      = v.findViewById(R.id.tabMoviesCategories);
        recyclerMovies = v.findViewById(R.id.recyclerMovies);
        btnSearch      = v.findViewById(R.id.btnSearchMovies);
        btnFilter      = v.findViewById(R.id.btnFilterMovies);
        txtMoviesTitle = v.findViewById(R.id.txtMoviesTitle);

        recyclerMovies.setLayoutManager(new GridLayoutManager(getContext(), 3));

        // ربط الضغط بواجهة التفاصيل
        adapter = new MovieGridAdapter(items, item -> {
            if (getContext() != null) {
                Intent intent = new Intent(getContext(), MovieDetailsActivity.class);
                intent.putExtra(MovieDetailsActivity.EXTRA_RATING_KEY, item.getRatingKey());
                intent.putExtra(MovieDetailsActivity.EXTRA_SECTION_KEY, currentSectionKey);
                startActivity(intent);
            }
        });
        recyclerMovies.setAdapter(adapter);

        btnSearch.setOnClickListener(view -> showSearchDialog());
        btnFilter.setOnClickListener(view -> showFilterDialog());

        // تهيئة الجيستشر للسحب يمين/يسار بين التابات (على الـRecyclerView)
        gestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {

                    private static final int SWIPE_THRESHOLD = 80;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 50;

                    @Override
                    public boolean onDown(MotionEvent e) {
                        // مهم حتى يستقبل onScroll/onFling
                        return true;
                    }

                    @Override
                    public boolean onFling(MotionEvent e1, MotionEvent e2,
                                           float velocityX, float velocityY) {
                        if (e1 == null || e2 == null) return false;

                        float diffX = e2.getX() - e1.getX();
                        float diffY = e2.getY() - e1.getY();

                        if (Math.abs(diffX) > Math.abs(diffY)
                                && Math.abs(diffX) > SWIPE_THRESHOLD
                                && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {

                            int currentIndex = tabLayout.getSelectedTabPosition();
                            int tabCount = tabLayout.getTabCount();

                            if (diffX < 0) {
                                // سحب لليسار -> التاب التالي
                                int nextIndex = currentIndex + 1;
                                if (nextIndex < tabCount) {
                                    TabLayout.Tab tab = tabLayout.getTabAt(nextIndex);
                                    if (tab != null) tab.select();
                                }
                            } else {
                                // سحب لليمين -> التاب السابق
                                int prevIndex = currentIndex - 1;
                                if (prevIndex >= 0) {
                                    TabLayout.Tab tab = tabLayout.getTabAt(prevIndex);
                                    if (tab != null) tab.select();
                                }
                            }
                            return true;
                        }
                        return false;
                    }
                });

        recyclerMovies.setOnTouchListener((view, motionEvent) ->
                gestureDetector.onTouchEvent(motionEvent));

        // تحميل مكتبات الأفلام من Plex
        loadMovieSections();

        return v;
    }

    /**
     * جلب مكتبات Plex وتصفية مكتبات الأفلام فقط
     */
    private void loadMovieSections() {
        new AsyncTask<Void, Void, List<LibrarySection>>() {

            private Exception error;

            @Override
            protected List<LibrarySection> doInBackground(Void... voids) {
                try {
                    List<LibrarySection> all = plexApiClient.getLibrarySections();
                    List<LibrarySection> movies = new ArrayList<>();
                    for (LibrarySection s : all) {
                        if ("movie".equals(s.getType())) {
                            movies.add(s);
                        }
                    }
                    return movies;
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<LibrarySection> result) {
                if (!isAdded()) return;

                if (error != null || result == null || result.isEmpty()) {
                    Toast.makeText(getContext(),
                            "تعذّر تحميل مكتبات الأفلام",
                            Toast.LENGTH_LONG).show();
                    tabLayout.removeAllTabs();
                    currentSectionKey = null;
                    items.clear();
                    adapter.notifyDataSetChanged();
                    return;
                }

                movieSections.clear();
                movieSections.addAll(result);
                setupTabsFromSections();
            }
        }.execute();
    }

    /**
     * إنشاء Tabs ديناميكياً من مكتبات Plex (type = movie)
     * + جعل التاب المختار Bold
     */
    private void setupTabsFromSections() {
        tabLayout.removeAllTabs();

        for (int i = 0; i < movieSections.size(); i++) {
            LibrarySection s = movieSections.get(i);
            tabLayout.addTab(tabLayout.newTab().setText(s.getTitle()), i);
        }

        // اختيار أول مكتبة كافتراضي
        LibrarySection first = movieSections.get(0);
        currentSectionKey = first.getKey();

        if (tabLayout.getTabCount() > 0) {
            TabLayout.Tab firstTab = tabLayout.getTabAt(0);
            if (firstTab != null) {
                firstTab.select();

                // جعل أول تاب Bold مبدئياً
                View tabView = firstTab.view;
                if (tabView instanceof ViewGroup) {
                    TextView tv = findTextViewInTab((ViewGroup) tabView);
                    if (tv != null) {
                        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
                    }
                }
            }
        }

        isSearchMode = false;
        loadMovies(currentSectionKey, "الكل (أ - ي)", null);

        // مستمع لتغيير التاب + جعل المختار Bold فقط في شريط التبويبات
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position >= 0 && position < movieSections.size()) {
                    currentSectionKey = movieSections.get(position).getKey();
                    isSearchMode = false;
                    loadMovies(currentSectionKey, "الكل (أ - ي)", null);
                }

                // التاب المختار: Bold
                View tabView = tab.view;
                if (tabView instanceof ViewGroup) {
                    TextView tv = findTextViewInTab((ViewGroup) tabView);
                    if (tv != null) {
                        tv.setTypeface(tv.getTypeface(), Typeface.BOLD);
                    }
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {
                // التاب غير المختار: Normal
                View tabView = tab.view;
                if (tabView instanceof ViewGroup) {
                    TextView tv = findTextViewInTab((ViewGroup) tabView);
                    if (tv != null) {
                        tv.setTypeface(tv.getTypeface(), Typeface.NORMAL);
                    }
                }
            }

            @Override
            public void onTabReselected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position >= 0 && position < movieSections.size()) {
                    currentSectionKey = movieSections.get(position).getKey();
                    isSearchMode = false;
                    loadMovies(currentSectionKey, "الكل (أ - ي)", null);
                }
            }
        });
    }

    /**
     * البحث عن أول TextView داخل ViewGroup الخاص بالتبويب
     */
    @Nullable
    private TextView findTextViewInTab(ViewGroup tabView) {
        for (int i = 0; i < tabView.getChildCount(); i++) {
            View child = tabView.getChildAt(i);
            if (child instanceof TextView) {
                return (TextView) child;
            } else if (child instanceof ViewGroup) {
                TextView nested = findTextViewInTab((ViewGroup) child);
                if (nested != null) return nested;
            }
        }
        return null;
    }

    // واجهة مبسّطة تستدعى بدون سنة
    private void loadMovies(String sectionKey, String mode) {
        loadMovies(sectionKey, mode, null);
    }

    // تحميل الأفلام مع فلترة اختيارية بالسنة
    private void loadMovies(String sectionKey, String mode, @Nullable String yearFilter) {
        if (sectionKey == null) {
            return;
        }

        new AsyncTask<String, Void, List<LibraryItem>>() {

            private Exception error;

            @Override
            protected List<LibraryItem> doInBackground(String... params) {
                String key = params[0];
                String selectedMode = params[1];
                String year = params.length > 2 ? params[2] : null;

                try {
                    if ("المضاف حديثًا".equals(selectedMode)) {
                        return plexApiClient.getRecentlyAdded(key);
                    } else if ("السنة".equals(selectedMode) && year != null && !year.isEmpty()) {
                        String sortQuery = "?year=" + year + "&sort=addedAt:desc";
                        return plexApiClient.getLibraryItems(key, sortQuery);
                    } else {
                        return plexApiClient.getLibraryItems(
                                key,
                                "?sort=addedAt:desc");
                    }
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<LibraryItem> result) {
                if (!isAdded()) return;

                if (error != null || result == null) {
                    Toast.makeText(getContext(),
                            "Error: " + (error != null ? error.getMessage() : "null result"),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                items.clear();
                items.addAll(result);
                adapter.notifyDataSetChanged();
            }
        }.execute(sectionKey, mode, yearFilter != null ? yearFilter : "");
    }

    private void showFilterDialog() {
        if (currentSectionKey == null) {
            Toast.makeText(getContext(),
                    "لم يتم تحميل الأقسام بعد",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        String[] options = new String[]{
                "الكل (أ - ي)",
                "المضاف حديثًا",
                "السنة"
        };

        new AlertDialog.Builder(getContext())
                .setTitle("تصفح بحسب")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];

                    if ("السنة".equals(selected)) {
                        isSearchMode = false;
                        showYearDialog();
                    } else {
                        isSearchMode = false;
                        loadMovies(currentSectionKey, selected, null);
                    }
                })
                .show();
    }

    private void showYearDialog() {
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("مثال: 2024");

        new AlertDialog.Builder(getContext())
                .setTitle("فلترة حسب السنة")
                .setView(input)
                .setPositiveButton("تطبيق", (dialog, which) -> {
                    String year = input.getText().toString().trim();
                    if (!year.isEmpty()) {
                        isSearchMode = false;
                        loadMovies(currentSectionKey, "السنة", year);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showSearchDialog() {
        if (currentSectionKey == null) {
            Toast.makeText(getContext(),
                    "لم يتم تحميل الأقسام بعد",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        input.setHint("اكتب اسم فيلم");

        new AlertDialog.Builder(getContext())
                .setTitle("بحث عن فيلم")
                .setView(input)
                .setPositiveButton("بحث", (dialog, which) -> {
                    String q = input.getText().toString().trim();
                    if (!q.isEmpty()) {
                        searchMovies(q);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void searchMovies(String query) {
        new AsyncTask<String, Void, List<LibraryItem>>() {

            private Exception error;

            @Override
            protected List<LibraryItem> doInBackground(String... params) {
                String q = params[0];
                try {
                    return plexApiClient.searchInSection(currentSectionKey, q, "movie");
                } catch (Exception e) {
                    error = e;
                    return null;
                }
            }

            @Override
            protected void onPostExecute(List<LibraryItem> result) {
                if (!isAdded()) return;

                if (error != null || result == null) {
                    Toast.makeText(getContext(),
                            "Error: " + (error != null ? error.getMessage() : "null result"),
                            Toast.LENGTH_LONG).show();
                    return;
                }

                isSearchMode = true;

                items.clear();
                items.addAll(result);
                adapter.notifyDataSetChanged();
            }
        }.execute(query);
    }
}
