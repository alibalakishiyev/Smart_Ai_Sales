package com.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.viewpager2.widget.ViewPager2;

import com.authentication.LoginActivity;
import com.smart_ai_sales.R;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;
import com.utils.BaseActivity;

import java.util.ArrayList;
import java.util.List;

public class OnboardingActivity extends BaseActivity {

    private ViewPager2 viewPager;
    private TabLayout tabLayout;
    private Button btnNext, btnGetStarted;
    private TextView tvSkip;

    private OnboardingAdapter adapter;
    private List<OnboardingItem> onboardingItems;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // BaseActivity artıq dili və temanı idarə edir
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        initViews();
        setupOnboardingItems();
        setupViewPager();
        setupTabLayout();
        setupClickListeners();

        // Cari dilə uyğun olaraq mətnləri yenilə
        updateTexts();
    }

    private void updateTexts() {
        tvSkip.setText(getString(R.string.skip));
        btnNext.setText(getString(R.string.next));
        btnGetStarted.setText(getString(R.string.get_started));
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        tabLayout = findViewById(R.id.tabLayout);
        btnNext = findViewById(R.id.btnNext);
        btnGetStarted = findViewById(R.id.btnGetStarted);
        tvSkip = findViewById(R.id.tvSkip);
    }

    private void setupOnboardingItems() {
        onboardingItems = new ArrayList<>();

        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_title_1),
                getString(R.string.onboarding_desc_1),
                R.drawable.ic_sales
        ));

        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_title_2),
                getString(R.string.onboarding_desc_2),
                R.drawable.ic_ai
        ));

        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_title_3),
                getString(R.string.onboarding_desc_3),
                R.drawable.ic_chart
        ));

        onboardingItems.add(new OnboardingItem(
                getString(R.string.onboarding_title_4),
                getString(R.string.onboarding_desc_4),
                R.drawable.ic_firebase
        ));
    }

    private void setupViewPager() {
        adapter = new OnboardingAdapter(onboardingItems);
        viewPager.setAdapter(adapter);
    }

    private void setupTabLayout() {
        new TabLayoutMediator(tabLayout, viewPager,
                (tab, position) -> {}
        ).attach();
    }

    private void setupClickListeners() {
        btnNext.setOnClickListener(v -> {
            if (viewPager.getCurrentItem() < onboardingItems.size() - 1) {
                viewPager.setCurrentItem(viewPager.getCurrentItem() + 1);
            }
        });

        tvSkip.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
            finish();
        });

        btnGetStarted.setOnClickListener(v -> {
            startActivity(new Intent(OnboardingActivity.this, LoginActivity.class));
            finish();
        });

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                if (position == onboardingItems.size() - 1) {
                    btnNext.setVisibility(View.GONE);
                    btnGetStarted.setVisibility(View.VISIBLE);
                } else {
                    btnNext.setVisibility(View.VISIBLE);
                    btnGetStarted.setVisibility(View.GONE);
                }
            }
        });
    }
}