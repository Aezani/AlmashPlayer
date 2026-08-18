package com.ali.almashplayer;

import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import android.util.Log;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * كلاس بسيط للتعامل مع Plex API
 */
public class PlexApiClient {

    private final OkHttpClient client = new OkHttpClient();

    // ================== المكتبات ==================

    /**
     * جلب قائمة المكتبات
     */
    public List<LibrarySection> getLibrarySections() throws Exception {
        String url = Config.getBaseUrl() + Config.withToken("library/sections");
        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<LibrarySection> result = parseLibrarySectionsXml(inputStream);
        inputStream.close();
        return result;
    }

    private List<LibrarySection> parseLibrarySectionsXml(InputStream inputStream) throws Exception {
        List<LibrarySection> sections = new ArrayList<>();
        LibrarySection currentSection = null;

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                if ("Directory".equals(name)) {
                    String key = parser.getAttributeValue(null, "key");
                    String title = parser.getAttributeValue(null, "title");
                    String type = parser.getAttributeValue(null, "type");

                    currentSection = new LibrarySection(key, title, type);
                    sections.add(currentSection);

                } else if ("Location".equals(name) && currentSection != null) {
                    String path = parser.getAttributeValue(null, "path");
                    currentSection.addPath(path);
                }
            }

            eventType = parser.next();
        }

        return sections;
    }

    // ================== Hubs الرئيسية (للصفحة الرئيسية) ==================

    /**
     * دالة عامة لاستدعاء Hub (مثل /hubs/home/recentlyAdded?type=1)
     * وإرجاعه على شكل قائمة LibraryItem.
     */
    public List<LibraryItem> getHubItems(String hubKey) throws Exception {
        // hubKey يأتي مثل "/hubs/home/recentlyAdded?type=1"
        String cleanKey = hubKey;
        if (cleanKey.startsWith("/")) cleanKey = cleanKey.substring(1);

        String url = Config.getBaseUrl() + Config.withToken(cleanKey);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<LibraryItem> items = parseLibraryItemsXml(inputStream);
        inputStream.close();
        return items;
    }

    /**
     * أفلام مضافة حديثًا من Hubs الرئيسية
     * تكافئ /hubs/home/recentlyAdded?type=1
     */
    public List<LibraryItem> getHomeRecentlyAddedMovies() throws Exception {
        return getHubItems("/hubs/home/recentlyAdded?type=1");
    }

    /**
     * تلفزيون (مسلسلات/مواسم/حلقات) مضافة حديثًا من Hubs الرئيسية
     * تكافئ /hubs/home/recentlyAdded?type=2
     */
    public List<LibraryItem> getHomeRecentlyAddedShows() throws Exception {
        return getHubItems("/hubs/home/recentlyAdded?type=2");
    }

    // ================== عناصر المكتبة ==================

    /**
     * جلب عناصر مكتبة (أفلام/مسلسلات) من Section معيّن
     */
    public List<LibraryItem> getLibraryItems(String sectionKey, String sortQuery) throws Exception {
        String path = "library/sections/" + sectionKey + "/all";
        if (sortQuery != null && !sortQuery.isEmpty()) {
            path += sortQuery;
        }
        String url = Config.getBaseUrl() + Config.withToken(path);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<LibraryItem> items = parseLibraryItemsXml(inputStream);
        inputStream.close();
        return items;
    }

    /**
     * جلب العناصر المضافة حديثاً من مكتبة معيّنة (كل العناصر)
     */
    public List<LibraryItem> getRecentlyAdded(String sectionKey) throws Exception {
        String path = "library/sections/" + sectionKey + "/recentlyAdded";
        String url = Config.getBaseUrl() + Config.withToken(path);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<LibraryItem> items = parseLibraryItemsXml(inputStream);
        inputStream.close();
        return items;
    }

    /**
     * جلب العناصر المضافة حديثاً من مكتبة معيّنة مع حد أقصى (مثلاً 10)
     * Plex يعيد /recentlyAdded من الأحدث إلى الأقدم.
     */
    public List<LibraryItem> getRecentlyAdded(String sectionKey, int limit) throws Exception {
        List<LibraryItem> all = getRecentlyAdded(sectionKey);
        if (all == null) return new ArrayList<>();
        if (all.size() > limit) {
            return all.subList(0, limit);
        } else {
            return all;
        }
    }

    /**
     * جلب المسلسلات التي أضيفت لها حلقات حديثًا من قسم معيّن
     * النتيجة: كل مسلسل مرة واحدة فقط (من تجميع الحلقات).
     */
    public List<LibraryItem> getRecentlyAddedShows(String sectionKey) throws Exception {
        String path = "library/sections/" + sectionKey + "/recentlyAdded";
        String url = Config.getBaseUrl() + Config.withToken(path);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<LibraryItem> shows = parseRecentlyAddedEpisodesToShows(inputStream);
        inputStream.close();
        return shows;
    }

    // ===== جديد: الحلقات المضافة حديثًا من قسم مسلسلات واحد =====

    /**
     * جلب الحلقات المضافة حديثًا من قسم مسلسلات معيّن
     * كل عنصر في القائمة يمثل حلقة واحدة (type="episode").
     */
    public List<LibraryItem> getRecentlyAddedEpisodes(String sectionKey) throws Exception {
        // لاحظ إضافة type=4 في نهاية الرابط
        String path = "library/sections/" + sectionKey + "/recentlyAdded?type=4";
        String url  = Config.getBaseUrl() + Config.withToken(path);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<LibraryItem> items = parseEpisodeItemsXml(inputStream);
        inputStream.close();
        return items;
    }

    /**
     * جلب الحلقات المضافة حديثًا مع حد أقصى (مثلاً 10 من كل قسم)
     */
    public List<LibraryItem> getRecentlyAddedEpisodes(String sectionKey, int limit) throws Exception {
        List<LibraryItem> all = getRecentlyAddedEpisodes(sectionKey);
        if (all == null) return new ArrayList<>();
        if (all.size() > limit) {
            return all.subList(0, limit);
        } else {
            return all;
        }
    }

    /**
     * تحليل XML لإرجاع حلقات فقط من /recentlyAdded كـ LibraryItem
     * مع تعبئة grandparentTitle و grandparentThumb لعرض بوستر واسم المسلسل.
     */
    private List<LibraryItem> parseEpisodeItemsXml(InputStream inputStream) throws Exception {
        List<LibraryItem> items = new ArrayList<>();

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                if ("Video".equals(name)) {
                    String type = parser.getAttributeValue(null, "type");
                    if (type == null || !"episode".equals(type)) {
                        eventType = parser.next();
                        continue;
                    }

                    String ratingKey = parser.getAttributeValue(null, "ratingKey");
                    String title     = parser.getAttributeValue(null, "title");
                    String year      = parser.getAttributeValue(null, "year");
                    String thumb     = parser.getAttributeValue(null, "thumb");

                    LibraryItem item = new LibraryItem(ratingKey, title, year, thumb, "episode");

                    // بيانات المسلسل الأب (للاستخدام في HomeFragment)
                    String gpTitle = parser.getAttributeValue(null, "grandparentTitle");
                    String gpThumb = parser.getAttributeValue(null, "grandparentThumb");
                    item.setGrandparentTitle(gpTitle);
                    item.setGrandparentThumb(gpThumb);

                    items.add(item);
                }
            }

            eventType = parser.next();
        }

        return items;
    }

    // ================== البحث داخل المكتبة (باستخدام /hubs/search) ==================

    public List<LibraryItem> searchInSection(String sectionKey, String query, String wantedType) throws Exception {
        String path = "hubs/search"
                + "?query=" + java.net.URLEncoder.encode(query, "UTF-8")
                + "&sectionId=" + sectionKey;
        String url = Config.getBaseUrl() + Config.withToken(path);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<LibraryItem> items = parseHubsSearchXml(inputStream, wantedType, sectionKey);
        inputStream.close();
        return items;
    }

    public List<LibraryItem> searchInSection(String sectionKey, String query) throws Exception {
        return searchInSection(sectionKey, query, "movie");
    }

    private List<LibraryItem> parseHubsSearchXml(InputStream inputStream,
                                                 String wantedType,
                                                 String sectionKeyFilter) throws Exception {
        List<LibraryItem> items = new ArrayList<>();

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);

        boolean insideWantedHub = false;

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                if ("Hub".equals(name)) {
                    String hubType = parser.getAttributeValue(null, "type");
                    insideWantedHub = (hubType != null && hubType.equals(wantedType));

                } else if (insideWantedHub) {
                    if ("movie".equals(wantedType) && "Video".equals(name)) {
                        String librarySectionId = parser.getAttributeValue(null, "librarySectionID");
                        if (librarySectionId == null || !librarySectionId.equals(sectionKeyFilter)) {
                            eventType = parser.next();
                            continue;
                        }

                        String ratingKey = parser.getAttributeValue(null, "ratingKey");
                        String title     = parser.getAttributeValue(null, "title");
                        String year      = parser.getAttributeValue(null, "year");
                        String thumb     = parser.getAttributeValue(null, "thumb");
                        String type      = parser.getAttributeValue(null, "type");

                        LibraryItem item = new LibraryItem(ratingKey, title, year, thumb, type);
                        items.add(item);

                    } else if ("show".equals(wantedType) && "Directory".equals(name)) {
                        String librarySectionId = parser.getAttributeValue(null, "librarySectionID");
                        if (librarySectionId == null || !librarySectionId.equals(sectionKeyFilter)) {
                            eventType = parser.next();
                            continue;
                        }

                        String ratingKey = parser.getAttributeValue(null, "ratingKey");
                        String title     = parser.getAttributeValue(null, "title");
                        String year      = parser.getAttributeValue(null, "year");
                        String thumb     = parser.getAttributeValue(null, "thumb");
                        String type      = parser.getAttributeValue(null, "type");

                        LibraryItem item = new LibraryItem(ratingKey, title, year, thumb, type);
                        items.add(item);
                    }
                }

            } else if (eventType == XmlPullParser.END_TAG) {
                String name = parser.getName();
                if ("Hub".equals(name)) {
                    insideWantedHub = false;
                }
            }

            eventType = parser.next();
        }

        return items;
    }

    /**
     * تحليل XML لعناصر مكتبة Plex (أفلام/مسلسلات/حلقات عامة)
     * مع دعم حقول المسلسل للحلقات.
     */
    // --- داخل ملف PlexApiClient.java ---

    private List<LibraryItem> parseLibraryItemsXml(InputStream inputStream) throws Exception {
        List<LibraryItem> items = new ArrayList<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                if ("Video".equals(name) || "Directory".equals(name)) {
                    // 1. قراءة البيانات الأساسية
                    String type = parser.getAttributeValue(null, "type");
                    String rKey = parser.getAttributeValue(null, "ratingKey");
                    String title = parser.getAttributeValue(null, "title");
                    String year = parser.getAttributeValue(null, "year");
                    String thumb = parser.getAttributeValue(null, "thumb");

                    LibraryItem item = new LibraryItem(rKey, title, year, thumb, type);

                    // 2. إذا كانت حلقة (Episode) نقرأ الأرقام الحقيقية التي رأيناها في المتصفح
                    if ("episode".equals(type)) {
                        String sNum = parser.getAttributeValue(null, "parentIndex"); // رقم الموسم (مثل 6)
                        String eNum = parser.getAttributeValue(null, "index");       // رقم الحلقة (مثل 168)
                        String showName = parser.getAttributeValue(null, "grandparentTitle"); // اسم المسلسل (المنظمة)

                        if (sNum != null) item.setParentIndex(sNum);
                        if (eNum != null) item.setIndex(eNum);
                        if (showName != null) item.setGrandparentTitle(showName);

                        // هذا السجل سيظهر لك الأرقام الحقيقية في الـ Logcat للتأكد
                        Log.d("PlexSuccess", "تم جلب: " + showName + " - S:" + sNum + " E:" + eNum);
                    }

                    items.add(item);
                }
            }
            eventType = parser.next();
        }
        return items;
    }

    /**
     * يحوّل قائمة حلقات (Video type="episode") إلى مسلسلات مميزة
     */
    private List<LibraryItem> parseRecentlyAddedEpisodesToShows(InputStream inputStream) throws Exception {
        Map<String, LibraryItem> map = new LinkedHashMap<>();
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {
            if (eventType == XmlPullParser.START_TAG && "Video".equals(parser.getName())) {
                String type = parser.getAttributeValue(null, "type");
                if ("episode".equals(type)) {
                    String gKey = parser.getAttributeValue(null, "grandparentRatingKey");
                    String gTitle = parser.getAttributeValue(null, "grandparentTitle");
                    String gThumb = parser.getAttributeValue(null, "grandparentThumb");

                    if (gKey != null && !map.containsKey(gKey)) {
                        // نضع gKey هنا كأول معطى (ratingKey)
                        LibraryItem item = new LibraryItem(gKey, gTitle, null, gThumb, "episode");
                        item.setGrandparentRatingKey(gKey);
                        item.setGrandparentTitle(gTitle);

                        Log.d("FixCheck", "Added Show: " + gTitle + " with Key: " + gKey);
                        map.put(gKey, item);
                    }
                }
            }
            eventType = parser.next();
        }
        return new ArrayList<>(map.values());
    }

    // ================== تفاصيل فيلم واحد ==================

    public MovieDetails getMovieDetails(String ratingKey) throws Exception {
        String path = "library/metadata/" + ratingKey;
        String url  = Config.getBaseUrl() + Config.withToken(path);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        MovieDetails details = parseMovieDetailsXml(inputStream);
        inputStream.close();
        return details;
    }

    private MovieDetails parseMovieDetailsXml(InputStream inputStream) throws Exception {
        MovieDetails d = null;

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        List<String> genres = new ArrayList<>();
        List<CastRole> cast = new ArrayList<>();

        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                if ("Video".equals(name)) {
                    d = new MovieDetails();

                    d.setRatingKey(parser.getAttributeValue(null, "ratingKey"));
                    d.setTitle(parser.getAttributeValue(null, "title"));
                    d.setYear(parser.getAttributeValue(null, "year"));
                    d.setContentRating(parser.getAttributeValue(null, "contentRating"));
                    d.setTagline(parser.getAttributeValue(null, "tagline"));
                    d.setSummary(parser.getAttributeValue(null, "summary"));
                    d.setThumb(parser.getAttributeValue(null, "thumb"));
                    d.setArt(parser.getAttributeValue(null, "art"));

                    String durationMs = parser.getAttributeValue(null, "duration");
                    if (durationMs != null && !durationMs.isEmpty()) {
                        try {
                            d.setDurationMs(Long.parseLong(durationMs));
                        } catch (NumberFormatException ignored) { }
                    }

                    String ratingStr = parser.getAttributeValue(null, "rating");
                    if (ratingStr != null && !ratingStr.isEmpty()) {
                        try {
                            d.setRating(Double.parseDouble(ratingStr));
                        } catch (NumberFormatException ignored) { }
                    }

                } else if ("Genre".equals(name)) {
                    String tag = parser.getAttributeValue(null, "tag");
                    if (tag != null && !tag.isEmpty()) {
                        genres.add(tag);
                    }

                } else if ("Role".equals(name)) {
                    String actorName  = parser.getAttributeValue(null, "tag");
                    String actorRole  = parser.getAttributeValue(null, "role");
                    String actorThumb = parser.getAttributeValue(null, "thumb");

                    if (actorName != null && !actorName.isEmpty()) {
                        cast.add(new CastRole(actorName, actorRole, actorThumb));
                    }

                } else if ("Part".equals(name) && d != null) {
                    String key = parser.getAttributeValue(null, "key");
                    d.setPartKey(key);
                }
            }

            eventType = parser.next();
        }

        if (d == null) {
            return null;
        }

        d.setGenres(genres);
        d.setCastRoles(cast);
        return d;
    }

    // ================== أفلام مشابهة حسب Genre ==================

    public List<LibraryItem> getSimilarMovies(String sectionKey,
                                              String genreTag,
                                              String excludeRatingKey) throws Exception {

        String path = "library/sections/" + sectionKey + "/all"
                + "?type=1"
                + "&genre=" + java.net.URLEncoder.encode(genreTag, "UTF-8")
                + "&sort=rating:desc";

        String url = Config.getBaseUrl() + Config.withToken(path);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<LibraryItem> items = parseLibraryItemsXml(inputStream);
        inputStream.close();

        if (excludeRatingKey == null || excludeRatingKey.isEmpty()) {
            return items;
        }

        List<LibraryItem> filtered = new ArrayList<>();
        for (LibraryItem item : items) {
            if (item.getRatingKey() == null) {
                filtered.add(item);
            } else if (!excludeRatingKey.equals(item.getRatingKey())) {
                filtered.add(item);
            }
        }
        return filtered;
    }

    // ================== تفاصيل المسلسل ==================

    public ShowInfo getShowDetails(String showRatingKey) throws Exception {
        String path = "library/metadata/" + showRatingKey;
        String url = Config.getBaseUrl() + Config.withToken(path);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        ShowInfo info = parseShowDetailsXml(inputStream);
        inputStream.close();
        return info;
    }


    private ShowInfo parseShowDetailsXml(InputStream inputStream) throws Exception {
        ShowInfo info = new ShowInfo();

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                if ("Directory".equals(name) || "Video".equals(name)) {
                    String type = parser.getAttributeValue(null, "type");
                    Log.d("PlexApiClient", "parseShowDetailsXml: tag=" + name + " type=" + type);

                    // التعديل الجوهري هنا:
                    // إذا كان النوع "episode" (حلقة مضافة حديثاً)، نسحب بيانات الأب (المسلسل)
                    if ("episode".equals(type)) {
                        info.setRatingKey(parser.getAttributeValue(null, "grandparentRatingKey"));
                        info.setTitle(parser.getAttributeValue(null, "grandparentTitle"));
                        info.setThumb(parser.getAttributeValue(null, "grandparentThumb"));
                    } else {
                        // الوضع الطبيعي للمسلسل (type="show")
                        info.setRatingKey(parser.getAttributeValue(null, "ratingKey"));
                        info.setTitle(parser.getAttributeValue(null, "title"));
                        info.setThumb(parser.getAttributeValue(null, "thumb"));
                    }

                    // بقية البيانات المشتركة
                    info.setYear(parser.getAttributeValue(null, "year"));
                    info.setSummary(parser.getAttributeValue(null, "summary"));
                    info.setArt(parser.getAttributeValue(null, "art"));

                    String durationMs = parser.getAttributeValue(null, "duration");
                    if (durationMs != null && !durationMs.isEmpty()) {
                        try {
                            long ms = Long.parseLong(durationMs);
                            int minutes = (int) (ms / (1000 * 60));
                            info.setDuration(minutes + " دقيقة");
                        } catch (NumberFormatException ignored) { }
                    }

                    String childCount = parser.getAttributeValue(null, "childCount");
                    if (childCount != null && !childCount.isEmpty()) {
                        try {
                            info.setSeasonCount(Integer.parseInt(childCount));
                        } catch (NumberFormatException ignored) { }
                    }

                    // بمجرد العثور على أول تاغ صالح، نخرج من الدوران
                    if (info.getRatingKey() != null) {
                        break;
                    }
                }
            }
            eventType = parser.next();
        }

        Log.d("PlexApiClient", "parseShowDetailsXml: result ratingKey=" + info.getRatingKey()
                + " title=" + info.getTitle());
        return info;
    }

    // ================== مواسم المسلسل ==================

    public List<SeasonInfo> getShowSeasons(String showRatingKey) throws Exception {
        // 1. تنظيف الـ Key: إذا كان يحتوي على مسارات، نأخذ الرقم الأخير فقط
        String cleanKey = showRatingKey;
        if (showRatingKey != null && showRatingKey.contains("/")) {
            String[] parts = showRatingKey.split("/");
            cleanKey = parts[parts.length - 1];
        }

        // 2. بناء المسار بشكل صحيح (تأكد من وجود / واحدة فقط بين الأجزاء)
        String path = "/library/metadata/" + cleanKey + "/children";

        // 3. بناء الرابط النهائي
        String url = Config.getBaseUrl() + Config.withToken(path);

        // سجل اللوج لفحص الرابط النهائي في حالة حدوث خطأ
        android.util.Log.d("PlexApiClient", "طلب المواسم من الرابط: " + url);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .addHeader("Accept", "application/xml") // نطلب XML صراحة
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            // طباعة تفاصيل الخطأ في اللوج
            android.util.Log.e("PlexApiClient", "فشل جلب المواسم. الكود: " + response.code() + " الرابط: " + url);
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<SeasonInfo> seasons = parseSeasonsXml(inputStream);
        inputStream.close();
        response.close(); // مهم جداً إغلاق الـ response في OkHttp
        return seasons;
    }

    private List<SeasonInfo> parseSeasonsXml(InputStream inputStream) throws Exception {
        List<SeasonInfo> seasons = new ArrayList<>();

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                if ("Directory".equals(name)) {
                    String type = parser.getAttributeValue(null, "type");
                    if (type != null && !"season".equals(type)) {
                        eventType = parser.next();
                        continue;
                    }

                    String title = parser.getAttributeValue(null, "title");
                    String idxStr = parser.getAttributeValue(null, "index");
                    int idx = -1;
                    if (idxStr != null && !idxStr.isEmpty()) {
                        try { idx = Integer.parseInt(idxStr); } catch (NumberFormatException ignored) { }
                    }

                    if (title != null && title.toLowerCase().contains("all episodes")) {
                        eventType = parser.next();
                        continue;
                    }
                    if (idx == 0) {
                        eventType = parser.next();
                        continue;
                    }

                    SeasonInfo s = new SeasonInfo();
                    s.setRatingKey(parser.getAttributeValue(null, "ratingKey"));
                    s.setParentRatingKey(parser.getAttributeValue(null, "parentRatingKey"));
                    s.setTitle(title);
                    s.setThumb(parser.getAttributeValue(null, "thumb"));
                    if (idx > 0) s.setIndex(idx);

                    String leafCount = parser.getAttributeValue(null, "leafCount");
                    if (leafCount != null && !leafCount.isEmpty()) {
                        try { s.setEpisodeCount(Integer.parseInt(leafCount)); } catch (NumberFormatException ignored) { }
                    }

                    seasons.add(s);
                }
            }

            eventType = parser.next();
        }

        return seasons;
    }

    // ================== حلقات الموسم ==================

    public List<EpisodeInfo> getSeasonEpisodes(String seasonRatingKey) throws Exception {
        String path = "library/metadata/" + seasonRatingKey + "/children";
        String url = Config.getBaseUrl() + Config.withToken(path);

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Connection", "close")
                .build();

        Response response = client.newCall(request).execute();
        if (!response.isSuccessful()) {
            throw new IOException("HTTP error code: " + response.code());
        }

        InputStream inputStream = response.body().byteStream();
        List<EpisodeInfo> episodes = parseEpisodesXml(inputStream);
        inputStream.close();
        return episodes;
    }

    private List<EpisodeInfo> parseEpisodesXml(InputStream inputStream) throws Exception {
        List<EpisodeInfo> episodes = new ArrayList<>();

        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(inputStream, null);

        int eventType = parser.getEventType();
        EpisodeInfo current = null;

        while (eventType != XmlPullParser.END_DOCUMENT) {

            if (eventType == XmlPullParser.START_TAG) {
                String name = parser.getName();

                if ("Video".equals(name)) {
                    String type = parser.getAttributeValue(null, "type");
                    if (type != null && !"episode".equals(type)) {
                        eventType = parser.next();
                        continue;
                    }

                    current = new EpisodeInfo();
                    current.setRatingKey(parser.getAttributeValue(null, "ratingKey"));
                    current.setParentRatingKey(parser.getAttributeValue(null, "parentRatingKey"));
                    current.setTitle(parser.getAttributeValue(null, "title"));
                    current.setSummary(parser.getAttributeValue(null, "summary"));
                    current.setThumb(parser.getAttributeValue(null, "thumb"));

                    String idx = parser.getAttributeValue(null, "index");
                    if (idx != null && !idx.isEmpty()) {
                        try { current.setIndex(Integer.parseInt(idx)); } catch (NumberFormatException ignored) { }
                    }

                    String durationMs = parser.getAttributeValue(null, "duration");
                    if (durationMs != null && !durationMs.isEmpty()) {
                        try {
                            long ms = Long.parseLong(durationMs);
                            int minutes = (int) (ms / 1000 / 60);
                            current.setDuration(minutes + " دقيقة");
                        } catch (NumberFormatException ignored) { }
                    }

                    episodes.add(current);

                } else if ("Part".equals(name) && current != null) {
                    String key = parser.getAttributeValue(null, "key");
                    current.setPartKey(key);
                }

            }

            eventType = parser.next();
        }

        return episodes;
    }
}
