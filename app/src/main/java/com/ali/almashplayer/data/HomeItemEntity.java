package com.ali.almashplayer.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "home_items")
public class HomeItemEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public long rowId;

    public String ratingKey;
    public String itemType;
    public String title;
    public String thumb;

    // عنوان المسلسل إن وُجد
    public String showTitle;
    public String showThumb;

    // وقت الإضافة (غير مستخدم حالياً لكن موجود)
    public long addedAt;

    public String sectionTitle;

    // الجديد: مفتاح القسم في Plex لتمريره إلى MovieDetailsActivity
    public String sectionKey;
}
