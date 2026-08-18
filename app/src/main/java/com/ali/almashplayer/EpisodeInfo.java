package com.ali.almashplayer;

public class EpisodeInfo {
    private String ratingKey;
    private String parentRatingKey; // ratingKey للموسم
    private int index;              // رقم الحلقة
    private String title;
    private String summary;
    private String thumb;
    private String duration;        // نص مدة الحلقة
    private String partKey;         // رابط الـPart النسبي من Plex لتشغيل الفيديو

    public String getRatingKey() { return ratingKey; }
    public void setRatingKey(String ratingKey) { this.ratingKey = ratingKey; }

    public String getParentRatingKey() { return parentRatingKey; }
    public void setParentRatingKey(String parentRatingKey) { this.parentRatingKey = parentRatingKey; }

    public int getIndex() { return index; }
    public void setIndex(int index) { this.index = index; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getThumb() { return thumb; }
    public void setThumb(String thumb) { this.thumb = thumb; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getPartKey() { return partKey; }
    public void setPartKey(String partKey) { this.partKey = partKey; }
}
