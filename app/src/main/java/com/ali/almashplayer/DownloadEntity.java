package com.ali.almashplayer;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "downloads")
public class DownloadEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    public String title;
    public String url;
    public String thumbUrl;

    public int status;         // نفس قيم DownloadItem.STATUS_...
    public int progress;       // 0–100

    public long downloadId;    // لم نعد نحتاجه مع OkHttp (يمكن يبقى 0)
    public String filePath;    // المسار المحلي للملف

    public long totalBytes;       // الحجم الكلي من Content-Length (إن توفّر)
    public long downloadedBytes;  // ما تم تنزيله حتى الآن
}
