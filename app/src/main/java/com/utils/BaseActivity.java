package com.utils;

import android.animation.Animator;
import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.BounceInterpolator;
import android.widget.FrameLayout;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.card.MaterialCardView;
import com.model.MultilingualChatBotDialog;
import com.smart_ai_sales.R;

import java.util.Locale;

public abstract class BaseActivity extends AppCompatActivity {

    private static final String PREF_NAME = "settings";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_DARK_MODE = "dark_mode";

    // Floating Chat Button
    private View floatingChatButton;
    private MaterialCardView chatIcon;
    private View rippleView;
    private Handler mainHandler = new Handler(Looper.getMainLooper());

    // Animasiya
    private ValueAnimator rippleAnimator;
    private ObjectAnimator rotationAnimator;

    // Statistika
    protected double totalIncome = 0;
    protected double totalExpense = 0;
    protected double monthlySalary = 0;
    protected String lastAIAnalysis = "";

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(updateBaseContextLocale(newBase));
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        loadLocale();
        loadTheme();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mainHandler.postDelayed(() -> showFloatingChatButton(), 500);
    }

    @Override
    protected void onPause() {
        super.onPause();
        hideFloatingChatButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        hideFloatingChatButton();
        cancelAllAnimations();
        mainHandler.removeCallbacksAndMessages(null);
    }

    private void loadTheme() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean(KEY_DARK_MODE, true);
        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
    }

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String languageCode = prefs.getString(KEY_LANGUAGE, "az");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    public void setLocale(String languageCode) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        if (prefs.getString(KEY_LANGUAGE, "az").equals(languageCode)) return;
        prefs.edit().putString(KEY_LANGUAGE, languageCode).apply();
        Intent intent = new Intent(this, com.main.MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    public void changeTheme(boolean isDarkMode) {
        SharedPreferences prefs = getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_DARK_MODE, isDarkMode).apply();
        AppCompatDelegate.setDefaultNightMode(isDarkMode ?
                AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        recreate();
    }

    public String getCurrentLanguage() {
        return getSharedPreferences(PREF_NAME, MODE_PRIVATE).getString(KEY_LANGUAGE, "az");
    }

    private Context updateBaseContextLocale(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREF_NAME, MODE_PRIVATE);
        String languageCode = prefs.getString(KEY_LANGUAGE, "az");
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        Configuration config = new Configuration();
        config.setLocale(locale);
        return context.createConfigurationContext(config);
    }

    private void cancelAllAnimations() {
        if (rippleAnimator != null) rippleAnimator.cancel();
        if (rotationAnimator != null) rotationAnimator.cancel();
    }

    // ==================== CHAT BUTTON ====================

    private void showFloatingChatButton() {
        try {
            hideFloatingChatButton();

            if (isFinishing() || isDestroyed()) return;

            ViewGroup decorView = (ViewGroup) getWindow().getDecorView();
            ViewGroup contentView = decorView.findViewById(android.R.id.content);
            if (contentView == null) return;

            // Layout-u inflate et
            floatingChatButton = LayoutInflater.from(this).inflate(R.layout.floating_chat_button, contentView, false);

            // View-ləri tap
            chatIcon = floatingChatButton.findViewById(R.id.chatIcon);
            rippleView = floatingChatButton.findViewById(R.id.rippleView);

            // Click listener
            if (chatIcon != null) {
                chatIcon.setOnClickListener(v -> {
                    showClickAnimation();
                    openChatBotDialog();
                });
            }

            // Layout parametrləri
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.WRAP_CONTENT,
                    FrameLayout.LayoutParams.WRAP_CONTENT
            );
            params.gravity = Gravity.BOTTOM | Gravity.END;
            params.bottomMargin = 80;
            params.rightMargin = 24;

            floatingChatButton.setLayoutParams(params);
            contentView.addView(floatingChatButton);

            // Animasiyaları başlat
            startAnimations();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Professional animasiyalar
     */
    private void startAnimations() {
        if (chatIcon == null) return;

        try {
            // 1. Pulse Animation (böyüyüb kiçilir)
            ObjectAnimator scaleX = ObjectAnimator.ofFloat(chatIcon, "scaleX", 1f, 1.08f, 1f);
            ObjectAnimator scaleY = ObjectAnimator.ofFloat(chatIcon, "scaleY", 1f, 1.08f, 1f);
            scaleX.setDuration(1000);
            scaleY.setDuration(1000);
            scaleX.setRepeatCount(ObjectAnimator.INFINITE);
            scaleY.setRepeatCount(ObjectAnimator.INFINITE);
            scaleX.setRepeatMode(ObjectAnimator.REVERSE);
            scaleY.setRepeatMode(ObjectAnimator.REVERSE);
            scaleX.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleY.setInterpolator(new AccelerateDecelerateInterpolator());
            scaleX.start();
            scaleY.start();

            // 2. Ripple Effect (dalğa)
            if (rippleView != null) {
                rippleAnimator = ValueAnimator.ofFloat(1f, 1.5f);
                rippleAnimator.setDuration(1200);
                rippleAnimator.setRepeatCount(ValueAnimator.INFINITE);
                rippleAnimator.setRepeatMode(ValueAnimator.RESTART);
                rippleAnimator.addUpdateListener(animation -> {
                    if (rippleView != null) {
                        float scale = (float) animation.getAnimatedValue();
                        rippleView.setScaleX(scale);
                        rippleView.setScaleY(scale);
                        float alpha = 0.3f * (1 - (scale - 1f) / 0.5f);
                        rippleView.setAlpha(Math.max(0, Math.min(0.3f, alpha)));
                    }
                });
                rippleAnimator.start();
            }

            // 3. Yavaş rotasiya
            rotationAnimator = ObjectAnimator.ofFloat(chatIcon, "rotation", 0f, 5f, 0f, -5f, 0f);
            rotationAnimator.setDuration(2500);
            rotationAnimator.setRepeatCount(ObjectAnimator.INFINITE);
            rotationAnimator.setRepeatMode(ObjectAnimator.RESTART);
            rotationAnimator.start();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Click animasiyası
     */
    private void showClickAnimation() {
        if (chatIcon == null) return;

        try {
            ObjectAnimator scaleDownX = ObjectAnimator.ofFloat(chatIcon, "scaleX", 1f, 0.85f);
            ObjectAnimator scaleDownY = ObjectAnimator.ofFloat(chatIcon, "scaleY", 1f, 0.85f);
            scaleDownX.setDuration(100);
            scaleDownY.setDuration(100);

            ObjectAnimator scaleUpX = ObjectAnimator.ofFloat(chatIcon, "scaleX", 0.85f, 1f);
            ObjectAnimator scaleUpY = ObjectAnimator.ofFloat(chatIcon, "scaleY", 0.85f, 1f);
            scaleUpX.setDuration(150);
            scaleUpY.setDuration(150);
            scaleUpX.setInterpolator(new BounceInterpolator());
            scaleUpY.setInterpolator(new BounceInterpolator());

            AnimatorSet clickAnim = new AnimatorSet();
            clickAnim.play(scaleDownX).with(scaleDownY);
            clickAnim.play(scaleUpX).with(scaleUpY).after(scaleDownX);
            clickAnim.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void hideFloatingChatButton() {
        cancelAllAnimations();

        if (floatingChatButton != null && floatingChatButton.getParent() != null) {
            try {
                ((ViewGroup) floatingChatButton.getParent()).removeView(floatingChatButton);
            } catch (Exception e) {
                e.printStackTrace();
            }
            floatingChatButton = null;
            chatIcon = null;
            rippleView = null;
        }
    }

    protected void openChatBotDialog() {
        try {
            if (isFinishing() || isDestroyed()) return;
            FragmentManager fm = getSupportFragmentManager();
            MultilingualChatBotDialog dialog = new MultilingualChatBotDialog(
                    getTotalIncome(), getTotalExpense(), getMonthlySalary(), getLastAIAnalysis());
            dialog.show(fm, "MultilingualChatBotDialog");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected double getTotalIncome() { return totalIncome; }
    protected double getTotalExpense() { return totalExpense; }
    protected double getMonthlySalary() { return monthlySalary; }
    protected String getLastAIAnalysis() { return lastAIAnalysis; }

    protected void updateChatbotData(double income, double expense, double salary, String analysis) {
        this.totalIncome = income;
        this.totalExpense = expense;
        this.monthlySalary = salary;
        this.lastAIAnalysis = analysis;
    }
}