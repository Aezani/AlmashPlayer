package com.ali.almashplayer;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "shows")
public class ShowEntity {

    @PrimaryKey
    @NonNull
    public String ratingKey;

    public String title;
    public String year;
    public String summary;
    public String thumb;
    public String art;
    public String duration;
    public int seasonCount;

    // إدارة الكاش
    public long updatedAt; // آخر مرة تم فيها التحديث من السيرفر (بالـmillis)
}
