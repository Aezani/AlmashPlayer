package com.ali.almashplayer.data;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "home_rows")
public class HomeRowEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    // مثال: "آخر إضافات - أفلام عربية"
    public String title;

    // نوع الصف: movies / shows / episodes (اختياري للتنظيم)
    public String rowType;

    // مفتاح القسم في Plex (sectionKey) لو حبيت تربطه بالمكتبة
    public String sectionKey;

    // اسم القسم في Plex (مثلاً: "أفلام عربية")
    public String sectionTitle;

    // ترتيب الصف في الشاشة (0,1,2...)
    public int orderIndex;
}
