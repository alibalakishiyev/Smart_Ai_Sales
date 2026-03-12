package com.dashboard;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentManager;

import com.authentication.LoginActivity;
import com.dashboard.dialog.AIAnalysisDialog;
import com.dashboard.dialog.TransactionsDialog;
import com.data.AddDataActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.android.material.navigation.NavigationView;
import com.main.MainActivity;
import com.model.FinanceMLModel;
import com.smart_ai_sales.R;  // BURADA DOĞRU R IMPORT OLDUĞUNA ƏMİN OLUN
import com.utils.BaseActivity;
import com.utils.SettingsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class DashboardActivity extends BaseActivity {

    // Views
    private TextView tvCurrentDate, tvTotalBalance, tvMonthlySalary, tvMonthlyExpense;
    private TextView tvTodayIncome, tvTodayExpense, tvTodayNet;
    private TextView tvWeeklyIncome, tvWeeklyExpense, tvWeeklyNet;
    private TextView tvMonthlyIncome, tvMonthlyNet;
    private TextView tvAiInsights, tvUserName, tvDayOfWeek;
    private TextView tvPredictedIncome, tvPredictedExpense, tvPredictedNet;
    private TextView tvPredictionConfidence, tvModelStatus;
    private TextView tvTopCategory1, tvTopCategory1Amount, tvTopCategory1Percent;
    private TextView tvTopCategory2, tvTopCategory2Amount, tvTopCategory2Percent;
    private TextView tvTopCategory3, tvTopCategory3Amount, tvTopCategory3Percent;
    private TextView tvSavingsRate, tvExpenseRatio, tvFinancialHealth;
    private LineChart chartIncomeExpense;
    private CardView btnRefresh, cardAddData, cardMLPrediction, cardAiInsights, cardTransactions;
    private ImageView btnLogout, ivModelStatus;
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;

    private AdView mAdView;
    private InterstitialAd mInterstitialAd;
    private long lastAdShownTime = 0;
    private final long AD_INTERVAL = 60000;


    // Firebase
    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private FinanceMLModel financeModel;

    private Handler handler = new Handler();
    private Runnable refreshRunnable;
    private String userId;

    // Data Lists
    private List<Map<String, Object>> allTransactions = new ArrayList<>();
    private List<Entry> incomeEntries = new ArrayList<>();
    private List<Entry> expenseEntries = new ArrayList<>();

    // Statistics
    private double totalIncome = 0;
    private double totalExpense = 0;
    private double monthlySalary = 0;
    private double monthlyExpenses = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);

        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        if (mAuth.getCurrentUser() != null) {
            userId = mAuth.getCurrentUser().getUid();
            Log.d("FIREBASE", "User ID: " + userId);
        } else {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        initViews();
        setupClickListeners();
        updateDateTime();
        loadInterstitialAd();
        showInterstitialAd();



        initializeMLModel();
        fetchAllDataFromFirebase();
        startRealTimeUpdates();
        setupNavigationDrawer();


    }

    private void initViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
        }

        btnLogout = findViewById(R.id.btnLogout);
        btnRefresh = findViewById(R.id.btnRefresh);
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        tvUserName = findViewById(R.id.tvUserName);
        tvCurrentDate = findViewById(R.id.tvCurrentDate);
        tvDayOfWeek = findViewById(R.id.tvDayOfWeek);

        // Əsas maliyyə göstəriciləri
        tvTotalBalance = findViewById(R.id.tvTotalBalance);
        tvMonthlySalary = findViewById(R.id.tvMonthlySalary);
        tvMonthlyExpense = findViewById(R.id.tvMonthlyExpense);

        // Günlük
        tvTodayIncome = findViewById(R.id.tvTodayIncome);
        tvTodayExpense = findViewById(R.id.tvTodayExpense);
        tvTodayNet = findViewById(R.id.tvTodayNet);

        // Həftəlik
        tvWeeklyIncome = findViewById(R.id.tvWeeklyIncome);
        tvWeeklyExpense = findViewById(R.id.tvWeeklyExpense);
        tvWeeklyNet = findViewById(R.id.tvWeeklyNet);

        // Aylıq
        tvMonthlyIncome = findViewById(R.id.tvMonthlyIncome);
        tvMonthlyExpense = findViewById(R.id.tvMonthlyExpense);
        tvMonthlyNet = findViewById(R.id.tvMonthlyNet);

        // Chart
        chartIncomeExpense = findViewById(R.id.chartIncomeExpense);

        // ML Proqnoz
        cardMLPrediction = findViewById(R.id.cardMLPrediction);
        tvPredictedIncome = findViewById(R.id.tvPredictedIncome);
        tvPredictedExpense = findViewById(R.id.tvPredictedExpense);
        tvPredictedNet = findViewById(R.id.tvPredictedNet);
        tvPredictionConfidence = findViewById(R.id.tvPredictionConfidence);

        // AI Insights
        cardAiInsights = findViewById(R.id.cardAiInsights);
        tvAiInsights = findViewById(R.id.tvAiInsights);

        // Kateqoriya analizi
        tvTopCategory1 = findViewById(R.id.tvTopCategory1);
        tvTopCategory1Amount = findViewById(R.id.tvTopCategory1Amount);
        tvTopCategory1Percent = findViewById(R.id.tvTopCategory1Percent);
        tvTopCategory2 = findViewById(R.id.tvTopCategory2);
        tvTopCategory2Amount = findViewById(R.id.tvTopCategory2Amount);
        tvTopCategory2Percent = findViewById(R.id.tvTopCategory2Percent);
        tvTopCategory3 = findViewById(R.id.tvTopCategory3);
        tvTopCategory3Amount = findViewById(R.id.tvTopCategory3Amount);
        tvTopCategory3Percent = findViewById(R.id.tvTopCategory3Percent);

        // Maliyyə sağlamlığı
        tvSavingsRate = findViewById(R.id.tvSavingsRate);
        tvExpenseRatio = findViewById(R.id.tvExpenseRatio);
        tvFinancialHealth = findViewById(R.id.tvFinancialHealth);

        // Status
        tvModelStatus = findViewById(R.id.tvModelStatus);
        ivModelStatus = findViewById(R.id.ivModelStatus);

        // Kartlar
        cardAddData = findViewById(R.id.cardAddData);
        cardTransactions = findViewById(R.id.cardTransactions);

        mAdView = findViewById(R.id.adView2);
        AdRequest adRequest = new AdRequest.Builder().build();
        mAdView.loadAd(adRequest);
    }

    private void initializeMLModel() {
        try {
            financeModel = new FinanceMLModel(this);
            runOnUiThread(() -> {
                tvModelStatus.setText("AI Model aktiv");
                tvModelStatus.setTextColor(Color.parseColor("#10B981"));
                ivModelStatus.setImageResource(R.drawable.ic_circle_green);
                if (cardMLPrediction != null) {
                    cardMLPrediction.setVisibility(View.VISIBLE);
                }
            });
            Log.d("ML_MODEL", "AI Model aktiv və işləyir");
        } catch (Exception e) {
            Log.e("ML_MODEL", "Xəta baş verdi", e);
            runOnUiThread(() -> {
                tvModelStatus.setText("Sadə analiz rejimi");
                tvModelStatus.setTextColor(Color.parseColor("#F59E0B"));
                ivModelStatus.setImageResource(R.drawable.ic_circle_orange);
            });
        }
    }

    private void fetchAllDataFromFirebase() {
        Log.d("FIREBASE", "Məlumatlar çəkilir...");
        fetchTodayData();
        fetchWeeklyData();
        fetchMonthlyData();
        fetchAllTransactions();
        fetchSalaryInfo();
    }

    private void fetchTodayData() {
        long startOfDay = getStartOfDay();
        long endOfDay = getEndOfDay();

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThanOrEqualTo("date", startOfDay)
                .whereLessThanOrEqualTo("date", endOfDay)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double todayIncome = 0;
                        double todayExpense = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String type = doc.getString("type");
                            double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;

                            if ("income".equals(type)) {
                                todayIncome += amount;
                            } else if ("expense".equals(type)) {
                                todayExpense += amount;
                            }
                        }

                        double todayNet = todayIncome - todayExpense;

                        final double finalTodayIncome = todayIncome;
                        final double finalTodayExpense = todayExpense;
                        final double finalTodayNet = todayNet;

                        runOnUiThread(() -> {
                            tvTodayIncome.setText(String.format(Locale.getDefault(), "₼%.2f", finalTodayIncome));
                            tvTodayExpense.setText(String.format(Locale.getDefault(), "₼%.2f", finalTodayExpense));
                            tvTodayNet.setText(String.format(Locale.getDefault(), "₼%.2f", finalTodayNet));

                            // Rəngləri təyin et
                            tvTodayNet.setTextColor(finalTodayNet >= 0 ?
                                    Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
                        });

                        Log.d("FIREBASE", "Bugün: Gəlir=" + todayIncome + " Xərc=" + todayExpense);
                    }
                });
    }

    private void fetchWeeklyData() {
        long weekAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000);

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("date", weekAgo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double weeklyIncome = 0;
                        double weeklyExpense = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String type = doc.getString("type");
                            double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;

                            if ("income".equals(type)) {
                                weeklyIncome += amount;
                            } else if ("expense".equals(type)) {
                                weeklyExpense += amount;
                            }
                        }

                        double weeklyNet = weeklyIncome - weeklyExpense;

                        final double finalWeeklyIncome = weeklyIncome;
                        final double finalWeeklyExpense = weeklyExpense;
                        final double finalWeeklyNet = weeklyNet;

                        runOnUiThread(() -> {
                            tvWeeklyIncome.setText(String.format(Locale.getDefault(), "₼%.2f", finalWeeklyIncome));
                            tvWeeklyExpense.setText(String.format(Locale.getDefault(), "₼%.2f", finalWeeklyExpense));
                            tvWeeklyNet.setText(String.format(Locale.getDefault(), "₼%.2f", finalWeeklyNet));

                            tvWeeklyNet.setTextColor(finalWeeklyNet >= 0 ?
                                    Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
                        });
                    }
                });
    }

    private void loadInterstitialAd() {
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(this, "ca-app-pub-5367924704859976/9401961534", adRequest, new InterstitialAdLoadCallback() {
            @Override
            public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                mInterstitialAd = interstitialAd;
                Log.d("MainActivity", "Interstitial reklamı uğurla yükləndi.");
                showInterstitialAd();

                mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
                    @Override
                    public void onAdDismissedFullScreenContent() {
                        Log.d("MainActivity", "Reklam bağlandı. Yeni reklam yüklənir...");
                        mInterstitialAd = null; // Mövcud reklam obyektini null edin.
                        loadInterstitialAd();
                    }

                    @Override
                    public void onAdFailedToShowFullScreenContent(AdError adError) {
                        Log.d("MainActivity", "Reklam göstərilmədi: " + adError.getMessage());
                    }

                    @Override
                    public void onAdShowedFullScreenContent() {
                        Log.d("MainActivity", "Reklam göstərilir.");
                    }

                });

            }

            @Override
            public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                mInterstitialAd = null;
                Log.d("MainActivity", "Interstitial reklamı yüklənmədi: " + loadAdError.getMessage());
            }
        });

    }

    private void showInterstitialAd() {
        long currentTime = System.currentTimeMillis();
        if (mInterstitialAd != null && (currentTime - lastAdShownTime >= AD_INTERVAL)) {
            Log.d("MainActivity", "Reklam göstərilir...");
            mInterstitialAd.show(DashboardActivity.this);
            lastAdShownTime = currentTime; // Son reklam göstərilmə vaxtını yeniləyin
        } else if (mInterstitialAd == null) {
            Log.d("MainActivity", "Reklam hazır deyil.");
        } else {
            Log.d("MainActivity", "Reklam vaxtı tamamlanmayıb. Gözlənilir...");
        }
    }

    private void fetchMonthlyData() {
        long monthAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereGreaterThan("date", monthAgo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        monthlyExpenses = 0;
                        double monthlyIncome = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String type = doc.getString("type");
                            double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;

                            if ("income".equals(type)) {
                                monthlyIncome += amount;
                            } else if ("expense".equals(type)) {
                                monthlyExpenses += amount;
                            }
                        }

                        double monthlyNet = monthlyIncome - monthlyExpenses;

                        // FINAL dəyişənlər yaradın
                        final double finalMonthlyIncome = monthlyIncome;
                        final double finalMonthlyExpense = monthlyExpenses;
                        final double finalMonthlyNet = monthlyNet;

                        runOnUiThread(() -> {
                            tvMonthlyIncome.setText(String.format(Locale.getDefault(), "₼%.2f", finalMonthlyIncome));
                            tvMonthlyExpense.setText(String.format(Locale.getDefault(), "₼%.2f", finalMonthlyExpense));
                            tvMonthlyNet.setText(String.format(Locale.getDefault(), "₼%.2f", finalMonthlyNet));

                            tvMonthlyNet.setTextColor(finalMonthlyNet >= 0 ?
                                    Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

                            // Ümumi balansı yenilə
                            updateTotalBalance();

                            // Maliyyə sağlamlığını hesabla - FINAL dəyişənləri istifadə edin
                            calculateFinancialHealth(finalMonthlyIncome, finalMonthlyExpense);
                        });
                    }
                });
    }

    private void fetchSalaryInfo() {
        // Son 3 ayın maaşlarını hesabla (income type = salary)
        long threeMonthsAgo = System.currentTimeMillis() - (90L * 24 * 60 * 60 * 1000);

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .whereEqualTo("type", "income")
                .whereGreaterThan("date", threeMonthsAgo)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        double totalSalary = 0;
                        int salaryCount = 0;

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            String category = doc.getString("category");
                            double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;

                            // Maaş kateqoriyasını yoxla
                            if ("Maaş".equals(category) || "Salary".equals(category) ||
                                    "Əmək haqqı".equals(category)) {
                                totalSalary += amount;
                                salaryCount++;
                            }
                        }

                        monthlySalary = salaryCount > 0 ? totalSalary / salaryCount : 0;

                        final double finalMonthlySalary = monthlySalary;

                        runOnUiThread(() -> {
                            tvMonthlySalary.setText(String.format(Locale.getDefault(), "₼%.2f", finalMonthlySalary));
                            tvMonthlyExpense.setText(String.format(Locale.getDefault(), "₼%.2f", monthlyExpenses));

                            // Qənaət dərəcəsini hesabla
                            if (finalMonthlySalary > 0) {
                                double savingsRate = ((finalMonthlySalary - monthlyExpenses) / finalMonthlySalary) * 100;
                                tvSavingsRate.setText(String.format(Locale.getDefault(), "%.1f%%", savingsRate));

                                // Xərc nisbəti
                                double expenseRatio = (monthlyExpenses / finalMonthlySalary) * 100;
                                tvExpenseRatio.setText(String.format(Locale.getDefault(), "%.1f%%", expenseRatio));
                            }
                        });
                    }
                });
    }

    private void fetchAllTransactions() {
        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .orderBy("date", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        allTransactions.clear();
                        incomeEntries.clear();
                        expenseEntries.clear();

                        totalIncome = 0;
                        totalExpense = 0;

                        Map<Long, Double> dailyIncome = new TreeMap<>();
                        Map<Long, Double> dailyExpense = new TreeMap<>();
                        Map<String, Double> categoryTotals = new HashMap<>();

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Map<String, Object> transaction = new HashMap<>();
                            String type = doc.getString("type");
                            String category = doc.getString("category");
                            double amount = doc.getDouble("amount") != null ? doc.getDouble("amount") : 0;
                            long date = doc.getLong("date");
                            String note = doc.getString("note");

                            transaction.put("type", type);
                            transaction.put("category", category);
                            transaction.put("amount", amount);
                            transaction.put("date", date);
                            transaction.put("note", note);
                            transaction.put("id", doc.getId());

                            allTransactions.add(transaction);

                            long dayStart = getStartOfDay(date);

                            if ("income".equals(type)) {
                                totalIncome += amount;
                                dailyIncome.put(dayStart, dailyIncome.getOrDefault(dayStart, 0.0) + amount);
                            } else if ("expense".equals(type)) {
                                totalExpense += amount;
                                dailyExpense.put(dayStart, dailyExpense.getOrDefault(dayStart, 0.0) + amount);

                                // Kateqoriya analizi üçün
                                if (category != null) {
                                    categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
                                }
                            }
                        }

                        // Chart üçün məlumatları hazırla
                        List<Long> allDays = new ArrayList<>(dailyIncome.keySet());
                        allDays.addAll(dailyExpense.keySet());
                        Map<Long, Object> tempMap = new TreeMap<>();
                        for (Long day : allDays) {
                            tempMap.put(day, null);
                        }
                        allDays = new ArrayList<>(tempMap.keySet());

                        int index = 0;
                        for (Long day : allDays) {
                            incomeEntries.add(new Entry(index, dailyIncome.getOrDefault(day, 0.0).floatValue()));
                            expenseEntries.add(new Entry(index, dailyExpense.getOrDefault(day, 0.0).floatValue()));
                            index++;
                        }

                        // Kateqoriya analizini göstər
                        showCategoryAnalysis(categoryTotals);

                        // Chart-ı yenilə
                        runOnUiThread(() -> {
                            updateChart();
                            updateTotalBalance();
                        });

                        // ML analizini işə sal
                        runMLAnalysis();

                        Log.d("FIREBASE", "Cəmi " + allTransactions.size() + " əməliyyat yükləndi");

                    } else {
                        Log.e("FIREBASE", "Bütün məlumatlar alına bilmədi", task.getException());
                    }
                });
    }

    private void showCategoryAnalysis(Map<String, Double> categoryTotals) {
        if (categoryTotals.isEmpty()) {
            return;
        }

        List<Map.Entry<String, Double>> sortedCategories = new ArrayList<>(categoryTotals.entrySet());
        sortedCategories.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        double totalExpense = 0;
        for (double value : categoryTotals.values()) {
            totalExpense += value;
        }

        for (int i = 0; i < Math.min(3, sortedCategories.size()); i++) {
            Map.Entry<String, Double> entry = sortedCategories.get(i);
            int percent = totalExpense > 0 ? (int) ((entry.getValue() / totalExpense) * 100) : 0;

            switch (i) {
                case 0:
                    tvTopCategory1.setText(entry.getKey());
                    tvTopCategory1Amount.setText(String.format("₼%.2f", entry.getValue()));
                    tvTopCategory1Percent.setText(percent + "%");
                    break;
                case 1:
                    tvTopCategory2.setText(entry.getKey());
                    tvTopCategory2Amount.setText(String.format("₼%.2f", entry.getValue()));
                    tvTopCategory2Percent.setText(percent + "%");
                    break;
                case 2:
                    tvTopCategory3.setText(entry.getKey());
                    tvTopCategory3Amount.setText(String.format("₼%.2f", entry.getValue()));
                    tvTopCategory3Percent.setText(percent + "%");
                    break;
            }
        }
    }

    private void calculateFinancialHealth(double monthlyIncome, double monthlyExpenses) {
        if (monthlyIncome <= 0) {
            tvFinancialHealth.setText("Məlumat az");
            tvFinancialHealth.setTextColor(Color.parseColor("#F59E0B"));
            return;
        }

        double savingsRate = ((monthlyIncome - monthlyExpenses) / monthlyIncome) * 100;
        String healthText;
        int healthColor;

        if (savingsRate >= 20) {
            healthText = "🌟 Əla";
            healthColor = Color.parseColor("#10B981");
        } else if (savingsRate >= 10) {
            healthText = "👍 Yaxşı";
            healthColor = Color.parseColor("#3B82F6");
        } else if (savingsRate >= 0) {
            healthText = "⚠️ Orta";
            healthColor = Color.parseColor("#F59E0B");
        } else {
            healthText = "🔴 Zəif (Zərər)";
            healthColor = Color.parseColor("#EF4444");
        }

        tvFinancialHealth.setText(healthText);
        tvFinancialHealth.setTextColor(healthColor);
        tvSavingsRate.setText(String.format(Locale.getDefault(), "%.1f%%", savingsRate));
    }

    private void updateTotalBalance() {
        double balance = totalIncome - totalExpense;
        tvTotalBalance.setText(String.format(Locale.getDefault(), "₼%.2f", balance));
        tvTotalBalance.setTextColor(balance >= 0 ?
                Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
    }

    private void runMLAnalysis() {
        if (financeModel == null || !financeModel.isInitialized() || allTransactions.size() < 5) {
            showSimpleAnalysis();
            return;
        }

        try {
            // Son 30 günlük məlumatları hazırla
            float[][] historicalData = new float[30][2]; // [gəlir, xərc]

            long now = System.currentTimeMillis();
            Calendar cal = Calendar.getInstance();

            for (int i = 0; i < 30; i++) {
                long dayStart = getStartOfDay(now - (i * 24 * 60 * 60 * 1000L));
                long dayEnd = getEndOfDay(now - (i * 24 * 60 * 60 * 1000L));

                double dayIncome = 0, dayExpense = 0;

                for (Map<String, Object> t : allTransactions) {
                    Long date = (Long) t.get("date");
                    if (date != null && date >= dayStart && date <= dayEnd) {
                        String type = (String) t.get("type");
                        Double amount = (Double) t.get("amount");

                        if ("income".equals(type) && amount != null) {
                            dayIncome += amount;
                        } else if ("expense".equals(type) && amount != null) {
                            dayExpense += amount;
                        }
                    }
                }

                historicalData[29-i][0] = (float) dayIncome;
                historicalData[29-i][1] = (float) dayExpense;
            }

            float[] prediction = financeModel.predictNextDay(historicalData);

            runOnUiThread(() -> {
                if (prediction != null && prediction.length >= 3) {
                    tvPredictedIncome.setText(String.format(Locale.getDefault(), "₼%.2f", prediction[0]));
                    tvPredictedExpense.setText(String.format(Locale.getDefault(), "₼%.2f", prediction[1]));

                    double predictedNet = prediction[0] - prediction[1];
                    tvPredictedNet.setText(String.format(Locale.getDefault(), "₼%.2f", predictedNet));
                    tvPredictedNet.setTextColor(predictedNet >= 0 ?
                            Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

                    // Güvən səviyyəsi (məlumat miqdarına görə)
                    int confidence = Math.min(90, 60 + (allTransactions.size() / 2));
                    tvPredictionConfidence.setText(confidence + "%");

                    if (cardMLPrediction != null) {
                        cardMLPrediction.setVisibility(View.VISIBLE);
                    }
                }

                showAIInsights();
            });

        } catch (Exception e) {
            Log.e("ML_MODEL", "Analiz xətası", e);
            showSimpleAnalysis();
        }
    }

    private void showSimpleAnalysis() {
        runOnUiThread(() -> {
            StringBuilder insights = new StringBuilder();
            insights.append("📊 MALİYYƏ ANALİZİ\n");
            insights.append("────────────────────\n\n");

            insights.append(String.format("💵 Ümumi Gəlir: ₼%.2f\n", totalIncome));
            insights.append(String.format("💸 Ümumi Xərc: ₼%.2f\n", totalExpense));
            insights.append(String.format("💰 Xalis Mənfəət: ₼%.2f\n\n", (totalIncome - totalExpense)));

            if (monthlySalary > 0) {
                double savingsRate = ((monthlySalary - monthlyExpenses) / monthlySalary) * 100;
                insights.append(String.format("📈 Aylıq Qənaət: %.1f%%\n", savingsRate));

                if (savingsRate < 10) {
                    insights.append("⚠️ Tövsiyə: Xərcləri azaldın\n");
                } else if (savingsRate > 20) {
                    insights.append("🎉 Əla qənaət nisbəti!\n");
                }
            }

            tvAiInsights.setText(insights.toString());
        });
    }

    private void showAIInsights() {
        StringBuilder insights = new StringBuilder();
        insights.append("🤖 AI MALİYYƏ ANALİZİ\n");
        insights.append("══════════════════════\n\n");

        insights.append(String.format("📊 Ümumi vəziyyət:\n"));
        insights.append(String.format("   • Gəlir: ₼%.2f\n", totalIncome));
        insights.append(String.format("   • Xərc: ₼%.2f\n", totalExpense));
        insights.append(String.format("   • Balans: ₼%.2f\n\n", (totalIncome - totalExpense)));

        if (monthlySalary > 0) {
            double savingsRate = ((monthlySalary - monthlyExpenses) / monthlySalary) * 100;
            insights.append(String.format("💰 Maaş analizi:\n"));
            insights.append(String.format("   • Orta maaş: ₼%.2f\n", monthlySalary));
            insights.append(String.format("   • Aylıq xərc: ₼%.2f\n", monthlyExpenses));
            insights.append(String.format("   • Qənaət: %.1f%%\n\n", savingsRate));

            // Tövsiyələr
            insights.append("💡 TÖVSİYƏLƏR:\n");

            if (savingsRate < 10) {
                insights.append("   • ⚠️ Xərclərinizi azaldın\n");
                insights.append("   • 📝 Büdcə planlaması edin\n");
            } else if (savingsRate < 20) {
                insights.append("   • 👍 Yaxşı, daha da yaxşılaşdıra bilərsiniz\n");
                insights.append("   • 💰 İnvestisiya düşünün\n");
            } else {
                insights.append("   • 🎉 Mükəmməl maliyyə vəziyyəti\n");
                insights.append("   • 📈 İnvestisiya etmək üçün ideal vaxt\n");
            }

            // Proqnozlar
            if (tvPredictedIncome.getText() != null && !tvPredictedIncome.getText().equals("₼0.00")) {
                insights.append("\n🔮 SABAH PROQNOZU:\n");
                insights.append("   • " + tvPredictedIncome.getText() + " gəlir\n");
                insights.append("   • " + tvPredictedExpense.getText() + " xərc\n");
            }
        }

        tvAiInsights.setText(insights.toString());
    }

    private void updateChart() {
        if (incomeEntries.isEmpty() && expenseEntries.isEmpty()) {
            // Nümunə məlumatlar
            for (int i = 0; i < 7; i++) {
                incomeEntries.add(new Entry(i, (float) (Math.random() * 1000 + 500)));
                expenseEntries.add(new Entry(i, (float) (Math.random() * 800 + 200)));
            }
        }

        LineDataSet incomeDataSet = new LineDataSet(incomeEntries, "Gəlir");
        incomeDataSet.setColor(Color.parseColor("#10B981"));
        incomeDataSet.setCircleColor(Color.parseColor("#10B981"));
        incomeDataSet.setLineWidth(2f);
        incomeDataSet.setCircleRadius(3f);
        incomeDataSet.setDrawFilled(true);
        incomeDataSet.setFillColor(Color.parseColor("#10B981"));
        incomeDataSet.setFillAlpha(50);
        incomeDataSet.setValueTextColor(Color.WHITE);
        incomeDataSet.setValueTextSize(10f);
        incomeDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineDataSet expenseDataSet = new LineDataSet(expenseEntries, "Xərc");
        expenseDataSet.setColor(Color.parseColor("#EF4444"));
        expenseDataSet.setCircleColor(Color.parseColor("#EF4444"));
        expenseDataSet.setLineWidth(2f);
        expenseDataSet.setCircleRadius(3f);
        expenseDataSet.setDrawFilled(true);
        expenseDataSet.setFillColor(Color.parseColor("#EF4444"));
        expenseDataSet.setFillAlpha(50);
        expenseDataSet.setValueTextColor(Color.WHITE);
        expenseDataSet.setValueTextSize(10f);
        expenseDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        ILineDataSet[] dataSetsArray = new ILineDataSet[]{incomeDataSet, expenseDataSet};
        LineData lineData = new LineData(dataSetsArray);

        chartIncomeExpense.setData(lineData);
        chartIncomeExpense.getDescription().setEnabled(false);
        chartIncomeExpense.getXAxis().setDrawLabels(false);
        chartIncomeExpense.getAxisLeft().setTextColor(Color.WHITE);
        chartIncomeExpense.getAxisRight().setTextColor(Color.WHITE);
        chartIncomeExpense.getLegend().setTextColor(Color.WHITE);
        chartIncomeExpense.animateX(1000);
        chartIncomeExpense.invalidate();
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        SimpleDateFormat dayFormat = new SimpleDateFormat("EEEE", Locale.getDefault());
        tvCurrentDate.setText(sdf.format(new Date()));

        String day = dayFormat.format(new Date());
        String azDay = getAzDay(day);
        tvDayOfWeek.setText(azDay);
    }

    private String getAzDay(String engDay) {
        switch(engDay) {
            case "Monday": return "Bazar ertəsi";
            case "Tuesday": return "Çərşənbə axşamı";
            case "Wednesday": return "Çərşənbə";
            case "Thursday": return "Cümə axşamı";
            case "Friday": return "Cümə";
            case "Saturday": return "Şənbə";
            case "Sunday": return "Bazar";
            default: return engDay;
        }
    }

    private void setupClickListeners() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnLogout.setOnClickListener(v -> logout());

        btnRefresh.setOnClickListener(v -> {
            fetchAllDataFromFirebase();
            Toast.makeText(DashboardActivity.this, "Məlumatlar yenilənir...", Toast.LENGTH_SHORT).show();
        });

        if (cardAddData != null) {
            cardAddData.setOnClickListener(v -> {
                Intent intent = new Intent(DashboardActivity.this, AddDataActivity.class);
                startActivity(intent);
            });
        }

        if (cardAiInsights != null) {
            cardAiInsights.setOnClickListener(v -> showAIAnalysisDialog());
        }

        if (cardTransactions != null) {
            cardTransactions.setOnClickListener(v -> showAllTransactionsDialog());
        }
    }

    private void setupNavigationDrawer() {
        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_dashboard) {
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_reports) {
                showAllTransactionsDialog();
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_sales) {
                showAIAnalysisDialog();
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_settings) {
                startActivity(new Intent(DashboardActivity.this, SettingsActivity.class));
                drawerLayout.closeDrawer(GravityCompat.START);
            } else if (id == R.id.nav_logout) {
                logout();
                drawerLayout.closeDrawer(GravityCompat.START);
            }

            return true;
        });

        View headerView = navigationView.getHeaderView(0);
        TextView navUserName = headerView.findViewById(R.id.navUserName);
        TextView navUserEmail = headerView.findViewById(R.id.navUserEmail);

        if (mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            String userName = email != null ? email.split("@")[0] : "İstifadəçi";
            navUserName.setText(userName);
            navUserEmail.setText(email != null ? email : "email@example.com");
            tvUserName.setText(userName);
        }
    }

    private void showAllTransactionsDialog() {
        if (allTransactions == null || allTransactions.isEmpty()) {
            Toast.makeText(this, "Heç bir əməliyyat tapılmadı", Toast.LENGTH_SHORT).show();
            return;
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        TransactionsDialog dialog = new TransactionsDialog(allTransactions, "📋 Bütün Əməliyyatlar");
        dialog.show(fragmentManager, "TransactionsDialog");
    }

    private void showAIAnalysisDialog() {
        if (allTransactions == null || allTransactions.isEmpty()) {
            Toast.makeText(this, "Analiz üçün məlumat yoxdur", Toast.LENGTH_SHORT).show();
            return;
        }

        // Kateqoriya məlumatlarını hazırla
        Map<String, Double> categoryData = new HashMap<>();
        for (Map<String, Object> t : allTransactions) {
            String category = (String) t.get("category");
            String type = (String) t.get("type");
            Double amount = (Double) t.get("amount");

            if ("expense".equals(type) && category != null && amount != null) {
                categoryData.put(category, categoryData.getOrDefault(category, 0.0) + amount);
            }
        }

        FragmentManager fragmentManager = getSupportFragmentManager();
        AIAnalysisDialog dialog = new AIAnalysisDialog(
                tvAiInsights.getText().toString(),
                "🤖 AI Maliyyə Analizi",
                categoryData,
                totalIncome,
                totalExpense,
                monthlySalary
        );
        dialog.show(fragmentManager, "AIAnalysisDialog");
    }

    private long getStartOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDay() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private long getStartOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    private long getEndOfDay(long timestamp) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(timestamp);
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        calendar.set(Calendar.MILLISECOND, 999);
        return calendar.getTimeInMillis();
    }

    private void startRealTimeUpdates() {
        refreshRunnable = () -> {
            fetchAllDataFromFirebase();
            handler.postDelayed(refreshRunnable, 30000);
        };
        handler.post(refreshRunnable);
    }

    private void logout() {
        mAuth.signOut();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
        if (financeModel != null) {
            financeModel.close();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAllDataFromFirebase();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
            drawerLayout.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }
}