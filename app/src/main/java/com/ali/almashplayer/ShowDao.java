package com.ali.almashplayer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface ShowDao {

    @Query("SELECT * FROM shows WHERE ratingKey = :ratingKey LIMIT 1")
    ShowEntity getShowById(String ratingKey);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdate(ShowEntity entity);
}
