package com.ali.almashplayer;

public class HomeItem {

    private String ratingKey;
    private String title;
    private String type;
    private String thumb;

    // حقل إضافي للمسلسلات إن احتجته مستقبلاً
    private String showTitle;
    private String showThumb;

    // الجديد: sectionKey للقسم (مكتبة Plex)
    private String sectionKey;

    public HomeItem(String ratingKey, String title) {
        this.ratingKey = ratingKey;
        this.title = title;
    }

    public String getRatingKey() {
        return ratingKey;
    }

    public void setRatingKey(String ratingKey) {
        this.ratingKey = ratingKey;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getShowTitle() {
        return showTitle;
    }

    public void setShowTitle(String showTitle) {
        this.showTitle = showTitle;
    }

    public String getShowThumb() {
        return showThumb;
    }

    public void setShowThumb(String showThumb) {
        this.showThumb = showThumb;
    }

    public String getSectionKey() {
        return sectionKey;
    }

    public void setSectionKey(String sectionKey) {
        this.sectionKey = sectionKey;
    }
}
