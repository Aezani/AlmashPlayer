package com.ali.almashplayer;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "library_shows")
public class LibraryShowEntity {
    @PrimaryKey
    @NonNull
    public String ratingKey;

    public String sectionKey;
    public String title;
    public String year;
    public String thumb;
    public String type; // لكي نعرف هل هو show أم episode

    // أعمدة جديدة للحلقات
    public String parentIndex;      // رقم الموسم
    public String episodeIndex;     // رقم الحلقة
    public String grandparentTitle; // اسم المسلسل الأصلي
    public String grandparentThumb; // بوستر المسلسل الأصلي

    public long updatedAt;
}