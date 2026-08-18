package com.ali.almashplayer;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface DownloadDao {

    @Query("SELECT * FROM downloads ORDER BY id DESC")
    List<DownloadEntity> getAll();

    @Insert
    long insert(DownloadEntity entity);

    @Update
    void update(DownloadEntity entity);

    @Delete
    void delete(DownloadEntity entity);

    @Query("DELETE FROM downloads")
    void deleteAll();

    // تحميلات نشطة (جاري أو متوقفة مؤقتاً) لمراقبة التقدم
    @Query("SELECT * FROM downloads WHERE status = :downloading OR status = :paused")
    List<DownloadEntity> getActiveDownloads(int downloading, int paused);
}
