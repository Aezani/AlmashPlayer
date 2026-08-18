package com.ali.almashplayer;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import com.ali.almashplayer.data.HomeRowEntity;
import com.ali.almashplayer.data.HomeItemEntity;
import com.ali.almashplayer.data.HomeDao;

@Database(
        entities = {
                DownloadEntity.class,
                ShowEntity.class,
                LibraryShowEntity.class,
                HomeRowEntity.class,
                HomeItemEntity.class
        },
        version = 3,
        exportSchema = false
)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract DownloadDao downloadDao();
    public abstract ShowDao showDao();
    public abstract LibraryShowDao libraryShowDao();

    // DAO الواجهة الرئيسية
    public abstract HomeDao homeDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "almash_db"
                            )
                            // القاعدة هنا تُستخدم ككاش، فحذفها عند تغيير السكيمة مقبول
                            .fallbackToDestructiveMigration()
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
