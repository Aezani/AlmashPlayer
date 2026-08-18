package com.ali.almashplayer;

public class CastRole {

    private String name;   // اسم الممثل
    private String role;   // اسم الشخصية
    private String thumb;  // رابط صورة الممثل من Plex

    public CastRole() {
    }

    public CastRole(String name, String role, String thumb) {
        this.name = name;
        this.role = role;
        this.thumb = thumb;
    }

    // ==== getters & setters ====

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getThumb() {
        return thumb;
    }

    public void setThumb(String thumb) {
        this.thumb = thumb;
    }
}
