package com.ali.almashplayer;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.io.File;
import java.util.List;

/**
 * تبويب / شاشة قائمة التحميلات داخل التطبيق
 */
public class DownloadsFragment extends Fragment
        implements DownloadsAdapter.OnDownloadActionListener {

    private RecyclerView recyclerDownloads;
    private DownloadsAdapter adapter;

    // نستخدم القائمة المشتركة من DownloadsRepository
    private List<DownloadItem> downloads = DownloadsRepository.DOWNLOADS;

    // مرجع ثابت للـ Adapter ليستخدمه PlexRangeDownloader في تحديث العناصر
    public static DownloadsAdapter currentAdapter;

    public DownloadsFragment() {
        // مطلوب كونستركتور فارغ
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_downloads, container, false);

        recyclerDownloads = view.findViewById(R.id.recyclerDownloads);
        recyclerDownloads.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new DownloadsAdapter(downloads, this);
        currentAdapter = adapter;
        recyclerDownloads.setAdapter(adapter);

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // تحميل من Room في خلفية، ثم تحديث الـ UI
        DownloadsRepository.loadFromDbAsync(requireContext(), () -> {
            if (adapter != null && isAdded()) {
                requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        });
    }

    // ================== أحداث الأزرار في القائمة ==================

    /**
     * استئناف التحميل (أو مواصلته) لعنصر معيّن
     * يعمل مع PlexRangeDownloader
     * وقبل البدء يجلب الكونفيج (ومنها سرعة التحميل) من السيرفر عبر RemoteConfigLoader
     */
    @Override
    public void onResumeClicked(DownloadItem item) {
        if (getContext() == null) return;

        String path = item.getFilePath();
        if (path == null || path.isEmpty()) {
            Toast.makeText(getContext(), "لا يوجد ملف للاستئناف", Toast.LENGTH_SHORT).show();
            return;
        }

        File destFile = new File(path);
        File tempFile = new File(path + ".part");

        // لا يوجد لا ملف نهائي ولا مؤقت => لا يمكن الاستئناف
        if (!destFile.exists() && !tempFile.exists()) {
            Toast.makeText(getContext(), "الملف غير موجود على الجهاز", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(getContext(), "استئناف: " + item.getTitle(), Toast.LENGTH_SHORT).show();

        // إلغاء الإيقاف وتحديث الحالة في DB والـ UI
        item.setCancelled(false);
        item.setStatus(DownloadItem.STATUS_DOWNLOADING);
        DownloadsRepository.updateDownloadAsync(getContext(), item, () -> {
            if (adapter != null && isAdded()) {
                requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        });

        // جلب الكونفيج من السيرفر (يتضمن download_speed) ثم بدء / استكمال التحميل
        if (!isAdded()) return;
        Context appCtx = requireContext().getApplicationContext();
        RemoteConfigLoader.loadConfigAsync(appCtx, () -> {
            // عند هذه النقطة DownloadConfig.setDownloadSpeedKbps(...) تم استدعاؤها داخل RemoteConfigLoader
            PlexRangeDownloader.startOrResumeDownload(appCtx, item, destFile);
        });
    }

    /**
     * إيقاف / إلغاء التحميل مؤقتاً
     * يضبط الفلاغ cancelled ويستدعي cancelCurrentCall في PlexRangeDownloader
     */
    @Override
    public void onPauseClicked(DownloadItem item) {
        if (getContext() == null) return;

        // تعيين حالة الإلغاء على العنصر
        item.setCancelled(true);
        item.setStatus(DownloadItem.STATUS_PAUSED);

        DownloadsRepository.updateDownloadAsync(getContext(), item, () -> {
            if (adapter != null && isAdded()) {
                requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        });

        // إلغاء الاتصال الشبكي الحالي (إن وجد)
        PlexRangeDownloader.cancelCurrentCall();

        Toast.makeText(getContext(), "تم إيقاف التحميل: " + item.getTitle(), Toast.LENGTH_SHORT).show();
    }

    /**
     * حذف تحميل نهائياً (من القرص + من قاعدة البيانات + من القائمة)
     */
    @Override
    public void onDeleteClicked(DownloadItem item) {
        if (getContext() == null) return;

        // إذا كان التحميل جاري، نوقفه أولاً
        if (item.getStatus() == DownloadItem.STATUS_DOWNLOADING) {
            item.setCancelled(true);
            PlexRangeDownloader.cancelCurrentCall();
        }

        DownloadsRepository.deleteDownloadAsync(getContext(), item, () -> {
            if (adapter != null && isAdded()) {
                requireActivity().runOnUiThread(() -> adapter.notifyDataSetChanged());
            }
        });

        Toast.makeText(getContext(), "تم حذف التحميل: " + item.getTitle(), Toast.LENGTH_SHORT).show();
    }

    /**
     * تشغيل الفيديو المحمَّل (بعد اكتمال التحميل)
     * هنا يمكنك لاحقاً ربط فك التشفير وتشغيل الملف داخل التطبيق
     */
    //@Override
    public void onPlayClicked(DownloadItem item) {
        if (getContext() == null) return;

        String path = item.getFilePath();
        if (path == null || path.isEmpty()) {
            Toast.makeText(getContext(), "لا يوجد ملف للتشغيل", Toast.LENGTH_SHORT).show();
            return;
        }

        File destFile = new File(path);
        if (!destFile.exists()) {
            Toast.makeText(getContext(), "الملف غير موجود على الجهاز", Toast.LENGTH_SHORT).show();
            return;
        }

        // هنا لاحقاً: فك تشفير الملف (إن كان مشفّراً) ثم تشغيله داخل التطبيق
        Toast.makeText(getContext(), "تشغيل: " + item.getTitle(), Toast.LENGTH_SHORT).show();
        // TODO: استدعِ مشغل الفيديو الخاص بك
    }
}
