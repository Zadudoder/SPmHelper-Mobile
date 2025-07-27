package com.zadudoder.spmhelpermobile;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import androidx.viewpager2.widget.ViewPager2;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.zadudoder.spmhelpermobile.Fragment.CallsFragment;
import com.zadudoder.spmhelpermobile.Fragment.CardsFragment;
import com.zadudoder.spmhelpermobile.Fragment.ProfileFragment;
import com.zadudoder.spmhelpermobile.Fragment.SettingsFragment;
import com.zadudoder.spmhelpermobile.Fragment.TransfersFragment;

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
                switch (position) {
                    case 0: bottomNavigation.setSelectedItemId(R.id.navigation_profile); break;
                    case 1: bottomNavigation.setSelectedItemId(R.id.navigation_transfers); break;
                    case 2: bottomNavigation.setSelectedItemId(R.id.navigation_cards); break;
                    case 3: bottomNavigation.setSelectedItemId(R.id.navigation_calls); break;
                    case 4: bottomNavigation.setSelectedItemId(R.id.navigation_settings); break;
                }
            }
        });

        bottomNavigation.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.navigation_profile) {
                viewPager.setCurrentItem(0, true);
                return true;
            } else if (itemId == R.id.navigation_transfers) {
                viewPager.setCurrentItem(1, true);
                return true;
            } else if (itemId == R.id.navigation_cards) {
                viewPager.setCurrentItem(2, true);
                return true;
            } else if (itemId == R.id.navigation_calls) {
                viewPager.setCurrentItem(3, true);
                return true;
            } else if (itemId == R.id.navigation_settings) {
                viewPager.setCurrentItem(4, true);
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
            switch (position) {
                case 0: return new ProfileFragment();
                case 1: return new TransfersFragment();
                case 2: return new CardsFragment();
                case 3: return new CallsFragment();
                case 4: return new SettingsFragment();
                default: return new ProfileFragment();
            }
        }

        @Override
        public int getItemCount() {
            return 5; // Теперь у нас 5 фрагментов
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