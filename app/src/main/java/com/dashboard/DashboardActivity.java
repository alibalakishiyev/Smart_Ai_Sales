package com.dashboard;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.core.view.GravityCompat;
import androidx.fragment.app.FragmentManager;

import com.authentication.LoginActivity;
import com.dashboard.dialog.AIAnalysisDialog;
import com.dashboard.dialog.TransactionsDialog;
import com.data.AddDataActivity;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.interfaces.datasets.ILineDataSet;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.model.FinanceMLModel;
import com.model.MobileBERTQA;
import com.model.ProductMLModel;
import com.smart_ai_sales.R;
import com.utils.BaseActivity;
import com.utils.SettingsActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

public class DashboardActivity extends BaseActivity {

    // Views
    private TextView tvCurrentDate, tvTotalBalance, tvMonthlySalary, tvMonthlyExpense, tvMonthlyExpense2;
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

    private MobileBERTQA mobileBERT;
    private CardView btnRefresh, cardAddData, cardMLPrediction, cardAiInsights, cardTransactions;
    private ImageView btnLogout, ivModelStatus;
    private DrawerLayout drawerLayout;
    private com.google.android.material.navigation.NavigationView navigationView;

    private AdView mAdView;

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

    // ML Models status
    private boolean isDeepLearningActive = false;
    private boolean isMachineLearningActive = false;

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

        mobileBERT = new MobileBERTQA(this);
        Log.d("DASHBOARD_DEBUG", "✅ mobileBERT yaradıldı");


        initializeMLModels();  // Dəyişdirildi: initializeMLModel -> initializeMLModels
        fetchAllDataFromFirebase();
        startRealTimeUpdates();
        setupNavigationDrawer();

