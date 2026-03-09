package com.utils;


import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.main.MainActivity;
import com.smart_ai_sales.R;

import java.util.Calendar;

public class SettingsActivity extends BaseActivity {

    private CardView cardTheme, cardAbout, cardProfile, cardResetPassword, cardLogout, cardAddSalary;
    private SwitchMaterial switchDarkMode;
    private TextView tvVersion;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();

        initViews();
        setupClickListeners();
        setVersion();
        setupSalaryReminder();
    }

    private void initViews() {
        cardTheme = findViewById(R.id.cardTheme);
        cardAbout = findViewById(R.id.cardAbout);
        cardProfile = findViewById(R.id.cardProfile);
        cardResetPassword = findViewById(R.id.cardResetPassword);
        cardLogout = findViewById(R.id.cardLogout);
        cardAddSalary = findViewById(R.id.cardAddSalary);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        tvVersion = findViewById(R.id.tvVersion);

        // Switch-in vəziyyətini təyin et
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        boolean isDarkMode = prefs.getBoolean("dark_mode", true);
        switchDarkMode.setChecked(isDarkMode);
    }

    private void setupClickListeners() {
        // Theme dəyişmə
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changeTheme(isChecked);  // BaseActivity-dəki metodu çağır
        });

        cardTheme.setOnClickListener(v -> {
            switchDarkMode.setChecked(!switchDarkMode.isChecked());
        });


        cardTheme.setOnClickListener(v -> {
            switchDarkMode.setChecked(!switchDarkMode.isChecked());
        });

        cardAddSalary.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AddSalaryActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cardAbout.setOnClickListener(v -> {
            showAboutDialog();
        });

        cardProfile.setOnClickListener(v -> {
            showProfileDialog();
        });

        cardResetPassword.setOnClickListener(v -> {
            resetPassword();
        });

        cardLogout.setOnClickListener(v -> {
            showLogoutConfirmation();
        });
    }

    private void setVersion() {
        String versionName = BuildConfig.VERSION_NAME;
        tvVersion.setText("Version " + versionName);
    }

    private void showAboutDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Haqqında")
                .setMessage("Smart AI Sales v1.0.0\n\n" +
                        "Ali Balakishiyev\n\n" +
                        "Linkedin:linkedin.com/in/ali-balakishiyev\n\n" +
                        "AI ilə satış analitikası proqramı\n\n" +
                        "Allah Ruzinizi Bol elesin\n\n" +
                        "© 2026 Bütün hüquqlar qorunur.")
                .setPositiveButton("OK", null)
                .show();
    }

    private void showProfileDialog() {
        String email = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "Məlumat yoxdur";
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Mənim Məlumatlarım")
                .setMessage("Email: " + email + "\n\n" +
                        "İstifadəçi ID: " + uid + "\n\n" +
                        "Qeydiyyat tarixi: 2026")
                .setPositiveButton("OK", null)
                .show();
    }

    private void resetPassword() {
        String email = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getEmail() : "";

        new MaterialAlertDialogBuilder(this)
                .setTitle("Şifrə Sıfırlama")
                .setMessage(email + " ünvanına şifrə sıfırlama linki göndərilsin?")
                .setPositiveButton("Göndər", (dialog, which) -> {
                    mAuth.sendPasswordResetEmail(email)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Şifrə sıfırlama linki göndərildi", Toast.LENGTH_LONG).show();
                            })
                            .addOnFailureListener(e -> {
                                Toast.makeText(this, "Xəta: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                            });
                })
                .setNegativeButton("Ləğv et", null)
                .show();
    }

    private void showLogoutConfirmation() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Çıxış")
                .setMessage("Hesabdan çıxmaq istədiyinizə əminsiniz?")
                .setPositiveButton("Çıx", (dialog, which) -> {
                    logout();
                })
                .setNegativeButton("Qal", null)
                .show();
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(SettingsActivity.this, com.authentication.LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    private void setupSalaryReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, SalaryReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);

        if (calendar.getTimeInMillis() < System.currentTimeMillis()) {
            calendar.add(Calendar.MONTH, 1);
        }

        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                AlarmManager.INTERVAL_DAY * 30, pendingIntent);
    }
}