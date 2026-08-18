package com.ali.almashplayer;

public class SeasonInfo {
    private String ratingKey;
    private String parentRatingKey; // ratingKey للمسلسل
    private int index;              // رقم الموسم (1،2..)
    private String title;           // النص كما في Plex (مثلاً "Season 1" أو "موسم 1")
    private String thumb;           // صورة الموسم إن وجدت
    private int episodeCount;       // عدد الحلقات (اختياري للعرض)

    public String getRatingKey() { return ratingKey; }
    public void setRatingKey(String ratingKey) { this.ratingKey = ratingKey; }

    public String getParentRatingKey() { return parentRatingKey; }
    public void setParentRatingKey(String parentRatingKey) { this.parentRatingKey = parentRatingKey; }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getThumb() { return thumb; }
    public void setThumb(String thumb) { this.thumb = thumb; }

    public int getEpisodeCount() { return episodeCount; }
    public void setEpisodeCount(int episodeCount) { this.episodeCount = episodeCount; }
}
