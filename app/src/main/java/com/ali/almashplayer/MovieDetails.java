package com.ali.almashplayer;

import java.util.ArrayList;
import java.util.List;

public class MovieDetails {

    private String ratingKey;
    private String title;
    private String year;
    private long   durationMs;
    private String contentRating;
    private String tagline;
    private String summary;
    private double rating;
    private String thumb;
    private String art;

    // مفتاح الجزء (ملف الفيديو) مثل: /library/parts/26626/....mp4
    private String partKey;

    private List<String> genres = new ArrayList<>();
    private List<CastRole> castRoles = new ArrayList<>();

    // ==== getters & setters المطلوبة من PlexApiClient ====

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

    public String getYear() {
        return year;
    }

    public void setYear(String year) {
        this.year = year;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(long durationMs) {
        this.durationMs = durationMs;
    }

    public String getContentRating() {
        return contentRating;
    }

    public void setContentRating(String contentRating) {
        this.contentRating = contentRating;
    }

    public String getTagline() {
        return tagline;
    }

    public void setTagline(String tagline) {
        this.tagline = tagline;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public double getRating() {
        return rating;
    }

    public void setRating(double rating) {
        this.rating = rating;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }

    public String getArt() {
        return art;
    }

    public void setArt(String art) {
        this.art = art;
    }

    public List<String> getGenres() {
        return genres;
    }

    public void setGenres(List<String> genres) {
        this.genres = (genres != null) ? genres : new ArrayList<>();
    }

    public List<CastRole> getCastRoles() {
        return castRoles;
    }

    public void setCastRoles(List<CastRole> castRoles) {
        this.castRoles = (castRoles != null) ? castRoles : new ArrayList<>();
    }

    // partKey للفيلم
    public String getPartKey() {
        return partKey;
    }

    public void setPartKey(String partKey) {
        this.partKey = partKey;
    }
}
