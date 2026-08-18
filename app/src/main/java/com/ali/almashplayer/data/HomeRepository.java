package com.ali.almashplayer.data;

import android.content.Context;
import android.util.Log;

import com.ali.almashplayer.AppDatabase;
import com.ali.almashplayer.LibraryItem;
import com.ali.almashplayer.LibrarySection;
import com.ali.almashplayer.PlexApiClient;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class HomeRepository {

    private static final String TAG = "HomeRepo";

    private final AppDatabase db;
    private final HomeDao homeDao;
    private final PlexApiClient plexApiClient;

    public HomeRepository(Context context) {
        db = AppDatabase.getInstance(context.getApplicationContext());
        homeDao = db.homeDao();
        plexApiClient = new PlexApiClient();
    }

    // ================== قراءة من القاعدة ==================

    public List<HomeRowWithItems> loadHomeFromDb() {
        List<HomeRowWithItems> result = new ArrayList<>();

        List<HomeRowEntity> rows = homeDao.getAllRows();
        Log.d(TAG, "loadHomeFromDb: rows=" + rows.size());

        for (HomeRowEntity row : rows) {
            List<HomeItemEntity> items = homeDao.getItemsForRow(row.id);
            Log.d(TAG, "loadHomeFromDb: rowId=" + row.id + " title=" + row.title + " items=" + items.size());
            HomeRowWithItems rowWithItems = new HomeRowWithItems();
            rowWithItems.row = row;
            rowWithItems.items = items;
            result.add(rowWithItems);
        }

        return result;
    }

    // ================== مزامنة من Plex ==================

    /**
     * مزامنة كاملة للواجهة الرئيسية:
     * - لكل قسم أفلام: صف "آخر إضافات <اسم القسم>" مع 10 أفلام.
     * - لكل قسم مسلسلات: صف "آخر إضافات <اسم القسم>" مع مسلسلات فيها حلقات جديدة (بدون تكرار المسلسل).
     */
    public void syncHomeFromPlex() throws Exception {
        Log.d(TAG, "syncHomeFromPlex START");

        // نحذف القديم أولاً
        homeDao.clearItems();
        homeDao.clearRows();
        Log.d(TAG, "syncHomeFromPlex: cleared old rows/items at START");

        // 1) جلب المكتبات من Plex
        List<LibrarySection> sections = plexApiClient.getLibrarySections();
        Log.d(TAG, "syncHomeFromPlex: sections=" + (sections != null ? sections.size() : 0));

        List<LibrarySection> movieSections = new ArrayList<>();
        List<LibrarySection> showSections  = new ArrayList<>();

        if (sections != null) {
            for (LibrarySection s : sections) {
                Log.d(TAG, "section: key=" + s.getKey() + " title=" + s.getTitle() + " type=" + s.getType());
                if ("movie".equals(s.getType())) {
                    movieSections.add(s);
                } else if ("show".equals(s.getType())) {
                    showSections.add(s);
                }
            }
        }

        Log.d(TAG, "syncHomeFromPlex: movieSections=" + movieSections.size()
                + " showSections=" + showSections.size());

        int orderIndex = 0;
        int totalRows = 0;
        int totalItems = 0;

        // 2) لكل قسم أفلام: آخر إضافات + اسم القسم (10 عناصر)
        for (LibrarySection movieSection : movieSections) {
            List<LibraryItem> recent = new ArrayList<>();
            try {
                Log.d(TAG, "syncHomeFromPlex: getRecentlyAdded movies section=" + movieSection.getKey());
                recent = plexApiClient.getRecentlyAdded(movieSection.getKey(), 10);
                Log.d(TAG, "syncHomeFromPlex: movies section=" + movieSection.getKey()
                        + " recentCount=" + (recent != null ? recent.size() : 0));
            } catch (Exception e) {
                Log.e(TAG, "error getRecentlyAdded movies section=" + movieSection.getKey(), e);
            }

            if (recent == null || recent.isEmpty()) continue;

            HomeRowEntity row = new HomeRowEntity();
            // التسمية المطلوبة: "آخر إضافات أفلام انيمي" بدون شرطة
            row.title = "آخر إضافات " + movieSection.getTitle();
            row.rowType = "movies";
            row.sectionKey = movieSection.getKey();
            row.sectionTitle = movieSection.getTitle();
            row.orderIndex = orderIndex++;
            long rowId = homeDao.insertRow(row);
            Log.d(TAG, "syncHomeFromPlex: inserted movie row id=" + rowId + " title=" + row.title);
            totalRows++;

            List<HomeItemEntity> rowItems = new ArrayList<>();
            for (LibraryItem li : recent) {
                HomeItemEntity hi = new HomeItemEntity();
                hi.rowId = rowId;
                hi.ratingKey = li.getRatingKey();
                hi.itemType = "movie";
                hi.title = li.getTitle();
                hi.thumb = li.getThumb();
                hi.addedAt = 0L;
                hi.sectionTitle = movieSection.getTitle();
                hi.sectionKey = movieSection.getKey();
                rowItems.add(hi);
            }
            homeDao.insertItems(rowItems);
            totalItems += rowItems.size();
        }

// 3) لكل قسم مسلسلات: آخر إضافات + اسم القسم (مسلسلات فيها حلقات جديدة)
        for (LibrarySection showSection : showSections) {
            List<LibraryItem> showItems = new ArrayList<>();
            try {
                Log.d(TAG, "syncHomeFromPlex: getRecentlyAddedShows section=" + showSection.getKey());
                // استخدام الدالة التي ترجع مسلسلات مميزة
                showItems = plexApiClient.getRecentlyAddedShows(showSection.getKey());
                Log.d(TAG, "syncHomeFromPlex: shows section=" + showSection.getKey()
                        + " count=" + (showItems != null ? showItems.size() : 0));
            } catch (Exception e) {
                Log.e(TAG, "error getRecentlyAddedShows section=" + showSection.getKey(), e);
            }

            if (showItems == null || showItems.isEmpty()) continue;

            HomeRowEntity row = new HomeRowEntity();
            row.title = "آخر إضافات " + showSection.getTitle();
            row.rowType = "shows";
            row.sectionKey = showSection.getKey();
            row.sectionTitle = showSection.getTitle();
            row.orderIndex = orderIndex++;
            long rowId = homeDao.insertRow(row);
            Log.d(TAG, "syncHomeFromPlex: inserted show row id=" + rowId + " title=" + row.title);
            totalRows++;

            List<HomeItemEntity> rowItems = new ArrayList<>();
            for (LibraryItem showItem : showItems) {
                HomeItemEntity hi = new HomeItemEntity();
                hi.rowId = rowId;

                // ratingKey هنا هو مفتاح المسلسل لأن parseRecentlyAddedEpisodesToShows استخدم grandparentRatingKey
                hi.ratingKey = showItem.getRatingKey();
                hi.itemType  = "show";
                hi.title     = showItem.getTitle();
                hi.thumb     = showItem.getThumb();
                hi.showTitle = showItem.getTitle();
                hi.showThumb = showItem.getThumb();
                hi.addedAt   = 0L;
                hi.sectionTitle = showSection.getTitle();
                hi.sectionKey   = showSection.getKey();

                Log.d(TAG, "syncHomeFromPlex: save SHOW item title=" + hi.title
                        + " showRatingKey=" + hi.ratingKey
                        + " sectionKey=" + hi.sectionKey);

                rowItems.add(hi);
            }
            homeDao.insertItems(rowItems);
            totalItems += rowItems.size();
        }
        Log.d(TAG, "syncHomeFromPlex DONE: totalRows=" + totalRows + " totalItems=" + totalItems);
    }

    // كلاس مساعد يرجع صف مع عناصره
    public static class HomeRowWithItems {
        public HomeRowEntity row;
        public List<HomeItemEntity> items;
    }
}
