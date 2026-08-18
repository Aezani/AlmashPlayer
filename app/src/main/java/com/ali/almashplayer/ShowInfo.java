package com.ali.almashplayer;

public class ShowInfo {
    private String ratingKey;
    private String title;
    private String year;
    private String summary;
    private String thumb;      // بوستر
    private String art;        // خلفية
    private String duration;   // مدة الحلقة أو إجمالي (نص جاهز للعرض)
    private int seasonCount;   // عدد المواسم إن وُجد

    public String getRatingKey() { return ratingKey; }
    public void setRatingKey(String ratingKey) { this.ratingKey = ratingKey; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getThumb() { return thumb; }
    public void setThumb(String thumb) { this.thumb = thumb; }

    public String getArt() { return art; }
    public void setArt(String art) { this.art = art; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public int getSeasonCount() { return seasonCount; }
    public void setSeasonCount(int seasonCount) { this.seasonCount = seasonCount; }
}
