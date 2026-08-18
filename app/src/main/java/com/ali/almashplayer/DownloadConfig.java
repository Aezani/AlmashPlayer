package com.ali.almashplayer;

import android.content.Context;
import android.content.SharedPreferences;

public class DownloadConfig {

    private static final String PREF_NAME = "download_config";
    private static final String KEY_SPEED_KBPS = "download_speed_kbps";

    // cache في الذاكرة
    private static int downloadSpeedKbps = -1;

    public static void init(Context context) {
        if (downloadSpeedKbps == -1) {
            SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            downloadSpeedKbps = sp.getInt(KEY_SPEED_KBPS, 0);
        }
    }

    public static int getDownloadSpeedKbps() {
        if (downloadSpeedKbps < 0) {
            downloadSpeedKbps = 0;
        }
        return downloadSpeedKbps;
    }

    // اجعلها public لكي تُستدعى من RemoteConfigLoader
    public static void setDownloadSpeedKbps(Context context, int value) {
        downloadSpeedKbps = Math.max(0, value);
        SharedPreferences sp = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        sp.edit().putInt(KEY_SPEED_KBPS, downloadSpeedKbps).apply();
    }
}
