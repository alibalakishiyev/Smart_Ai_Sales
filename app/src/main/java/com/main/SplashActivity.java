package com.main;

import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.authentication.LoginActivity;
import com.smart_ai_sales.R;
import com.google.firebase.auth.FirebaseAuth;
import com.utils.BaseActivity;

public class SplashActivity extends BaseActivity {

    private VideoView videoView;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // Temanı yüklə (video başlamamış)
        loadTheme();

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        mAuth = FirebaseAuth.getInstance();

        videoView = findViewById(R.id.videoView);
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.main1);

        videoView.setVideoURI(videoUri);
        videoView.start();

        videoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                navigateToNextScreen();
            }
        });

        // 5 saniyə sonra avtomatik keçid (video bitməsə belə)
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (videoView.isPlaying()) {
                    videoView.stopPlayback();
                }
                navigateToNextScreen();
            }
        }, 5000);
    }

    private void loadTheme() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", true);

        if (isDarkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }

    private void navigateToNextScreen() {
        // İlk açılış yoxlaması
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean isFirstLaunch = prefs.getBoolean("isFirstLaunch", true);

        Intent intent;

        if (isFirstLaunch) {
            // İlk dəfədirsə onboarding göstər
            intent = new Intent(SplashActivity.this, OnboardingActivity.class);
            prefs.edit().putBoolean("isFirstLaunch", false).apply();
        } else {
            // İstifadəçi giriş edibsə MainActivity-ə, yoxsa LoginActivity-ə
            if (mAuth.getCurrentUser() != null) {
                intent = new Intent(SplashActivity.this, MainActivity.class);
            } else {
                intent = new Intent(SplashActivity.this, LoginActivity.class);
            }
        }

        startActivity(intent);
        finish();
    }
}