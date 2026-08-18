package com.ali.almashplayer;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

public class MainPagerAdapter extends FragmentStateAdapter {

    public MainPagerAdapter(@NonNull FragmentActivity activity) {
        super(activity);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        switch (position) {

            case 0:
                return new HomeFragment();        // الرئيسية

            case 1:
                return new MoviesFragment();      // الأفلام

            case 2:
                return new SeriesFragment();       // المسلسلات

            case 3:
                return new DownloadsFragment();   // التحميلات
        }

        return new HomeFragment();
    }

    @Override
    public int getItemCount() {
        return 4;   // عدد التبويبات
    }
}
