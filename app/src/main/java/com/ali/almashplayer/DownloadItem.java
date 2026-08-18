package com.ali.almashplayer;

public class DownloadItem {

    // حالات التحميل داخل التطبيق
    public static final int STATUS_QUEUED = 0;        // مضاف للقائمة ولم يبدأ بعد
    public static final int STATUS_DOWNLOADING = 1;   // جارِ التحميل
    public static final int STATUS_PAUSED = 2;        // متوقف مؤقتاً
    public static final int STATUS_COMPLETED = 3;     // اكتمل
    public static final int STATUS_FAILED = 4;        // فشل

    private long id;
    private String title;
    private String url;
    private String thumbUrl;

    private int status = STATUS_QUEUED;
    private int progress = 0;

    private long downloadId;   // غير مستخدم حالياً مع OkHttp (يمكن يبقى 0)
    private String filePath;   // المسار المحلي داخل الذاكرة

    private long totalBytes;       // الحجم الكلي للملف
    private long downloadedBytes;  // ما تم تنزيله حتى الآن

    // فلاغ الإيقاف / الإلغاء – volatile لضمان رؤيته من جميع الـ Threads
    private volatile boolean cancelled = false;

    public DownloadItem(long id, String title, String url, String thumbUrl) {
        this.id = id;
        this.title = title;
        this.url = url;
        this.thumbUrl = thumbUrl;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }

    public String getThumbUrl() {
        return thumbUrl;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public void setDownloadId(long downloadId) {
        this.downloadId = downloadId;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public long getTotalBytes() {
        return totalBytes;
    }

    public void setTotalBytes(long totalBytes) {
        this.totalBytes = totalBytes;
    }

    public long getDownloadedBytes() {
        return downloadedBytes;
    }

    public void setDownloadedBytes(long downloadedBytes) {
        this.downloadedBytes = downloadedBytes;
    }

    // تغيير رابط التحميل بعد إنشاء الكائن
    public void setUrl(String url) {
        this.url = url;
    }

    // فلاغ الإيقاف / الإلغاء
    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }
}
