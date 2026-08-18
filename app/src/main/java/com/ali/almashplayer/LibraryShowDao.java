package com.ali.almashplayer;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LibraryShowDao {

    @Query("SELECT * FROM library_shows WHERE sectionKey = :sectionKey ORDER BY title COLLATE NOCASE ASC")
    List<LibraryShowEntity> getShowsForSection(String sectionKey);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertOrUpdateAll(List<LibraryShowEntity> entities);

    @Query("DELETE FROM library_shows WHERE sectionKey = :sectionKey")
    void clearSection(String sectionKey);
}