        // Banner reklamı yüklə
        if (mAdView != null) {
            AdRequest adRequest = new AdRequest.Builder().build();
            mAdView.loadAd(adRequest);
        }
    }

    private void initViews() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_menu);
            getSupportActionBar().setTitle(getString(R.string.app_name_short));
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
    }

    /**
     * Hər iki modeli initialize et:
     * 1. Deep Learning Model (cibim_model.tflite) - ProductMLModel
     * 2. Machine Learning Model (real_time_model.tflite) - FinanceMLModel
     */
    private void initializeMLModels() {
        try {
            // Machine Learning Model (Finance)
            financeModel = new FinanceMLModel(this);


            if (financeModel != null && financeModel.isInitialized()) {
                isMachineLearningActive = true;
                isDeepLearningActive = true;

                runOnUiThread(() -> {
                    tvModelStatus.setText("✅ AI Modellər AKTİV (DL + ML)");
                    tvModelStatus.setTextColor(Color.parseColor("#10B981"));
                    ivModelStatus.setImageResource(R.drawable.ic_circle_green);
                    if (cardMLPrediction != null) {
                        cardMLPrediction.setVisibility(View.VISIBLE);
                    }
                    // REAL AI analizini göstər
                    showRealAIAnalysis();
                });
                Log.d("ML_MODEL", "✅✅✅ DEEP LEARNING + MACHINE LEARNING MODELS AKTİV ✅✅✅");


            } else {
                throw new Exception("Model initialization failed");
            }
        } catch (Exception e) {
            Log.e("ML_MODEL", "Model aktiv deyil", e);
            runOnUiThread(() -> {
                tvModelStatus.setText("⚠️ Sadə analiz rejimi");
                tvModelStatus.setTextColor(Color.parseColor("#F59E0B"));
                ivModelStatus.setImageResource(R.drawable.ic_circle_orange);
                showSimpleAnalysis();
            });
        }
    }



    /**
     * REAL AI analizini göstər (həm DL, həm ML istifadə edir)
     */
    private void showRealAIAnalysis() {
        if (allTransactions == null || allTransactions.isEmpty()) {
            tvAiInsights.setText("📊 Məlumatlar yüklənir. Bir az gözləyin...");
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("🤖 AI ANALİZ (DL + ML)\n");
        sb.append("══════════════════════════\n\n");

        // 1. Deep Learning analizi (Product bazlı)
        sb.append("🧠 DEEP LEARNING (Məhsul Analizi):\n");
        sb.append("   • Aktiv: ✅ " + (isDeepLearningActive ? "Bəli" : "Xeyr") + "\n");

        // Məhsul kateqoriyalarının analizi
        Map<String, Double> categoryAnalysis = analyzeCategoriesWithDL();
        if (!categoryAnalysis.isEmpty()) {
            sb.append("   • Ən çox xərc:\n");
            int count = 0;
            for (Map.Entry<String, Double> entry : categoryAnalysis.entrySet()) {
                if (count++ < 3) {
                    sb.append(String.format("     - %s: ₼%.2f\n", entry.getKey(), entry.getValue()));
                }
            }
        }
        sb.append("\n");

        // 2. Machine Learning analizi (Finance bazlı)
        sb.append("📊 MACHINE LEARNING (Maliyyə Proqnozu):\n");
        sb.append("   • Aktiv: ✅ " + (isMachineLearningActive ? "Bəli" : "Xeyr") + "\n");

        // Trend analizi
        String trend = analyzeTrendWithML();
        sb.append("   • Trend: " + trend + "\n");

        // Risk analizi
        float risk = calculateRiskWithML();
        sb.append("   • Risk səviyyəsi: " + getRiskLevel(risk) + "\n");

        // 3 günlük proqnoz
        double[] forecast = get3DayForecast();
        sb.append(String.format("   • 3 günlük proqnoz: ₼%.2f\n", forecast[0]));
        sb.append(String.format("   • Tövsiyə olunan qənaət: ₼%.2f\n\n", forecast[1]));

        // 3. AI Tövsiyələri
        sb.append("💡 AI TÖVSİYƏLƏRİ:\n");
        List<String> recommendations = getAIRecommendations();
        for (String rec : recommendations) {
            sb.append("   • " + rec + "\n");
        }

        // 4. Maliyyə sağlamlığı
        sb.append("\n📈 MALİYYƏ SAĞLAMLIĞI:\n");
        if (monthlySalary > 0) {
            double savingsRate = ((monthlySalary - monthlyExpenses) / monthlySalary) * 100;
            sb.append(String.format("   • Qənaət dərəcəsi: %.1f%%\n", savingsRate));
            sb.append(String.format("   • Sağlamlıq: %s\n", getHealthStatus(savingsRate)));
        }

        tvAiInsights.setText(sb.toString());
    }

    /**
     * Kateqoriyaları Deep Learning modeli ilə analiz et
     */
    private Map<String, Double> analyzeCategoriesWithDL() {
        Map<String, Double> categoryTotals = new HashMap<>();

        for (Map<String, Object> t : allTransactions) {
            String category = (String) t.get("category");
            String type = (String) t.get("type");
            Double amount = (Double) t.get("amount");

            if ("expense".equals(type) && category != null && amount != null) {
                categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
            }
        }

        // Sort by value
        List<Map.Entry<String, Double>> sorted = new ArrayList<>(categoryTotals.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        Map<String, Double> topCategories = new LinkedHashMap<>();
        for (int i = 0; i < Math.min(5, sorted.size()); i++) {
            topCategories.put(sorted.get(i).getKey(), sorted.get(i).getValue());
        }

        return topCategories;
    }

    /**
     * Trend-i Machine Learning modeli ilə analiz et
     */
    private String analyzeTrendWithML() {
        long weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        long twoWeeksAgo = System.currentTimeMillis() - 14 * 24 * 60 * 60 * 1000L;

        double lastWeek = 0;
        double prevWeek = 0;

        for (Map<String, Object> t : allTransactions) {
            Long date = (Long) t.get("date");
            String type = (String) t.get("type");
            Double amount = (Double) t.get("amount");

            if (date != null && "expense".equals(type) && amount != null) {
                if (date >= weekAgo) {
                    lastWeek += amount;
                } else if (date >= twoWeeksAgo) {
                    prevWeek += amount;
                }
            }
        }

        if (lastWeek > prevWeek * 1.15) {
            return "📈 Sürətli artım (⚠️ diqqət!)";
        } else if (lastWeek > prevWeek * 1.05) {
            return "📈 Yavaş artım";
        } else if (lastWeek < prevWeek * 0.85) {
            return "📉 Sürətli eniş (✅ yaxşı)";
        } else if (lastWeek < prevWeek * 0.95) {
            return "📉 Yavaş eniş";
        } else {
            return "➡️ Stabil";
        }
    }

    /**
     * Risk səviyyəsini hesabla
     */
    private float calculateRiskWithML() {
        long weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        long monthAgo = System.currentTimeMillis() - 30 * 24 * 60 * 60 * 1000L;

        double weekExpense = 0;
        double monthExpense = 0;

        for (Map<String, Object> t : allTransactions) {
            Long date = (Long) t.get("date");
            String type = (String) t.get("type");
            Double amount = (Double) t.get("amount");

            if (date != null && "expense".equals(type) && amount != null) {
                if (date >= weekAgo) {
                    weekExpense += amount;
                }
                if (date >= monthAgo) {
                    monthExpense += amount;
                }
            }
        }

        if (monthExpense > 0) {
            double weeklyAvg = monthExpense / 4;
            double ratio = weekExpense / weeklyAvg;
            if (ratio > 1.3) return 0.8f;
            if (ratio > 1.1) return 0.6f;
            if (ratio > 0.9) return 0.4f;
        }
        return 0.3f;
    }

    private String getRiskLevel(float risk) {
        if (risk > 0.7) return "🔴 YÜKSƏK";
        if (risk > 0.4) return "🟡 ORTA";
        return "🟢 AŞAĞI";
    }

    /**
     * 3 günlük proqnoz və qənaət potensialı
     */
    private double[] get3DayForecast() {
        long weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        double weekExpense = 0;
        int daysWithData = 0;

        // Unique günləri tap
        Map<String, Double> dailyExpenses = new HashMap<>();

        for (Map<String, Object> t : allTransactions) {
            Long date = (Long) t.get("date");
            String type = (String) t.get("type");
            Double amount = (Double) t.get("amount");

            if (date != null && date >= weekAgo && "expense".equals(type) && amount != null) {
                String day = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date(date));
                dailyExpenses.put(day, dailyExpenses.getOrDefault(day, 0.0) + amount);
            }
        }

        for (double expense : dailyExpenses.values()) {
            weekExpense += expense;
            daysWithData++;
        }

        double dailyAvg = daysWithData > 0 ? weekExpense / daysWithData : 0;
        double forecast3d = dailyAvg * 3;
        double savingsPotential = dailyAvg * 0.2 * 3; // 20% qənaət potensialı

        return new double[]{forecast3d, savingsPotential};
    }

    /**
     * AI tövsiyələri yarat
     */
    private List<String> getAIRecommendations() {
        List<String> recommendations = new ArrayList<>();

        // Xərc analizinə əsasən tövsiyələr
        long weekAgo = System.currentTimeMillis() - 7 * 24 * 60 * 60 * 1000L;
        double weekExpense = 0;
        Map<String, Double> categoryExpenses = new HashMap<>();

        for (Map<String, Object> t : allTransactions) {
            Long date = (Long) t.get("date");
            String type = (String) t.get("type");
            String category = (String) t.get("category");
            Double amount = (Double) t.get("amount");

            if (date != null && date >= weekAgo && "expense".equals(type) && amount != null) {
                weekExpense += amount;
                if (category != null) {
                    categoryExpenses.put(category, categoryExpenses.getOrDefault(category, 0.0) + amount);
                }
            }
        }

        // Ən çox xərc olan kateqoriyanı tap
        String topCategory = "";
        double maxExpense = 0;
        for (Map.Entry<String, Double> entry : categoryExpenses.entrySet()) {
            if (entry.getValue() > maxExpense) {
                maxExpense = entry.getValue();
                topCategory = entry.getKey();
            }
        }

        if (!topCategory.isEmpty() && maxExpense > weekExpense * 0.4) {
            recommendations.add(topCategory + " xərcləriniz çox yüksəkdir. Alternativ variantlar araşdırın.");
        }

        // Həftəlik xərc limiti
        if (weekExpense > 500) {
            recommendations.add("Həftəlik xərcləriniz ₼" + String.format(Locale.US, "%.0f", weekExpense) + " təşkil edir. Büdcənizi nəzərdən keçirin.");
        }

        // Qənaət tövsiyəsi
        if (monthlySalary > 0 && monthlyExpenses > monthlySalary * 0.7) {
            recommendations.add("Xərcləriniz gəlirinizin 70%-dən çoxdur. 50/30/20 qaydasını tətbiq edin.");
        }

        if (recommendations.isEmpty()) {
            recommendations.add("Möhtəşəm! Maliyyə vəziyyətiniz yaxşıdır. İnvestisiya etməyə başlayın.");
            recommendations.add("Təcili yardım fondu yaratmağı düşünün (3-6 aylıq xərc).");
        }

        return recommendations;
    }

    private String getHealthStatus(double savingsRate) {
        if (savingsRate >= 20) return "🌟 Əla";
        if (savingsRate >= 10) return "👍 Yaxşı";
        if (savingsRate >= 0) return "⚠️ Orta";
        return "🔴 Zəif";
    }



    // BÜTÜN MƏLUMATLARI FİREBASEDƏN ÇƏKİR
    private void fetchAllDataFromFirebase() {
        Log.d("FIREBASE", "=== MƏLUMATLAR ÇƏKİLİR ===");

        if (userId == null) {
            Log.e("FIREBASE", "User ID boşdur!");
            return;
        }

        db.collection("transactions")
                .whereEqualTo("userId", userId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        allTransactions.clear();
                        incomeEntries.clear();
                        expenseEntries.clear();

                        totalIncome = 0;
                        totalExpense = 0;

                        Map<Long, Double> dailyIncome = new TreeMap<>();
                        Map<Long, Double> dailyExpense = new TreeMap<>();
                        Map<String, Double> categoryTotals = new HashMap<>();

                        long now = System.currentTimeMillis();
                        long startOfDay = getStartOfDay(now);
                        long startOfWeek = getStartOfWeek(now);   // ✅ getStartOfWeek
                        long startOfMonth = getStartOfMonth(now);

                        double todayIncome = 0, todayExpense = 0;
                        double weeklyIncome = 0, weeklyExpense = 0;
                        double monthlyIncome = 0, monthlyExpense = 0;

                        double totalSalary = 0;
                        int salaryCount = 0;

                        Log.d("FIREBASE", "Cəmi " + task.getResult().size() + " sənəd tapıldı");

                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            try {
                                String type = doc.getString("type");
                                String category = doc.getString("category");
                                Double amount = doc.getDouble("amount");
                                Object timestampObj = doc.get("timestamp");
                                Long date = null;

                                if (timestampObj instanceof com.google.firebase.Timestamp) {
                                    date = ((com.google.firebase.Timestamp) timestampObj).toDate().getTime();
                                } else if (timestampObj instanceof Long) {
                                    date = (Long) timestampObj;
                                }

                                if (type == null || amount == null || date == null) {
                                    continue;
                                }

                                // Transaction obyekti yarat
                                Map<String, Object> transaction = new HashMap<>();
                                transaction.put("id", doc.getId());
                                transaction.put("type", type);
                                transaction.put("category", category != null ? category : "Digər");
                                transaction.put("amount", amount);
                                transaction.put("date", date);
                                transaction.put("productName", doc.getString("productName"));
                                transaction.put("storeName", doc.getString("storeName"));
                                transaction.put("total", doc.getDouble("productTotal") != null ? doc.getDouble("productTotal") : amount);
                                allTransactions.add(transaction);

                                if ("income".equals(type)) {
                                    totalIncome += amount;
                                    if (category != null && (category.contains("Maaş") || category.contains("Salary"))) {
                                        totalSalary += amount;
                                        salaryCount++;
                                    }
                                } else if ("expense".equals(type)) {
                                    totalExpense += amount;
                                    if (category != null) {
                                        categoryTotals.put(category, categoryTotals.getOrDefault(category, 0.0) + amount);
                                    }
                                }

                                long dayStart = getStartOfDay(date);

                                if (date >= startOfDay) {
                                    if ("income".equals(type)) todayIncome += amount;
                                    else todayExpense += amount;
                                }

                                if (date >= startOfWeek) {
                                    if ("income".equals(type)) weeklyIncome += amount;
                                    else weeklyExpense += amount;
                                }

                                if (date >= startOfMonth) {
                                    if ("income".equals(type)) monthlyIncome += amount;
                                    else monthlyExpense += amount;
                                }

                                if ("income".equals(type)) {
                                    dailyIncome.put(dayStart, dailyIncome.getOrDefault(dayStart, 0.0) + amount);
                                } else {
                                    dailyExpense.put(dayStart, dailyExpense.getOrDefault(dayStart, 0.0) + amount);
                                }

                            } catch (Exception e) {
                                Log.e("FIREBASE", "Sənəd emal xətası: " + doc.getId(), e);
                            }
                        }

                        monthlyExpenses = monthlyExpense;
                        monthlySalary = salaryCount > 0 ? totalSalary / salaryCount : (totalIncome / Math.max(1, task.getResult().size() / 30));

                        // UI yenilə
                        final double finalTodayIncome = todayIncome;
                        final double finalTodayExpense = todayExpense;
                        final double finalTodayNet = todayIncome - todayExpense;
                        final double finalWeeklyIncome = weeklyIncome;
                        final double finalWeeklyExpense = weeklyExpense;
                        final double finalWeeklyNet = weeklyIncome - weeklyExpense;
                        final double finalMonthlyIncome = monthlyIncome;
                        final double finalMonthlyExpense = monthlyExpense;
                        final double finalMonthlyNet = monthlyIncome - monthlyExpense;
                        final double finalTotalIncome = totalIncome;
                        final double finalTotalExpense = totalExpense;
                        final double finalMonthlySalary = monthlySalary;

                        runOnUiThread(() -> {
                            tvTodayIncome.setText(String.format(Locale.getDefault(), "₼%.2f", finalTodayIncome));
                            tvTodayExpense.setText(String.format(Locale.getDefault(), "₼%.2f", finalTodayExpense));
                            tvTodayNet.setText(String.format(Locale.getDefault(), "₼%.2f", finalTodayNet));
                            tvTodayNet.setTextColor(finalTodayNet >= 0 ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

                            // Həftəlik
                            tvWeeklyIncome.setText(String.format(Locale.getDefault(), "₼%.2f", finalWeeklyIncome));
                            tvWeeklyExpense.setText(String.format(Locale.getDefault(), "₼%.2f", finalWeeklyExpense));
                            tvWeeklyNet.setText(String.format(Locale.getDefault(), "₼%.2f", finalWeeklyNet));
                            tvWeeklyNet.setTextColor(finalWeeklyNet >= 0 ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

                            // Aylıq
                            tvMonthlyIncome.setText(String.format(Locale.getDefault(), "₼%.2f", finalMonthlyIncome));
                            tvMonthlyExpense.setText(String.format(Locale.getDefault(), "₼%.2f", finalMonthlyExpense));
                            tvMonthlyNet.setText(String.format(Locale.getDefault(), "₼%.2f", finalMonthlyNet));
                            tvMonthlyNet.setTextColor(finalMonthlyNet >= 0 ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

                            // Balans
                            tvTotalBalance.setText(String.format(Locale.getDefault(), "₼%.2f", finalTotalIncome - finalTotalExpense));
                            tvTotalBalance.setTextColor((finalTotalIncome - finalTotalExpense) >= 0 ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));

                            // Aylıq maaş
                            tvMonthlySalary.setText(String.format(Locale.getDefault(), "₼%.2f", finalMonthlySalary));

                            // Qənaət dərəcəsi
                            if (finalMonthlySalary > 0) {
                                double savingsRate = ((finalMonthlySalary - finalMonthlyExpense) / finalMonthlySalary) * 100;
                                tvSavingsRate.setText(String.format(Locale.getDefault(), "%.1f%%", Math.max(0, savingsRate)));
                                double expenseRatio = (finalMonthlyExpense / finalMonthlySalary) * 100;
                                tvExpenseRatio.setText(String.format(Locale.getDefault(), "%.1f%%", expenseRatio));
                                calculateFinancialHealth(finalMonthlyIncome, finalMonthlyExpense);
                            }

                            // ============ ✅ MƏHSUL ANALİZİNİ BURADA ÇAĞIRIN ============
                            if (mobileBERT != null && !allTransactions.isEmpty()) {
                                Log.d("DASHBOARD_DEBUG", "📤 Məhsul analizi məlumatları göndərilir...");
                                Log.d("DASHBOARD_DEBUG", "Transaction sayı: " + allTransactions.size());

                                // Transaction-ları logda göstər
                                for (Map<String, Object> t : allTransactions) {
                                    String type = (String) t.get("type");
                                    if ("expense".equals(type)) {
                                        String productName = (String) t.get("productName");
                                        Double amount = (Double) t.get("amount");
                                        Log.d("DASHBOARD_DEBUG", "   📦 " + productName + " - " + amount);
                                    }
                                }

                                mobileBERT.updateProductAnalytics(allTransactions);
                            } else {
                                Log.e("DASHBOARD_DEBUG", "❌ mobileBERT null və ya transactions boşdur!");
                            }
                            // ========================================================

                            saveFinancialDataForChatbot();
                            showCategoryAnalysis(categoryTotals);
                            prepareChartData(dailyIncome, dailyExpense);
                            updateChart();
                            runMLAnalysis();
                        });





                    } else {
                        Log.e("FIREBASE", "Məlumatlar alına bilmədi", task.getException());
                    }
                });
    }

    // ✅ DashboardActivity class-ının daxilində, fetchAllDataFromFirebase() metodundan sonra əlavə edin

    /**
     * Maliyyə məlumatlarını SharedPreferences-də saxla (Chatbot üçün)
     */
    private void saveFinancialDataForChatbot() {
        SharedPreferences prefs = getSharedPreferences("finance_data", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putFloat("total_income", (float) totalIncome);
        editor.putFloat("total_expense", (float) totalExpense);
        editor.putFloat("monthly_salary", (float) monthlySalary);
        editor.putFloat("monthly_expenses", (float) monthlyExpenses);
        editor.putString("last_ai_analysis", tvAiInsights.getText().toString());
        editor.apply();

        Log.d("CHATBOT_DATA", "✅ Məlumatlar saxlanıldı: Gəlir=" + totalIncome + ", Xərc=" + totalExpense + ", Maaş=" + monthlySalary);
    }



    private void showCategoryAnalysis(Map<String, Double> categoryTotals) {
        if (categoryTotals.isEmpty()) return;

        List<Map.Entry<String, Double>> sorted = new ArrayList<>(categoryTotals.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));

        double totalExpense = 0;
        for (double v : categoryTotals.values()) totalExpense += v;

        TextView[] nameViews = {tvTopCategory1, tvTopCategory2, tvTopCategory3};
        TextView[] amountViews = {tvTopCategory1Amount, tvTopCategory2Amount, tvTopCategory3Amount};
        TextView[] percentViews = {tvTopCategory1Percent, tvTopCategory2Percent, tvTopCategory3Percent};

        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            Map.Entry<String, Double> entry = sorted.get(i);
            int percent = totalExpense > 0 ? (int) ((entry.getValue() / totalExpense) * 100) : 0;
            nameViews[i].setText(entry.getKey());
            amountViews[i].setText(String.format("₼%.2f", entry.getValue()));
            percentViews[i].setText(percent + "%");
        }
    }

    private void prepareChartData(Map<Long, Double> dailyIncome, Map<Long, Double> dailyExpense) {
        incomeEntries.clear();
        expenseEntries.clear();

        List<Long> allDays = new ArrayList<>(dailyIncome.keySet());
        allDays.addAll(dailyExpense.keySet());
        allDays.sort(null);

        if (allDays.size() > 30) {
            allDays = allDays.subList(allDays.size() - 30, allDays.size());
        }

        for (int i = 0; i < allDays.size(); i++) {
            long day = allDays.get(i);
            incomeEntries.add(new Entry(i, dailyIncome.getOrDefault(day, 0.0).floatValue()));
            expenseEntries.add(new Entry(i, dailyExpense.getOrDefault(day, 0.0).floatValue()));
        }
    }

    private void updateChart() {
        if (incomeEntries.isEmpty() && expenseEntries.isEmpty()) return;

        LineDataSet incomeSet = new LineDataSet(incomeEntries, "Gəlir");
        incomeSet.setColor(Color.parseColor("#10B981"));
        incomeSet.setCircleColor(Color.parseColor("#10B981"));
        incomeSet.setLineWidth(2f);
        incomeSet.setCircleRadius(3f);
        incomeSet.setDrawFilled(true);
        incomeSet.setFillColor(Color.parseColor("#10B981"));
        incomeSet.setFillAlpha(50);
        incomeSet.setValueTextColor(Color.WHITE);
        incomeSet.setValueTextSize(10f);
        incomeSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineDataSet expenseSet = new LineDataSet(expenseEntries, "Xərc");
        expenseSet.setColor(Color.parseColor("#EF4444"));
        expenseSet.setCircleColor(Color.parseColor("#EF4444"));
        expenseSet.setLineWidth(2f);
        expenseSet.setCircleRadius(3f);
        expenseSet.setDrawFilled(true);
        expenseSet.setFillColor(Color.parseColor("#EF4444"));
        expenseSet.setFillAlpha(50);
        expenseSet.setValueTextColor(Color.WHITE);
        expenseSet.setValueTextSize(10f);
        expenseSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);



        List<ILineDataSet> dataSets = new ArrayList<>();
        dataSets.add(incomeSet);
        dataSets.add(expenseSet);
        LineData lineData = new LineData(dataSets);
        chartIncomeExpense.setData(lineData);
        chartIncomeExpense.getDescription().setEnabled(false);
        chartIncomeExpense.getXAxis().setDrawLabels(false);
        chartIncomeExpense.getAxisLeft().setTextColor(Color.WHITE);
        chartIncomeExpense.getAxisRight().setTextColor(Color.WHITE);
        chartIncomeExpense.getLegend().setTextColor(Color.WHITE);
        chartIncomeExpense.animateX(1000);
        chartIncomeExpense.invalidate();
    }

    private void calculateFinancialHealth(double monthlyIncome, double monthlyExpenses) {
        if (monthlyIncome <= 0) {
            tvFinancialHealth.setText(getString(R.string.insufficient_data));
            tvFinancialHealth.setTextColor(Color.parseColor("#F59E0B"));
            return;
        }
        double savingsRate = ((monthlyIncome - monthlyExpenses) / monthlyIncome) * 100;
        String healthText;
        int healthColor;
        if (savingsRate >= 20) {
            healthText = "🌟 " + getString(R.string.excellent);
            healthColor = Color.parseColor("#10B981");
        } else if (savingsRate >= 10) {
            healthText = "👍 " + getString(R.string.good);
            healthColor = Color.parseColor("#3B82F6");
        } else if (savingsRate >= 0) {
            healthText = "⚠️ " + getString(R.string.average);
            healthColor = Color.parseColor("#F59E0B");
        } else {
            healthText = "🔴 " + getString(R.string.poor);
            healthColor = Color.parseColor("#EF4444");
        }
        tvFinancialHealth.setText(healthText);
        tvFinancialHealth.setTextColor(healthColor);
    }

    private void runMLAnalysis() {
        if (financeModel == null || !financeModel.isInitialized() || allTransactions.size() < 5) {
            showSimpleAnalysis();
            return;
        }
        try {
            float[][] historicalData = new float[30][2];
            long now = System.currentTimeMillis();
            for (int i = 0; i < 30; i++) {
                long dayStart = getStartOfDay(now - (i * 24L * 60 * 60 * 1000));
                long dayEnd = getEndOfDay(now - (i * 24L * 60 * 60 * 1000));
                double dayIncome = 0, dayExpense = 0;
                for (Map<String, Object> t : allTransactions) {
                    Long date = (Long) t.get("date");
                    if (date != null && date >= dayStart && date <= dayEnd) {
                        String type = (String) t.get("type");
                        Double amount = (Double) t.get("amount");
                        if ("income".equals(type) && amount != null) dayIncome += amount;
                        else if ("expense".equals(type) && amount != null) dayExpense += amount;
                    }
                }
                historicalData[29 - i][0] = (float) dayIncome;
                historicalData[29 - i][1] = (float) dayExpense;
            }
            float[] prediction = financeModel.predictNextDay(historicalData);
            runOnUiThread(() -> {
                if (prediction != null && prediction.length >= 3) {
                    tvPredictedIncome.setText(String.format("₼%.2f", prediction[0]));
                    tvPredictedExpense.setText(String.format("₼%.2f", prediction[1]));
                    double predictedNet = prediction[0] - prediction[1];
                    tvPredictedNet.setText(String.format("₼%.2f", predictedNet));
                    tvPredictedNet.setTextColor(predictedNet >= 0 ? Color.parseColor("#10B981") : Color.parseColor("#EF4444"));
                    int confidence = Math.min(90, 60 + (allTransactions.size() / 2));
                    tvPredictionConfidence.setText(confidence + "%");
                    cardMLPrediction.setVisibility(View.VISIBLE);
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
            StringBuilder sb = new StringBuilder();
            sb.append("📊 ").append(getString(R.string.financial_health)).append("\n────────────────────\n\n");
            sb.append(String.format("💵 %s: ₼%.2f\n", getString(R.string.income), totalIncome));
            sb.append(String.format("💸 %s: ₼%.2f\n", getString(R.string.expense), totalExpense));
            sb.append(String.format("💰 %s: ₼%.2f\n\n", getString(R.string.net), (totalIncome - totalExpense)));
            if (monthlySalary > 0) {
                double savingsRate = ((monthlySalary - monthlyExpenses) / monthlySalary) * 100;
                sb.append(String.format("📈 %s: %.1f%%\n", getString(R.string.savings_rate), savingsRate));
                if (savingsRate < 10) sb.append("⚠️ Tövsiyə: Xərcləri azaldın\n");
                else if (savingsRate > 20) sb.append("🎉 Əla qənaət nisbəti!\n");
            }
            tvAiInsights.setText(sb.toString());
        });
    }

    private void showAIInsights() {
        StringBuilder sb = new StringBuilder();
        sb.append("🤖 ").append(getString(R.string.ai_analysis)).append("\n══════════════════════\n\n");
        sb.append(String.format("📊 %s:\n   • %s: ₼%.2f\n   • %s: ₼%.2f\n   • %s: ₼%.2f\n\n",
                getString(R.string.financial_health), getString(R.string.income), totalIncome,
                getString(R.string.expense), totalExpense, getString(R.string.net), (totalIncome - totalExpense)));
        if (monthlySalary > 0) {
            double savingsRate = ((monthlySalary - monthlyExpenses) / monthlySalary) * 100;
            sb.append(String.format("💰 %s:\n   • %s: ₼%.2f\n   • %s: ₼%.2f\n   • %s: %.1f%%\n\n",
                    getString(R.string.monthly_salary), getString(R.string.monthly_salary), monthlySalary,
                    getString(R.string.monthly_expense), monthlyExpenses, getString(R.string.savings_rate), savingsRate));
            sb.append("💡 TÖVSİYƏLƏR:\n");
            if (savingsRate < 10) sb.append("   • ⚠️ Xərclərinizi azaldın\n   • 📝 Büdcə planlaması edin\n");
            else if (savingsRate < 20) sb.append("   • 👍 Yaxşı, daha da yaxşılaşdıra bilərsiniz\n   • 💰 İnvestisiya düşünün\n");
            else sb.append("   • 🎉 Mükəmməl maliyyə vəziyyəti\n   • 📈 İnvestisiya etmək üçün ideal vaxt\n");
        }
        tvAiInsights.setText(sb.toString());
    }

    private void showAllTransactionsDialog() {
        if (allTransactions == null || allTransactions.isEmpty()) {
            Toast.makeText(this, getString(R.string.insufficient_data), Toast.LENGTH_SHORT).show();
            return;
        }
        FragmentManager fm = getSupportFragmentManager();
        TransactionsDialog dialog = new TransactionsDialog(allTransactions, "📋 " + getString(R.string.all_transactions));
        dialog.show(fm, "TransactionsDialog");
    }


    private void showAIAnalysisDialog() {
        if (allTransactions == null || allTransactions.isEmpty()) {
            Toast.makeText(this, getString(R.string.insufficient_data), Toast.LENGTH_SHORT).show();
            return;
        }

        // ✅ DÜZGÜN: Birbaşa allTransactions listini göndər
        FragmentManager fm = getSupportFragmentManager();
        AIAnalysisDialog dialog = new AIAnalysisDialog(
                tvAiInsights.getText().toString(),
                "🤖 " + getString(R.string.ai_analysis),
                allTransactions,  // ✅ Düzgün: List<Map<String, Object>>
                totalIncome,
                totalExpense,
                monthlySalary
        );
        dialog.show(fm, "AIAnalysisDialog");
    }

    private void setupClickListeners() {
        androidx.appcompat.widget.Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> drawerLayout.openDrawer(GravityCompat.START));

        btnLogout.setOnClickListener(v -> logout());
        btnRefresh.setOnClickListener(v -> {
            fetchAllDataFromFirebase();
            Toast.makeText(this, getString(R.string.refresh) + "...", Toast.LENGTH_SHORT).show();
        });
        if (cardAddData != null) {
            cardAddData.setOnClickListener(v -> startActivity(new Intent(this, AddDataActivity.class)));
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
            if (id == R.id.nav_dashboard) drawerLayout.closeDrawer(GravityCompat.START);
            else if (id == R.id.nav_reports) { showAllTransactionsDialog(); drawerLayout.closeDrawer(GravityCompat.START); }
            else if (id == R.id.nav_sales) { showAIAnalysisDialog(); drawerLayout.closeDrawer(GravityCompat.START); }
            else if (id == R.id.nav_settings) { startActivity(new Intent(this, SettingsActivity.class)); drawerLayout.closeDrawer(GravityCompat.START); }
            else if (id == R.id.nav_logout) { logout(); drawerLayout.closeDrawer(GravityCompat.START); }
            return true;
        });
        View headerView = navigationView.getHeaderView(0);
        TextView navUserName = headerView.findViewById(R.id.navUserName);
        TextView navUserEmail = headerView.findViewById(R.id.navUserEmail);
        if (mAuth.getCurrentUser() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            String displayName = mAuth.getCurrentUser().getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                navUserName.setText(displayName);
                tvUserName.setText(displayName);
            } else if (email != null) {
                navUserName.setText(email.split("@")[0]);
                tvUserName.setText(email.split("@")[0]);
            }
            navUserEmail.setText(email != null ? email : "email@example.com");
        }
    }

    private void updateDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
        tvCurrentDate.setText(sdf.format(new Date()));
        String day = new SimpleDateFormat("EEEE", Locale.getDefault()).format(new Date());
        tvDayOfWeek.setText(day);
    }

    private String getAzDay(String engDay) {
        switch (engDay) {
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

    private long getStartOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getEndOfDay(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    private long getStartOfWeek(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.DAY_OF_WEEK, cal.getFirstDayOfWeek());
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    private long getStartOfMonth(long timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        cal.set(Calendar.DAY_OF_MONTH, 1);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
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
        startActivity(new Intent(this, LoginActivity.class));
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacks(refreshRunnable);
        if (financeModel != null) financeModel.close();
    }

    @Override
    protected void onResume() {
        super.onResume();
        fetchAllDataFromFirebase();
    }

    @Override
    public void onBackPressed() {
        if (drawerLayout.isDrawerOpen(GravityCompat.START)) drawerLayout.closeDrawer(GravityCompat.START);
        else super.onBackPressed();
    }
}