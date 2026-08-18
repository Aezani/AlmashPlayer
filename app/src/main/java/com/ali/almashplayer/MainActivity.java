package com.ali.almashplayer;

import android.Manifest;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.Toast;
import android.content.Intent;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private static final int REQ_MEDIA_PERMISSIONS = 1001;
    private static final int REQ_POST_NOTIFICATIONS = 1002;

    private static final String PREFS_NAME = "almash_prefs";
    private static final String KEY_MEDIA_WARNING_SHOWN = "media_perm_warning_shown";
    private static final String KEY_NOTIF_WARNING_SHOWN = "notif_perm_warning_shown";
    private static final String KEY_FIRST_RUN = "first_run";

    // زمن المهلة بين ضغطتي زر الرجوع (2 ثانية)
    private static final int BACK_PRESS_INTERVAL = 2000;
    private long lastBackPressedTime = 0;

    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(this::onNavItemSelected);

        // إعداد التفضيلات لأول تشغيل
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        boolean firstRun = prefs.getBoolean(KEY_FIRST_RUN, true);

        // طلب أذونات الوسائط / التخزين
        if (!hasStoragePermissions()) {
            requestStoragePermissions();
        }

        // طلب إذن الإشعارات في أندرويد 13 وما بعده
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{ Manifest.permission.POST_NOTIFICATIONS },
                    REQ_POST_NOTIFICATIONS
            );
        }

        // أول تشغيل
        if (firstRun) {
            prefs.edit().putBoolean(KEY_FIRST_RUN, false).apply();
        }

        // تحميل الإعدادات من plex-config.json مرة واحدة في بداية التطبيق
        RemoteConfigLoader.loadConfigAsync(this, () -> {
            runOnUiThread(() -> {
                // هل تم فتح التطبيق من إشعار التحميل؟
                boolean openDownloads = getIntent() != null
                        && getIntent().getBooleanExtra("open_downloads", false);

                if (openDownloads) {
                    bottomNavigation.setSelectedItemId(R.id.nav_downloads);
                    loadFragment(new DownloadsFragment());
                } else {
                    // أول شاشة افتراضياً: الرئيسية
                    bottomNavigation.setSelectedItemId(R.id.nav_home);
                    loadFragment(new HomeFragment());
                }

                // تحميل قائمة التحميلات من Room إلى الذاكرة (لشاشة التحميلات)
                DownloadsRepository.loadFromDbAsync(this, null);
            });
        });
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);

        boolean openDownloads = intent != null
                && intent.getBooleanExtra("open_downloads", false);
        if (openDownloads) {
            bottomNavigation.setSelectedItemId(R.id.nav_downloads);
            loadFragment(new DownloadsFragment());
        }
    }

    // ================== إعادة تحميل الكونفيج والهوم عند الضغط على "إعادة الاتصال" ==================

    public void retryLoadConfigAndHome() {
        RemoteConfigLoader.loadConfigAsync(this, () -> {
            runOnUiThread(() -> {
                // بعد نجاح تحميل الكونفيج، نحاول تحديث الـ Home الحالي إن كان ظاهرًا
                Fragment current = getSupportFragmentManager()
                        .findFragmentById(R.id.fragment_container);
                if (current instanceof HomeFragment) {
                    ((HomeFragment) current).reloadAfterConfigReady();
                } else {
                    // إن لم يكن الـ Home ظاهرًا، نعرضه
                    bottomNavigation.setSelectedItemId(R.id.nav_home);
                    loadFragment(new HomeFragment());
                }
            });
        });
    }

    // ================== زر الرجوع مع تأكيد الخروج ==================
    @Override
    public void onBackPressed() {
        // لو في BackStack لفراغمنتات داخلية، رجّع خطوة أولاً
        if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
            getSupportFragmentManager().popBackStack();
            return;
        }

        long currentTime = System.currentTimeMillis();
        if (currentTime - lastBackPressedTime < BACK_PRESS_INTERVAL) {
            // الضغط الثاني خلال 2 ثانية → نفّذ السلوك الافتراضي ثم أغلق كل الـ Activities
            super.onBackPressed();
            finishAffinity();
        } else {
            // أول ضغط → إظهار رسالة فقط
            Toast.makeText(this,
                    "اضغط زر الرجوع مرة أخرى للخروج",
                    Toast.LENGTH_SHORT).show();
            lastBackPressedTime = currentTime;
        }
    }

    // ================== التنقل بين الصفحات ==================

    private boolean onNavItemSelected(@NonNull MenuItem item) {
        Fragment selected = null;
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            selected = new HomeFragment();
        } else if (id == R.id.nav_movies) {
            selected = new MoviesFragment();
        } else if (id == R.id.nav_series) {
            selected = new SeriesFragment();
        } else if (id == R.id.nav_downloads) {
            selected = new DownloadsFragment();
        } else if (id == R.id.nav_live) {
            // التبويب الجديد "بث مباشر"
            selected = new LiveFragment();
        }

        if (selected != null) {
            loadFragment(selected);
            return true;
        }
        return false;
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }

    // ================== أذونات التخزين / الوسائط ==================

    private boolean hasStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    == PackageManager.PERMISSION_GRANTED;
        } else {
            return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED
                    && ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED;
        }
    }

    private void requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_MEDIA_VIDEO,
                            Manifest.permission.READ_MEDIA_IMAGES
                    },
                    REQ_MEDIA_PERMISSIONS
            );
        } else {
            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE
                    },
                    REQ_MEDIA_PERMISSIONS
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        if (requestCode == REQ_MEDIA_PERMISSIONS) {
            boolean granted = true;
            for (int r : grantResults) {
                if (r != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }
            if (!granted) {
                boolean alreadyShown = prefs.getBoolean(KEY_MEDIA_WARNING_SHOWN, false);
                if (!alreadyShown) {
                    Toast.makeText(this,
                            "لن يعمل التحميل بدون منح إذن الوصول للوسائط",
                            Toast.LENGTH_LONG).show();
                    prefs.edit().putBoolean(KEY_MEDIA_WARNING_SHOWN, true).apply();
                }
            }
        } else if (requestCode == REQ_POST_NOTIFICATIONS) {
            boolean granted = grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED;
            if (!granted) {
                boolean alreadyShown = prefs.getBoolean(KEY_NOTIF_WARNING_SHOWN, false);
                if (!alreadyShown) {
                    Toast.makeText(this,
                            "لن تظهر إشعارات التحميل بدون منح إذن الإشعارات",
                            Toast.LENGTH_SHORT).show();
                    prefs.edit().putBoolean(KEY_NOTIF_WARNING_SHOWN, true).apply();
                }
            }
        }
    }
}
