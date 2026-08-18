package com.ali.almashplayer;

import android.content.Context;

/**
 * Repository للمسلسلات:
 * - يقرأ من Room (كاش).
 * - يجلب من Plex عند الحاجة ويحدّث Room.
 */
public class ShowsRepository {

    // مدة صلاحية الكاش للمسلسل (مثلاً 6 ساعات)
    private static final long CACHE_TTL_MS = 6L * 60L * 60L * 1000L;

    private final AppDatabase db;
    private final PlexApiClient plexApiClient;

    public ShowsRepository(Context context) {
        this.db = AppDatabase.getInstance(context.getApplicationContext());
        this.plexApiClient = new PlexApiClient();
    }

    /**
     * جلب ShowInfo مع كاش:
     * 1) يحاول قراءة ShowEntity من Room.
     * 2) إن وُجد وكان حديثاً، يرجعه مباشرة.
     * 3) دائماً يحاول تحديثه من الشبكة في الخلفية (يمكن استدعاؤها في Thread منفصل).
     */
    public ShowInfo getShowSync(String ratingKey) throws Exception {
        ShowEntity entity = db.showDao().getShowById(ratingKey);

        long now = System.currentTimeMillis();
        boolean needFetch = true;
        ShowInfo fromCache = null;

        if (entity != null) {
            fromCache = mapEntityToInfo(entity);
            long age = now - entity.updatedAt;
            if (age < CACHE_TTL_MS) {
                // الكاش ما زال صالحاً
                needFetch = false;
            }
        }

        if (!needFetch && fromCache != null) {
            // نرجع من الكاش دون استدعاء الشبكة
            return fromCache;
        }

        // نحتاج نجلب من Plex
        ShowInfo fromNetwork = plexApiClient.getShowDetails(ratingKey);

        if (fromNetwork != null && fromNetwork.getRatingKey() != null) {
            ShowEntity newEntity = mapInfoToEntity(fromNetwork);
            newEntity.updatedAt = now;
            db.showDao().insertOrUpdate(newEntity);
            return fromNetwork;
        }

        // لو فشل الشبكة، نرجع الكاش لو موجود
        if (fromCache != null) {
            return fromCache;
        }

        // لا يوجد كاش ولا شبكة
        throw new Exception("لا يمكن جلب بيانات المسلسل حالياً");
    }

    private ShowInfo mapEntityToInfo(ShowEntity e) {
        ShowInfo info = new ShowInfo();
        info.setRatingKey(e.ratingKey);
        info.setTitle(e.title);
        info.setYear(e.year);
        info.setSummary(e.summary);
        info.setThumb(e.thumb);
        info.setArt(e.art);
        info.setDuration(e.duration);
        info.setSeasonCount(e.seasonCount);
        return info;
    }

    private ShowEntity mapInfoToEntity(ShowInfo info) {
        ShowEntity e = new ShowEntity();
        e.ratingKey = info.getRatingKey();
        e.title = info.getTitle();
        e.year = info.getYear();
        e.summary = info.getSummary();
        e.thumb = info.getThumb();
        e.art = info.getArt();
        e.duration = info.getDuration();
        e.seasonCount = info.getSeasonCount();
        return e;
    }
}
