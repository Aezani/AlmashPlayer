package com.ali.almashplayer;

import android.content.Context;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;

/**
 * Repository لقوائم المسلسلات في قسم معيّن:
 * - يقرأ من Room ككاش أولي لعرض سريع.
 * - يجلب دائماً من Plex حسب المود ويحدّث Room ثم يرجع النتيجة الأحدث.
 *
 * تم التعديل ليكون الترتيب الافتراضي هو الأحدث إضافة (addedAt:desc)
 */
public class ShowsListRepository {

    private static final String TAG = "ShowsListRepository";

    private final AppDatabase db;
    private final PlexApiClient plexApiClient;

    public ShowsListRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
        this.plexApiClient = new PlexApiClient();
    }

    public interface Callback {
        void onResult(List<LibraryItem> list, Exception error);
    }

    public void getShowsForSectionAsync(String sectionKey,
                                        String mode,
                                        String yearFilter,
                                        Callback callback) {
        new Thread(() -> {
            try {
                long now = System.currentTimeMillis();

                // 1) قراءة من الكاش (سيظهر الترتيب المخزن مسبقاً)
                List<LibraryShowEntity> cached = db.libraryShowDao().getShowsForSection(sectionKey);
                List<LibraryItem> cachedItems = mapEntitiesToLibraryItems(cached);

                if (callback != null && !cachedItems.isEmpty()) {
                    callback.onResult(cachedItems, null);
                }

                // 2) جلب من Plex دائماً حسب المود (مع تعديل الترتيب للأحدث)
                List<LibraryItem> fresh;

                // تم تعديل المسمى ليتوافق مع الواجهة الجديدة
                if ("المضاف حديثًا".equals(mode)) {
                    fresh = plexApiClient.getRecentlyAddedEpisodes(sectionKey);

                } else if ("السنة".equals(mode)
                        && yearFilter != null
                        && !yearFilter.isEmpty()) {

                    // تعديل: ترتيب مسلسلات السنة من الأحدث إضافة
                    String sortQuery = "?year=" + yearFilter + "&sort=addedAt:desc";
                    fresh = plexApiClient.getLibraryItems(sectionKey, sortQuery);

                } else {
                    // تعديل: جلب كل المسلسلات مرتبة من الأحدث إلى الأقدم
                    // تم تغيير titleSort:asc إلى addedAt:desc
                    fresh = plexApiClient.getLibraryItems(
                            sectionKey,
                            "?sort=addedAt:desc");
                }

                if (fresh == null) {
                    if (callback != null && cachedItems.isEmpty()) {
                        callback.onResult(null, new Exception("null result from Plex"));
                    }
                    return;
                }

                // 3) تحديث Room (مسح القديم وحفظ الترتيب الجديد)
                List<LibraryShowEntity> toStore = mapLibraryItemsToEntities(sectionKey, fresh, now);

                db.libraryShowDao().clearSection(sectionKey);
                db.libraryShowDao().insertOrUpdateAll(toStore);

                // 4) إرجاع القائمة المحدثة بالترتيب الجديد
                if (callback != null) {
                    callback.onResult(fresh, null);
                }

            } catch (Exception e) {
                Log.e(TAG, "getShowsForSectionAsync error", e);
                if (callback != null) {
                    callback.onResult(null, e);
                }
            }
        }).start();
    }

    private List<LibraryItem> mapEntitiesToLibraryItems(List<LibraryShowEntity> entities) {
        List<LibraryItem> list = new ArrayList<>();
        if (entities == null) return list;
        for (LibraryShowEntity e : entities) {
            LibraryItem item = new LibraryItem(e.ratingKey, e.title, e.year, e.thumb, e.type);
            item.setParentIndex(e.parentIndex);
            item.setIndex(e.episodeIndex);
            item.setGrandparentTitle(e.grandparentTitle);
            item.setGrandparentThumb(e.grandparentThumb);
            list.add(item);
        }
        return list;
    }

    private List<LibraryShowEntity> mapLibraryItemsToEntities(String sectionKey, List<LibraryItem> items, long updatedAt) {
        List<LibraryShowEntity> list = new ArrayList<>();
        if (items == null) return list;
        for (LibraryItem item : items) {
            LibraryShowEntity e = new LibraryShowEntity();
            e.ratingKey = item.getRatingKey();
            e.sectionKey = sectionKey;
            e.title = item.getTitle();
            e.year = item.getYear();
            e.thumb = item.getThumb();
            e.type = item.getType();

            // حفظ البيانات الجديدة
            e.parentIndex = item.getParentIndex();
            e.episodeIndex = item.getIndex();
            e.grandparentTitle = item.getGrandparentTitle();
            e.grandparentThumb = item.getGrandparentThumb();

            e.updatedAt = updatedAt;
            list.add(e);
        }
        return list;
    }
}