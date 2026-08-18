package com.ali.almashplayer;

import android.content.Context;
import android.util.Log;

import org.json.JSONObject;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class RemoteConfigLoader {

    private static final String TAG = "RemoteConfigLoader";

    private static PlexConfig cachedConfig;

    public static void loadConfigAsync(Context context, Runnable onLoaded) {
        Log.d(TAG, "loadConfigAsync called (always fetch from server)");

        new Thread(() -> {
            try {
                String configUrl = "http://ms.mizzabi.com:88/get-config.php";
                Log.d(TAG, "fetching config from " + configUrl);

                OkHttpClient client = AlmashApplication.getInstance().getHttpClient();
                Request request = new Request.Builder()
                        .url(configUrl)
                        .header("X-APP-KEY", "831211")
                        .build();

                Response response = client.newCall(request).execute();
                Log.d(TAG, "config HTTP code=" + response.code());

                if (!response.isSuccessful() || response.body() == null) {
                    Log.e(TAG, "failed to load config, body=null or code!=200");
                    return;
                }

                String body = response.body().string();
                Log.d(TAG, "config raw body=" + body);

                JSONObject json = new JSONObject(body);

                String serverUrl   = json.getString("server_url");
                int plexPort       = json.getInt("plex_port");
                int nginxPort      = json.getInt("nginx_port");
                String token       = json.getString("token");
                int downloadSpeedK = json.optInt("download_speed", 0);

                PlexConfig config = new PlexConfig();
                config.serverUrl = serverUrl;
                config.plexPort = plexPort;
                config.nginxPort = nginxPort;
                config.token = token;
                config.downloadSpeedKbps = downloadSpeedK;

                Log.d(TAG, "parsed config: serverUrl=" + config.serverUrl
                        + ", plexPort=" + config.plexPort
                        + ", nginxPort=" + config.nginxPort
                        + ", token=" + config.token
                        + ", downloadSpeed=" + config.downloadSpeedKbps);

                // تخزين آخر كونفيج في الكاش (لو حاب تستخدمه لاحقاً)
                cachedConfig = config;

                // تهيئة Config العامة للتطبيق
                Config.applyRemoteConfig(
                        serverUrl,
                        plexPort,
                        nginxPort,
                        token
                );

                // تهيئة سرعة التحميل
                DownloadConfig.setDownloadSpeedKbps(context.getApplicationContext(), downloadSpeedK);

            } catch (Exception e) {
                Log.e(TAG, "error loading config", e);
            }

            if (onLoaded != null) {
                onLoaded.run();
            }
        }).start();
    }
    public static PlexConfig getConfig() {
        return cachedConfig;
    }
}
