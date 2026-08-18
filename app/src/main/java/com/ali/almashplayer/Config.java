package com.ali.almashplayer;

public class Config {

    private static String serverUrl;
    private static int plexPort;
    private static int nginxPort;
    private static String token;

    private static boolean initialized = false;

    public static void applyRemoteConfig(String serverUrlValue,
                                         int plexPortValue,
                                         int nginxPortValue,
                                         String tokenValue) {

        if (serverUrlValue == null || serverUrlValue.isEmpty()) {
            throw new IllegalArgumentException("server_url is required from plex-config.json");
        }
        if (tokenValue == null || tokenValue.isEmpty()) {
            throw new IllegalArgumentException("token is required from plex-config.json");
        }

        serverUrl = serverUrlValue;
        plexPort = plexPortValue;
        nginxPort = nginxPortValue;
        token = tokenValue;

        initialized = true;
    }

    private static void ensureInitialized() {
        if (!initialized) {
            throw new IllegalStateException("لايمكن الاتصال بالسيرفر");
        }
    }

    // عنوان Plex المباشر (لو احتجته)
    public static String getPlexBaseUrl() {
        ensureInitialized();
        return serverUrl + ":" + plexPort;
    }

    // عنوان Nginx الذي يستخدمه التطبيق للتحميل
    public static String getNginxBaseUrl() {
        ensureInitialized();
        return serverUrl + ":" + nginxPort;
    }

    public static String getServerUrl() {
        ensureInitialized();
        return serverUrl;
    }

    public static int getPlexPort() {
        ensureInitialized();
        return plexPort;
    }

    public static int getNginxPort() {
        ensureInitialized();
        return nginxPort;
    }

    public static String getToken() {
        ensureInitialized();
        return token;
    }

    // تُستخدم في PlexApiClient لبناء path مع التوكن
    public static String withToken(String path) {
        ensureInitialized();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return path + "?X-Plex-Token=" + token;
    }

    // تُستخدم في MovieGridAdapter لإضافة التوكن إلى رابط الصورة
    public static String getTokenQuery() {
        ensureInitialized();
        return "?X-Plex-Token=" + token;
    }

    // دالة توافقية للشفرة القديمة
    public static String getBaseUrl() {
        // حالياً نعيد عنوان Plex المباشر
        return getPlexBaseUrl();
    }
}
