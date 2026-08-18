package com.ali.almashplayer;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class LiveFragment extends Fragment {

    private RecyclerView rvLiveChannels;
    private TextView tvEmpty;

    private LiveChannelAdapter adapter;
    private List<LiveChannel> channels = new ArrayList<>();

    // أساس السيرفر والبورت للبث المباشر
    private static final String LIVE_BASE_URL = "http://ms.mizzabi.com:3030";

    public LiveFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.layout_live_fragment, container, false);

        rvLiveChannels = view.findViewById(R.id.rv_live_channels);
        tvEmpty        = view.findViewById(R.id.tv_empty);

        rvLiveChannels.setLayoutManager(new LinearLayoutManager(getContext()));

        // إنشاء الـ Adapter مع مستمع الأزرار الثلاثة
        adapter = new LiveChannelAdapter(channels, new LiveChannelAdapter.OnChannelActionListener() {
            @Override
            public void onPlayInternal(LiveChannel channel) {
                playInternal(channel.getUrl());
            }

            @Override
            public void onPlayWithMx(LiveChannel channel) {
                playWithMxPlayer(channel.getUrl());
            }

            @Override
            public void onPlayWithVlc(LiveChannel channel) {
                playWithVlc(channel.getUrl());
            }
        });
        rvLiveChannels.setAdapter(adapter);

        // عند فتح التبويب يتم تحميل القنوات
        loadLiveChannels();

        return view;
    }

    /**
     * جلب قنوات البث المباشر من:
     * http://ms.mizzabi.com:3030/channels?is_live=1
     */
    private void loadLiveChannels() {
        new AsyncTask<Void, Void, List<LiveChannel>>() {

            private Exception error;

            @Override
            protected List<LiveChannel> doInBackground(Void... voids) {
                List<LiveChannel> result = new ArrayList<>();
                HttpURLConnection connection = null;
                BufferedReader reader = null;

                try {
                    String urlStr = LIVE_BASE_URL + "/channels?is_live=1";
                    URL url = new URL(urlStr);
                    connection = (HttpURLConnection) url.openConnection();
                    connection.setConnectTimeout(10000);
                    connection.setReadTimeout(15000);
                    connection.setRequestMethod("GET");

                    int code = connection.getResponseCode();
                    if (code != HttpURLConnection.HTTP_OK) {
                        throw new Exception("HTTP " + code);
                    }

                    reader = new BufferedReader(
                            new InputStreamReader(connection.getInputStream(), "UTF-8")
                    );
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }

                    String json = sb.toString();

                    // Parsing للـ JSON حسب الشكل الذي أرسلته
                    JSONObject root = new JSONObject(json);
                    JSONObject responseObj = root.optJSONObject("response");
                    if (responseObj == null) return result;

                    JSONObject dataObj = responseObj.optJSONObject("data");
                    if (dataObj == null) return result;

                    JSONArray dataArray = dataObj.optJSONArray("data");
                    if (dataArray == null) return result;

                    for (int i = 0; i < dataArray.length(); i++) {
                        JSONObject chObj = dataArray.optJSONObject(i);
                        if (chObj == null) continue;

                        String channelName = chObj.optString("channel_name", "Channel");
                        String channelInfo = chObj.optString("channel_info", "");
                        String channelUrl  = chObj.optString("channel_url", "");

                        // نتأكد أن هناك رابط
                        if (channelUrl == null || channelUrl.isEmpty()) {
                            continue;
                        }

                        // الرابط الكامل للبث: نفس السيرفر + نفس البورت + channel_url
                        String fullUrl = LIVE_BASE_URL + channelUrl;

                        LiveChannel channel = new LiveChannel(
                                channelName,
                                channelInfo,
                                fullUrl
                        );
                        result.add(channel);
                    }
                } catch (Exception e) {
                    error = e;
                } finally {
                    try {
                        if (reader != null) reader.close();
                    } catch (Exception ignore) {}
                    if (connection != null) {
                        connection.disconnect();
                    }
                }

                return result;
            }

            @Override
            protected void onPostExecute(List<LiveChannel> result) {
                if (!isAdded()) return;

                if (error != null) {
                    Toast.makeText(
                            getContext(),
                            "تعذّر تحميل قنوات البث",
                            Toast.LENGTH_LONG
                    ).show();
                }


                channels.clear();
                if (result != null) {
                    channels.addAll(result);
                }
                adapter.notifyDataSetChanged();

                // إظهار أو إخفاء رسالة "لا توجد قنوات"
                if (channels.isEmpty()) {
                    tvEmpty.setVisibility(View.VISIBLE);
                    rvLiveChannels.setVisibility(View.GONE);
                } else {
                    tvEmpty.setVisibility(View.GONE);
                    rvLiveChannels.setVisibility(View.VISIBLE);
                }
            }
        }.execute();
    }

    // ================== طرق التشغيل ==================

    private void playInternal(String url) {
        try {
            Intent i = new Intent(getContext(), InternalPlayerActivity.class);
            i.putExtra("stream_url", url);
            startActivity(i);
        } catch (Exception e) {
            Toast.makeText(getContext(),
                    "تعذّر تشغيل القناة داخلياً",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void playWithMxPlayer(String url) {
        String pkgFree = "com.mxtech.videoplayer.ad";
        String pkgPro  = "com.mxtech.videoplayer.pro";

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "application/vnd.apple.mpegurl");

        // جرّب النسخة المجانية أولاً
        intent.setPackage(pkgFree);
        if (isIntentAvailable(intent)) {
            startActivity(intent);
            return;
        }

        // جرّب النسخة Pro
        intent.setPackage(pkgPro);
        if (isIntentAvailable(intent)) {
            startActivity(intent);
            return;
        }

        // لا يوجد MX → افتح متجر Google Play على MX
        openInPlayStore(pkgFree);
    }

    private void playWithVlc(String url) {
        String pkgVlc = "org.videolan.vlc";

        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.parse(url), "application/vnd.apple.mpegurl");
        intent.setPackage(pkgVlc);

        if (isIntentAvailable(intent)) {
            startActivity(intent);
        } else {
            // لا يوجد VLC → افتح متجر Google Play على VLC
            openInPlayStore(pkgVlc);
        }
    }

    // التحقق من أن الـ Intent له تطبيق يمكنه التعامل معه
    private boolean isIntentAvailable(Intent intent) {
        if (getContext() == null) return false;
        PackageManager pm = getContext().getPackageManager();
        return pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).size() > 0;
    }

    private void openInPlayStore(String packageName) {
        if (getContext() == null) return;

        try {
            // افتح تطبيق Google Play مباشرة
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            // في حال عدم وجود Play Store (بعض الأجهزة) استخدم رابط الويب
            Intent intent = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=" + packageName));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
        }
    }
}
