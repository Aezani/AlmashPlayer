package com.ali.almashplayer;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
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

public class SeriesFragment extends Fragment {

    private TabLayout tabLayout;
    private RecyclerView recyclerSeries;
    private ImageButton btnSearch, btnFilter;
    private TextView txtSeriesTitle;

    private MovieGridAdapter adapter;
    private final List<LibraryItem> items = new ArrayList<>();
    private final List<LibrarySection> showSections = new ArrayList<>();

    private PlexApiClient plexApiClient = new PlexApiClient();
    private ShowsListRepository showsListRepository;

    private String currentSectionKey = null;
    // تم تغيير المود الافتراضي ليكون الأحدث بدلاً من أ-ي
    private String currentMode = "الكل (الأحدث)";
    private String currentYearFilter = null;

    // للسحب بين التابات
    private GestureDetector gestureDetector;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_series, container, false);

        tabLayout = v.findViewById(R.id.tabSeriesCategories);
        recyclerSeries = v.findViewById(R.id.recyclerSeries);
        btnSearch = v.findViewById(R.id.btnSearchSeries);
        btnFilter = v.findViewById(R.id.btnFilterSeries);
        txtSeriesTitle = v.findViewById(R.id.txtSeriesTitle);

        recyclerSeries.setLayoutManager(new GridLayoutManager(getContext(), 3));

        adapter = new MovieGridAdapter(items, item -> {
            if (item == null || item.getRatingKey() == null || item.getRatingKey().isEmpty()) {
                Toast.makeText(getContext(), "بيانات غير مكتملة", Toast.LENGTH_SHORT).show();
                return;
            }

            Intent intent = new Intent(requireContext(), ShowDetailsActivity.class);
            if ("episode".equals(item.getType()) && item.getGrandparentRatingKey() != null) {
                intent.putExtra(ShowDetailsActivity.EXTRA_SHOW_RATING_KEY, item.getGrandparentRatingKey());
            } else {
                intent.putExtra(ShowDetailsActivity.EXTRA_SHOW_RATING_KEY, item.getRatingKey());
            }
            startActivity(intent);
        });
        recyclerSeries.setAdapter(adapter);

        showsListRepository = new ShowsListRepository(requireContext());

        btnSearch.setOnClickListener(view -> showSearchDialog());
        btnFilter.setOnClickListener(view -> showFilterDialog());

        // جيستشر للسحب يمين/يسار بين تبويبات المسلسلات
        gestureDetector = new GestureDetector(getContext(),
                new GestureDetector.SimpleOnGestureListener() {

                    private static final int SWIPE_THRESHOLD = 80;
                    private static final int SWIPE_VELOCITY_THRESHOLD = 50;

                    @Override
                    public boolean onDown(MotionEvent e) {
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

        recyclerSeries.setOnTouchListener((view, motionEvent) ->
                gestureDetector.onTouchEvent(motionEvent));

        loadShowSections();

        return v;
    }

    private void loadShowSections() {
        new Thread(() -> {
            Exception error = null;
            List<LibrarySection> result = null;
            try {
                List<LibrarySection> all = plexApiClient.getLibrarySections();
                List<LibrarySection> shows = new ArrayList<>();
                for (LibrarySection s : all) {
                    if ("show".equals(s.getType())) {
                        shows.add(s);
                    }
                }
                result = shows;
            } catch (Exception e) {
                error = e;
            }

            List<LibrarySection> finalResult = result;
            Exception finalError = error;

            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (finalError != null || finalResult == null || finalResult.isEmpty()) {
                    Toast.makeText(getContext(), "تعذّر تحميل مكتبات المسلسلات", Toast.LENGTH_LONG).show();
                    return;
                }

                showSections.clear();
                showSections.addAll(finalResult);
                setupTabsFromSections();
            });
        }).start();
    }

    private void setupTabsFromSections() {
        tabLayout.removeAllTabs();

        for (int i = 0; i < showSections.size(); i++) {
            LibrarySection s = showSections.get(i);
            tabLayout.addTab(tabLayout.newTab().setText(s.getTitle()), i);
        }

        if (showSections.size() > 0) {
            LibrarySection first = showSections.get(0);
            currentSectionKey = first.getKey();
            currentMode = "الكل (الأحدث)";
            currentYearFilter = null;

            TabLayout.Tab firstTab = tabLayout.getTabAt(0);
            if (firstTab != null) {
                firstTab.select();
                if (txtSeriesTitle != null) txtSeriesTitle.setText(firstTab.getText());
            }

            loadSeriesWithCache(currentSectionKey, currentMode, currentYearFilter);
        }

        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                int position = tab.getPosition();
                if (position >= 0 && position < showSections.size()) {
                    currentSectionKey = showSections.get(position).getKey();
                    currentMode = "الكل (الأحدث)";
                    currentYearFilter = null;

                    if (txtSeriesTitle != null) txtSeriesTitle.setText(tab.getText());
                    loadSeriesWithCache(currentSectionKey, currentMode, currentYearFilter);
                }
            }
            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {
                loadSeriesWithCache(currentSectionKey, currentMode, currentYearFilter);
            }
        });
    }

    private void loadSeriesWithCache(String sectionKey, String mode, @Nullable String yearFilter) {
        if (sectionKey == null) return;

        currentMode = mode;
        currentYearFilter = yearFilter;

        showsListRepository.getShowsForSectionAsync(sectionKey, mode, yearFilter, (list, error) -> {
            if (!isAdded()) return;

            requireActivity().runOnUiThread(() -> {
                if (error != null && (list == null || list.isEmpty())) {
                    Toast.makeText(getContext(), "Error: " + error.getMessage(), Toast.LENGTH_LONG).show();
                    return;
                }
                if (list == null) return;

                items.clear();
                items.addAll(list);
                adapter.notifyDataSetChanged();
            });
        });
    }

    private void showFilterDialog() {
        if (currentSectionKey == null) return;

        String[] options = new String[]{
                "الكل (الأحدث)",
                "المضاف حديثًا",
                "السنة"
        };

        new AlertDialog.Builder(getContext())
                .setTitle("تصفح بحسب")
                .setItems(options, (dialog, which) -> {
                    String selected = options[which];
                    if ("السنة".equals(selected)) {
                        showYearDialog();
                    } else {
                        loadSeriesWithCache(currentSectionKey, selected, null);
                    }
                })
                .show();
    }

    private void showYearDialog() {
        final EditText input = new EditText(getContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("مثال: 2026");

        new AlertDialog.Builder(getContext())
                .setTitle("فلترة حسب السنة")
                .setView(input)
                .setPositiveButton("تطبيق", (dialog, which) -> {
                    String year = input.getText().toString().trim();
                    if (!year.isEmpty()) {
                        loadSeriesWithCache(currentSectionKey, "السنة", year);
                    }
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void showSearchDialog() {
        if (currentSectionKey == null) return;

        final EditText input = new EditText(getContext());
        input.setHint("اكتب اسم مسلسل");

        new AlertDialog.Builder(getContext())
                .setTitle("بحث")
                .setView(input)
                .setPositiveButton("بحث", (dialog, which) -> {
                    String q = input.getText().toString().trim();
                    if (!q.isEmpty()) searchSeries(q);
                })
                .setNegativeButton("إلغاء", null)
                .show();
    }

    private void searchSeries(String query) {
        new Thread(() -> {
            try {
                List<LibraryItem> result = plexApiClient.searchInSection(currentSectionKey, query, "show");
                if (!isAdded()) return;
                requireActivity().runOnUiThread(() -> {
                    if (result != null) {
                        items.clear();
                        items.addAll(result);
                        adapter.notifyDataSetChanged();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }
}
