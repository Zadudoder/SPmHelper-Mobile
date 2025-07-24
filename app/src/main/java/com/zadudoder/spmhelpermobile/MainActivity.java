package com.zadudoder.spmhelpermobile;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private ViewPager2 viewPager;
    private BottomNavigationView bottomNavigation;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        viewPager = findViewById(R.id.view_pager);
        viewPager.setAdapter(new ScreenSlidePagerAdapter(this));
        viewPager.setUserInputEnabled(true);

        bottomNavigation = findViewById(R.id.bottom_navigation);

        setupViewPagerWithNavigation();
    }

    private void setupViewPagerWithNavigation() {
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == 0) {
                    bottomNavigation.setSelectedItemId(R.id.navigation_transfers);
                } else if (position == 1) {
                    bottomNavigation.setSelectedItemId(R.id.navigation_cards);
                }
            }
        });

        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_transfers) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.navigation_cards) {
                viewPager.setCurrentItem(1, true);
                return true;
            }
            return false;
        });
    }

    private static class ScreenSlidePagerAdapter extends FragmentStateAdapter {
        public ScreenSlidePagerAdapter(FragmentActivity fa) {
            super(fa);
        }

        @NonNull
        @Override
        public Fragment createFragment(int position) {
            if (position == 0) {
                return new TransfersFragment();
            }
            return new CardsFragment();
        }

        @Override
        public int getItemCount() {
            return 2;
        }
    }

    public void updateTransfersFragment() {
        Fragment fragment = getSupportFragmentManager()
                .findFragmentByTag("android:switcher:" + R.id.view_pager + ":0");

        if (fragment instanceof TransfersFragment) {
            ((TransfersFragment) fragment).updateSelectedCardInfo();
        }
    }
}