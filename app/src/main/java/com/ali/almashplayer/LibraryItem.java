package com.ali.almashplayer;

public class LibraryItem {
    private String ratingKey;
    private String title;
    private String year;
    private String thumb;
    private String type;
    private String grandparentTitle;
    private String grandparentThumb;
    private String grandparentRatingKey;

    // الحقول المفقودة التي تسببت في الخطأ
    private String parentIndex; // رقم الموسم
    private String index;       // رقم الحلقة

    public LibraryItem(String ratingKey, String title, String year, String thumb, String type) {
        this.ratingKey = ratingKey; // تأكد من هذا السطر
        this.title = title;
        this.year = year;
        this.thumb = thumb;
        this.type = type;
    }

    // --- إضافة الـ Getters والـ Setters المفقودة ---
    public String getParentIndex() {
        return parentIndex;
    }
    public void setParentIndex(String parentIndex) { this.parentIndex = parentIndex; }

    public String getIndex() {
        return index;
    }
    public void setIndex(String index) { this.index = index; }

    // --- باقي الـ Getters ---
    public String getRatingKey() { return ratingKey; }
    public String getTitle() { return title; }
    public String getYear() { return year; }
    public String getThumb() { return thumb; }
    public String getType() { return type; }
    public String getGrandparentTitle() { return grandparentTitle; }
    public void setGrandparentTitle(String grandparentTitle) { this.grandparentTitle = grandparentTitle; }
    public String getGrandparentThumb() { return grandparentThumb; }
    public void setGrandparentThumb(String grandparentThumb) { this.grandparentThumb = grandparentThumb; }
    public String getGrandparentRatingKey() { return grandparentRatingKey; }
    public void setGrandparentRatingKey(String grandparentRatingKey) { this.grandparentRatingKey = grandparentRatingKey; }
}