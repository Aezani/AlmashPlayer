package com.ali.almashplayer.data;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;

import java.util.List;

@Dao
public interface HomeDao {

    // حذف كل صفوف الواجهة والعناصر (مثلاً عند مزامنة كاملة)
    @Query("DELETE FROM home_items")
    void clearItems();

    @Query("DELETE FROM home_rows")
    void clearRows();

    // إدخال صفوف
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertRow(HomeRowEntity row);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertRows(List<HomeRowEntity> rows);

    // إدخال عناصر
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insertItem(HomeItemEntity item);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    List<Long> insertItems(List<HomeItemEntity> items);

    // إحضار كل الصفوف مرتبة
    @Query("SELECT * FROM home_rows ORDER BY orderIndex ASC")
    List<HomeRowEntity> getAllRows();

    // إحضار عناصر صف معيّن
    @Query("SELECT * FROM home_items WHERE rowId = :rowId ORDER BY id ASC")
    List<HomeItemEntity> getItemsForRow(long rowId);

    // عملية مزامنة كاملة بسيطة (مسح ثم إدخال)
    @Transaction
    default void replaceAll(List<HomeRowEntity> rows,
                            List<HomeItemEntity> items) {
        clearItems();
        clearRows();
        insertRows(rows);
        insertItems(items);
    }
}
