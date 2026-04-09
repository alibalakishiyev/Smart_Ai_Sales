package com.utils;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import androidx.cardview.widget.CardView;

import com.authentication.UserProfile;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.BuildConfig;
import com.google.firebase.auth.FirebaseAuth;
import com.smart_ai_sales.R;

import java.util.Calendar;

public class SettingsActivity extends BaseActivity {

    private CardView cardTheme, cardAbout, cardProfile, cardLogout, cardAddSalary, cardLanguage, cardNotifications;
    private SwitchMaterial switchDarkMode;
    private TextView tvVersion, tvCurrentLanguage, tvNotificationStatus;
    private FirebaseAuth mAuth;
    private SharedPreferences sharedPreferences;

    // Bildiriş preference key-ləri
    private static final String PREF_NOTIFICATION_FINANCE = "notification_finance";
    private static final String PREF_NOTIFICATION_DISCOUNTS = "notification_discounts";
    private static final String PREF_NOTIFICATION_SALARY = "notification_salary";

    private static final String PREF_NOTIFICATION_CHATBOT = "notification_chatbot"; // YENİ

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        mAuth = FirebaseAuth.getInstance();
        sharedPreferences = getSharedPreferences("settings", MODE_PRIVATE);

        initViews();
        setupClickListeners();
        setVersion();
        updateCurrentLanguageDisplay();
        updateNotificationStatus();
    }

    private void initViews() {
        cardTheme = findViewById(R.id.cardTheme);
        cardAbout = findViewById(R.id.cardAbout);
        cardProfile = findViewById(R.id.cardProfile);
        cardLogout = findViewById(R.id.cardLogout);
        cardAddSalary = findViewById(R.id.cardAddSalary);
        cardLanguage = findViewById(R.id.cardLanguage);
        cardNotifications = findViewById(R.id.cardNotifications);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        tvVersion = findViewById(R.id.tvVersion);
        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage);
        tvNotificationStatus = findViewById(R.id.tvNotificationStatus);

        boolean isDarkMode = sharedPreferences.getBoolean("dark_mode", true);
        switchDarkMode.setChecked(isDarkMode);
    }

    private void setupClickListeners() {
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            changeTheme(isChecked);
        });

        cardTheme.setOnClickListener(v -> {
            switchDarkMode.setChecked(!switchDarkMode.isChecked());
        });

        cardAddSalary.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, AddSalaryActivity.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cardLanguage.setOnClickListener(v -> {
            showLanguageDialog();
        });

        cardNotifications.setOnClickListener(v -> {
            showNotificationSettingsDialog();
        });

        cardAbout.setOnClickListener(v -> {
            showAboutDialog();
        });

        cardProfile.setOnClickListener(v -> {
            Intent intent = new Intent(SettingsActivity.this, UserProfile.class);
            startActivity(intent);
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });


        cardLogout.setOnClickListener(v -> {
            showLogoutConfirmation();
        });
    }

    /**
     * Bildiriş parametrləri dialoqu
     */
    private void showNotificationSettingsDialog() {
        // Cari vəziyyətləri oxu
        boolean financeEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_FINANCE, true);
        boolean discountsEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_DISCOUNTS, true);
        boolean salaryEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_SALARY, true);
        boolean chatbotEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_CHATBOT, true); // YENİ

        // Lambda daxilində dəyişmək üçün array istifadə et
        boolean[] financeState = {financeEnabled};
        boolean[] discountsState = {discountsEnabled};
        boolean[] salaryState = {salaryEnabled};
        boolean[] chatbotState = {chatbotEnabled}; // YENİ

        String[] items = {
                getString(R.string.notification_finance),
                getString(R.string.notification_discounts),
                getString(R.string.notification_salary),
                getString(R.string.notification_chatbot)  // YENİ
        };

        boolean[] checkedItems = {financeState[0], discountsState[0], salaryState[0], chatbotState[0]};

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.notification_settings)
                .setMultiChoiceItems(items, checkedItems, (dialog, which, isChecked) -> {
                    switch (which) {
                        case 0:
                            financeState[0] = isChecked;
                            break;
                        case 1:
                            discountsState[0] = isChecked;
                            break;
                        case 2:
                            salaryState[0] = isChecked;
                            break;
                        case 3:  // YENİ
                            chatbotState[0] = isChecked;
                            break;
                    }
                })
                .setPositiveButton(R.string.save, (dialog, which) -> {
                    // Seçimləri yadda saxla
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(PREF_NOTIFICATION_FINANCE, financeState[0]);
                    editor.putBoolean(PREF_NOTIFICATION_DISCOUNTS, discountsState[0]);
                    editor.putBoolean(PREF_NOTIFICATION_SALARY, salaryState[0]);
                    editor.putBoolean(PREF_NOTIFICATION_CHATBOT, chatbotState[0]); // YENİ
                    editor.apply();

                    // Bildiriş xidmətlərini yenilə
                    updateNotificationServices();

                    // Statusu yenilə
                    updateNotificationStatus();

                    Toast.makeText(this, R.string.notification_saved, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }


    private void updateNotificationStatus() {
        boolean financeEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_FINANCE, true);
        boolean discountsEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_DISCOUNTS, true);
        boolean salaryEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_SALARY, true);
        boolean chatbotEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_CHATBOT, true); // YENİ

        int enabledCount = 0;
        if (financeEnabled) enabledCount++;
        if (discountsEnabled) enabledCount++;
        if (salaryEnabled) enabledCount++;
        if (chatbotEnabled) enabledCount++; // YENİ

        if (enabledCount == 0) {
            tvNotificationStatus.setText(R.string.notification_all_off);
            tvNotificationStatus.setTextColor(getColor(R.color.text_secondary));
        } else {
            String status = getString(R.string.notification_active_count, enabledCount);
            tvNotificationStatus.setText(status);
            tvNotificationStatus.setTextColor(getColor(R.color.income_green));
        }
    }

    // updateNotificationServices() metodu
    private void updateNotificationServices() {
        boolean financeEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_FINANCE, true);
        boolean discountsEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_DISCOUNTS, true);
        boolean salaryEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_SALARY, true);
        boolean chatbotEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_CHATBOT, true); // YENİ

        // FinanceMonitoringService
        Intent financeIntent = new Intent(this, com.serviceNotification.FinanceMonitoringService.class);
        if (financeEnabled || discountsEnabled) {
            startService(financeIntent);
        } else {
            stopService(financeIntent);
        }

        // ChatbotNotificationService - YENİ
        Intent chatbotIntent = new Intent(this, com.serviceNotification.ChatbotNotificationService.class);
        if (chatbotEnabled) {
            startService(chatbotIntent);
            Log.d("SettingsActivity", "🤖 Chatbot bildiriş xidməti başladıldı");
        } else {
            stopService(chatbotIntent);
            Log.d("SettingsActivity", "🤖 Chatbot bildiriş xidməti dayandırıldı");
        }

        // Maaş xatırlatması
        if (salaryEnabled) {
            setupSalaryReminder();
        } else {
            cancelSalaryReminder();
        }
    }


    /**
     * Maaş xatırlatmasını ləğv et
     */
    private void cancelSalaryReminder() {
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        Intent intent = new Intent(this, SalaryReminderReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
        pendingIntent.cancel();

        Log.d("SettingsActivity", "Maaş xatırlatması ləğv edildi");
    }

    /**
     * Maaş xatırlatmasını qur (yalnız aktivdirsə)
     */
    private void setupSalaryReminder() {
        boolean salaryEnabled = sharedPreferences.getBoolean(PREF_NOTIFICATION_SALARY, true);

        if (!salaryEnabled) {
            return; // Aktiv deyilsə, qurma
        }

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

        if (alarmManager != null) {
            alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(),
                    AlarmManager.INTERVAL_DAY * 30, pendingIntent);
        }

        Log.d("SettingsActivity", "Maaş xatırlatması quruldu");
    }


    private void updateCurrentLanguageDisplay() {
        String currentLang = getCurrentLanguage();
        String languageName = "";

        switch (currentLang) {
            case "az":
                languageName = "Azərbaycan";
                break;
            case "en":
                languageName = "English";
                break;
            case "ru":
                languageName = "Русский";
                break;
            default:
                languageName = "Azərbaycan";
                break;
        }

        if (tvCurrentLanguage != null) {
            tvCurrentLanguage.setText(languageName);
        }
    }

    private void showLanguageDialog() {
        String[] languages = {"Azərbaycan", "English", "Русский"};
        String[] languageCodes = {"az", "en", "ru"};

        String currentLang = getCurrentLanguage();
        int checkedItem = 0;

        switch (currentLang) {
            case "az":
                checkedItem = 0;
                break;
            case "en":
                checkedItem = 1;
                break;
            case "ru":
                checkedItem = 2;
                break;
        }

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.language)
                .setSingleChoiceItems(languages, checkedItem, (dialog, which) -> {
                    String newLang = languageCodes[which];

                    if (!newLang.equals(currentLang)) {
                        setLocale(newLang);
                        Toast.makeText(SettingsActivity.this,
                                R.string.language_changed, Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                })
                .setNegativeButton(R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
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

    @Override
    protected void onResume() {
        super.onResume();
        updateCurrentLanguageDisplay();
        updateNotificationStatus();
    }
}